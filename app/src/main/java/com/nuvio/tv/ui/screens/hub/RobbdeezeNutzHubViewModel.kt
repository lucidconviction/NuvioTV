package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.GameToChannelMatcher
import com.robbdeeze.nuviotv.domain.model.*
import com.robbdeeze.nuviotv.ui.screens.player.SportsNowStore
import com.robbdeeze.nuviotv.domain.repository.IptvRepository
import com.robbdeeze.nuviotv.domain.repository.MagNutzRepository
import com.robbdeeze.nuviotv.domain.repository.MusicNutzRepository
import com.robbdeeze.nuviotv.domain.repository.VidNutzRepository
import com.robbdeeze.nuviotv.data.sports.DaddyLiveClient
import com.robbdeeze.nuviotv.data.sports.YouTubeStreamResolver
import com.robbdeeze.nuviotv.data.local.MusicNutzStore
import com.robbdeeze.nuviotv.data.local.StreamValidationStore
import com.robbdeeze.nuviotv.data.local.StreamValidator
import com.robbdeeze.nuviotv.core.profile.ProfileManager
import com.robbdeeze.nuviotv.data.remote.api.ExternalStreamsClient
import com.robbdeeze.nuviotv.data.remote.api.ExternalStreamMatch
import com.robbdeeze.nuviotv.data.remote.api.SportsClient
import com.robbdeeze.nuviotv.data.remote.api.TheSportsDbClient
import com.robbdeeze.nuviotv.data.remote.api.WikipediaClient
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalClient
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalEvent
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalTvChannel
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalMappings
import com.robbdeeze.nuviotv.data.remote.dto.EspnStandingEntry
import com.robbdeeze.nuviotv.data.remote.dto.TheSportsDbEvent
import com.robbdeeze.nuviotv.data.local.PortalLicenseKey
import com.robbdeeze.nuviotv.data.local.PortalLicenseManager
import com.robbdeeze.nuviotv.data.local.QuickChannelPreferences
import com.robbdeeze.nuviotv.data.local.LicenseStatus
import com.robbdeeze.nuviotv.data.local.LicenseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, MusicNutz, MagNutz, Multi, ExternalStreams }

@HiltViewModel
class RobbdeezeNutzHubViewModel @Inject constructor(
    private val iptvRepository: IptvRepository,
    private val sportsClient: SportsClient,
    private val vidNutzRepository: VidNutzRepository,
    private val musicNutzRepository: MusicNutzRepository,
    private val magNutzRepository: MagNutzRepository,
    private val theSportsDbClient: TheSportsDbClient,
    private val musicNutzStore: MusicNutzStore,
    private val profileManager: ProfileManager,
    private val streamValidationStore: StreamValidationStore,
    private val _portalLicenseManager: PortalLicenseManager,
    private val quickChannelPreferences: QuickChannelPreferences,
) : ViewModel() {

    val portalLicenseManager: PortalLicenseManager = _portalLicenseManager

    private val _subScreen = MutableStateFlow(HubSubScreen.Hub)
    val subScreen: StateFlow<HubSubScreen> = _subScreen.asStateFlow()

    private val _resetEvent = MutableSharedFlow<Unit>(replay = 0)
    val resetEvent: SharedFlow<Unit> = _resetEvent.asSharedFlow()

    private val _playerLoadingMessage = MutableStateFlow<String?>(null)
    val playerLoadingMessage: StateFlow<String?> = _playerLoadingMessage.asStateFlow()

    private val _validationProgress = MutableStateFlow<String?>(null)
    val validationProgress: StateFlow<String?> = _validationProgress.asStateFlow()

    private val _showDeadStreams = MutableStateFlow(false)
    val showDeadStreams: StateFlow<Boolean> = _showDeadStreams.asStateFlow()

    private val _deadUrls = MutableStateFlow<Set<String>>(emptySet())
    val deadUrls: StateFlow<Set<String>> = _deadUrls.asStateFlow()

    private var pendingValidation = false

    fun toggleShowDeadStreams() {
        _showDeadStreams.value = !_showDeadStreams.value
    }

    fun loadDeadUrls() {
        viewModelScope.launch {
            val profileId = profileManager.activeProfileId.value
            _deadUrls.value = streamValidationStore.getDeadUrls(profileId)
        }
    }

    fun requestValidation() {
        pendingValidation = true
    }

    fun runPendingValidation(channels: List<IptvChannel>) {
        if (!pendingValidation) return
        pendingValidation = false
        viewModelScope.launch {
            val profileId = profileManager.activeProfileId.value
            val urls = channels.map { it.url }.filter { it.isNotBlank() }
            if (urls.isEmpty()) return@launch
            _validationProgress.value = "Validating 0/${urls.size}..."
            val dead = StreamValidator.validateUrls(urls) { done, total ->
                _validationProgress.value = "Validating $done/$total..."
            }
            streamValidationStore.markDead(dead, profileId)
            if (dead.isNotEmpty()) {
                streamValidationStore.markGood((urls.toSet() - dead), profileId)
            }
            _deadUrls.value = dead
            _validationProgress.value = if (dead.isEmpty()) "All ${urls.size} streams working!" else "Found ${dead.size} dead of ${urls.size}"
            kotlinx.coroutines.delay(5000)
            _validationProgress.value = null
        }
    }

    private val _vidNutzReturnContext = MutableStateFlow<VidNutzReturnContext?>(null)
    val vidNutzReturnContext: StateFlow<VidNutzReturnContext?> = _vidNutzReturnContext.asStateFlow()

    data class VidNutzReturnContext(
        val scrollPosition: Int,
        val isInSearchMode: Boolean,
        val searchQuery: String,
        val selectedCategory: VidNutzCategory,
    )

    fun storeVidNutzReturnContext(scrollPosition: Int, isInSearchMode: Boolean, searchQuery: String, selectedCategory: VidNutzCategory) {
        _vidNutzReturnContext.value = VidNutzReturnContext(
            scrollPosition = scrollPosition,
            isInSearchMode = isInSearchMode,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
        )
    }

    fun restoreVidNutzReturnContext(): VidNutzReturnContext? {
        return _vidNutzReturnContext.value
    }

    fun clearVidNutzReturnContext() {
        _vidNutzReturnContext.value = null
    }

    fun setVidNutzScrollPosition(position: Int) {
        _vidNutzUiState.value = _vidNutzUiState.value.copy(scrollPosition = position)
    }

    fun setVidNutzInSearchMode(isSearch: Boolean) {
        _vidNutzUiState.value = _vidNutzUiState.value.copy(isInSearchMode = isSearch)
    }

    fun setVidNutzSearchQuery(query: String) {
        _vidNutzUiState.value = _vidNutzUiState.value.copy(searchQuery = query)
    }

    fun setVidNutzSelectedCategory(category: VidNutzCategory) {
        _vidNutzUiState.value = _vidNutzUiState.value.copy(selectedCategory = category)
    }

    fun playVideo(
        videoId: String,
        title: String,
        thumbnail: String?,
        onPlayChannel: (IptvChannel) -> Unit,
        scrollPosition: Int = 0,
        isInSearchMode: Boolean = false,
        searchQuery: String = "",
        selectedCategory: VidNutzCategory = VidNutzCategory.TRENDING,
    ) {
        storeVidNutzReturnContext(scrollPosition, isInSearchMode, searchQuery, selectedCategory)
        viewModelScope.launch {
            _playerLoadingMessage.value = "Preparing..."
            try {
                val result = YouTubeStreamResolver.resolveStreamResult(videoId)
                if (result != null) {
                    onPlayChannel(IptvChannel(
                        id = videoId, name = title, url = result.videoUrl, logoUrl = thumbnail,
                        audioUrl = result.audioUrl, qualities = result.qualities,
                    ))
                }
            } catch (_: Exception) {}
            _playerLoadingMessage.value = null
        }
    }

    val iptvSources = iptvRepository.getSources()
    val iptvFavorites = iptvRepository.getFavorites()
    private val _iptvChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val iptvChannels = _iptvChannels.asStateFlow()
    private val _iptvLoading = MutableStateFlow(false)
    val iptvLoading = _iptvLoading.asStateFlow()
    private val _iptvLoadProgress = MutableStateFlow("")
    val iptvLoadProgress: StateFlow<String> = _iptvLoadProgress.asStateFlow()
    private val _iptvSearchQuery = MutableStateFlow("")
    val iptvSearchQuery: StateFlow<String> = _iptvSearchQuery.asStateFlow()
    private val _activeIptvSourceName = MutableStateFlow("")
    val activeIptvSourceName: StateFlow<String> = _activeIptvSourceName.asStateFlow()
    private val _iptvCategories = MutableStateFlow<List<String>>(emptyList())
    val iptvCategories: StateFlow<List<String>> = _iptvCategories.asStateFlow()
    private val _activeIptvSource = MutableStateFlow<IptvSource?>(null)
    val activeIptvSource: StateFlow<IptvSource?> = _activeIptvSource.asStateFlow()
    private var iptvLoadJob: Job? = null
    private val _allIptvChannels = MutableStateFlow<List<IptvChannel>>(emptyList())
    val allIptvChannels: StateFlow<List<IptvChannel>> = _allIptvChannels.asStateFlow()
    private val _customQuickChannels = MutableStateFlow<List<QuickChannel>>(emptyList())
    val customQuickChannels: StateFlow<List<QuickChannel>> = _customQuickChannels.asStateFlow()

    init {
        viewModelScope.launch {
            quickChannelPreferences.customQuickChannels.collect { _customQuickChannels.value = it }
        }
    }

    fun addCustomQuickChannel(channel: QuickChannel) {
        viewModelScope.launch { quickChannelPreferences.addCustomQuickChannel(channel) }
    }

    fun removeCustomQuickChannel(channel: QuickChannel) {
        viewModelScope.launch { quickChannelPreferences.removeCustomQuickChannel(channel) }
    }

    fun clearCustomQuickChannels() {
        viewModelScope.launch { quickChannelPreferences.clearCustomQuickChannels() }
    }

    companion object {
        @JvmStatic var pendingQuickChannelName: String? = null
        var pendingMagnetUri: String? = null
        private const val MAX_SPORT_CHANNEL_MATCHES = 200
    }

    fun setActiveIptvSource(source: IptvSource?) {
        _activeIptvSource.value = source
    }

    // Sports in-memory cache (60-second TTL)
    private data class CacheEntry<T>(val data: T, val timestamp: Long = System.currentTimeMillis())
    private val sportsCache = mutableMapOf<String, CacheEntry<List<SportEvent>>>()
    private val CACHE_TTL_MS = 60_000L
    private fun isCacheValid(key: String): Boolean {
        val entry = sportsCache[key] ?: return false
        return System.currentTimeMillis() - entry.timestamp < CACHE_TTL_MS
    }

    private val _sportsEvents = MutableStateFlow<List<SportEvent>>(emptyList())
    val sportsEvents = _sportsEvents.asStateFlow()
    private var _activeSportLeague: SportLeague? = null
    private val _sportsLoading = MutableStateFlow(false)
    val sportsLoading = _sportsLoading.asStateFlow()
    private val _allLiveEvents = MutableStateFlow<List<SportEvent>>(emptyList())
    val allLiveEvents: StateFlow<List<SportEvent>> = _allLiveEvents.asStateFlow()
    private val _allUpcomingEvents = MutableStateFlow<List<SportEvent>>(emptyList())
    val allUpcomingEvents: StateFlow<List<SportEvent>> = _allUpcomingEvents.asStateFlow()
    private val _allLiveLoading = MutableStateFlow(false)
    val allLiveLoading: StateFlow<Boolean> = _allLiveLoading.asStateFlow()
    private val _daddyLiveEvents = MutableStateFlow<List<com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent>>(emptyList())
    val daddyLiveEvents: StateFlow<List<com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent>> = _daddyLiveEvents.asStateFlow()
    private val _daddyLiveLoading = MutableStateFlow(false)
    val daddyLiveLoading: StateFlow<Boolean> = _daddyLiveLoading.asStateFlow()
    private val _ufcUpcomingEvents = MutableStateFlow<List<TheSportsDbEvent>>(emptyList())
    val ufcUpcomingEvents: StateFlow<List<TheSportsDbEvent>> = _ufcUpcomingEvents.asStateFlow()
    private val _ufcUpcomingLoading = MutableStateFlow(false)
    val ufcUpcomingLoading: StateFlow<Boolean> = _ufcUpcomingLoading.asStateFlow()

    private val _selectedTeam = MutableStateFlow<TeamDetailState?>(null)
    val selectedTeam: StateFlow<TeamDetailState?> = _selectedTeam.asStateFlow()

    private val _sportsLeagues = MutableStateFlow(
        listOf(
            SportLeague("now", "⚡ Sports Now/Later", "NOW", "now/_"),
            // American Combat Sports first
            SportLeague("ufc", "UFC MMA", "UFC", "mma/ufc"),
            SportLeague("mma", "MMA / Combat Sports", "MMA", "mma/_"),
            SportLeague("bkfc", "BKFC", "BKFC", "mma/bkfc"),
            SportLeague("powerslap", "Power Slap", "SLAP", "mma/powerslap"),
            SportLeague("boxing", "Boxing", "BOX", "boxing/boxing"),
            SportLeague("pfl", "PFL MMA", "PFL", "mma/pfl"),
            // American Major Leagues
            SportLeague("nfl", "NFL Football", "NFL", "football/nfl"),
            SportLeague("nba", "NBA Basketball", "NBA", "basketball/nba"),
            SportLeague("mlb", "MLB Baseball", "MLB", "baseball/mlb"),
            SportLeague("nhl", "NHL Hockey", "NHL", "hockey/nhl"),
            SportLeague("cfb", "College Football", "CFB", "football/college-football"),
            SportLeague("cbb", "College Basketball", "CBB", "basketball/mens-college-basketball"),
            SportLeague("wnba", "WNBA", "WNBA", "basketball/wnba"),
            // Other leagues
            SportLeague("ppv", "PPV / Special Events", "PPV", "ppv/_"),
            SportLeague("soccer", "MLS Soccer", "MLS", "soccer/usa.1"),
            SportLeague("epl", "Premier League", "EPL", "soccer/eng.1"),
            SportLeague("laliga", "La Liga", "LA", "soccer/esp.1"),
            SportLeague("seriea", "Serie A", "SA", "soccer/ita.1"),
            SportLeague("bundesliga", "Bundesliga", "BUN", "soccer/ger.1"),
            SportLeague("ligue1", "Ligue 1", "L1", "soccer/fra.1"),
            SportLeague("ucl", "Champions League", "UCL", "soccer/uefa.champions"),
            SportLeague("f1", "Formula 1", "F1", "racing/f1"),
            SportLeague("tennis", "Tennis", "TEN", "tennis/atp"),
            SportLeague("golf", "Golf", "GOL", "golf/pga"),
        )
    )
    val sportsLeagues = _sportsLeagues.asStateFlow()

    private val _selectedSportEvent = MutableStateFlow<SportEvent?>(null)
    val selectedSportEvent: StateFlow<SportEvent?> = _selectedSportEvent.asStateFlow()

    private val _matchedChannels = MutableStateFlow<List<MatchedChannel>>(emptyList())
    val matchedChannels: StateFlow<List<MatchedChannel>> = _matchedChannels.asStateFlow()

    private val _standingsEntries = MutableStateFlow<List<EspnStandingEntry>>(emptyList())
    val standingsEntries: StateFlow<List<EspnStandingEntry>> = _standingsEntries.asStateFlow()
    private val _standingsLoading = MutableStateFlow(false)
    val standingsLoading: StateFlow<Boolean> = _standingsLoading.asStateFlow()
    private val _standingsError = MutableStateFlow<String?>(null)
    val standingsError: StateFlow<String?> = _standingsError.asStateFlow()
    private val _sportsError = MutableStateFlow<String?>(null)
    val sportsError: StateFlow<String?> = _sportsError.asStateFlow()

    private val _sportsChannelLoading = MutableStateFlow(false)
    val sportsChannelLoading: StateFlow<Boolean> = _sportsChannelLoading.asStateFlow()

    private val _sportEventVideos = MutableStateFlow<List<SportEventVideo>>(emptyList())
    val sportEventVideos: StateFlow<List<SportEventVideo>> = _sportEventVideos.asStateFlow()
    private val _sportVideosLoading = MutableStateFlow(false)
    val sportVideosLoading: StateFlow<Boolean> = _sportVideosLoading.asStateFlow()
    private val _sportRegionFilter = MutableStateFlow("ALL")
    val sportRegionFilter: StateFlow<String> = _sportRegionFilter.asStateFlow()
    private val _activeEventTab = MutableStateFlow(EventTab.LIVE)
    val activeEventTab: StateFlow<EventTab> = _activeEventTab.asStateFlow()

    private val _sync2CalEvents = MutableStateFlow<Map<String, List<Sync2CalEvent>>>(emptyMap())
    val sync2CalEvents: StateFlow<Map<String, List<Sync2CalEvent>>> = _sync2CalEvents.asStateFlow()
    private val _sync2CalTvChannels = MutableStateFlow<Map<Long, List<Sync2CalTvChannel>>>(emptyMap())
    val sync2CalTvChannels: StateFlow<Map<Long, List<Sync2CalTvChannel>>> = _sync2CalTvChannels.asStateFlow()
    private val _sync2CalLoading = MutableStateFlow(false)
    val sync2CalLoading: StateFlow<Boolean> = _sync2CalLoading.asStateFlow()

    private val _sportsCalendarDays = MutableStateFlow<List<SportCalendarDay>>(emptyList())
    val sportsCalendarDays: StateFlow<List<SportCalendarDay>> = _sportsCalendarDays.asStateFlow()
    private val _sportsCalendarLoading = MutableStateFlow(false)
    val sportsCalendarLoading: StateFlow<Boolean> = _sportsCalendarLoading.asStateFlow()

    private var sportsRefreshJob: Job? = null

    private val _vidNutzUiState = MutableStateFlow(VidNutzUiState())
    val vidNutzUiState: StateFlow<VidNutzUiState> = _vidNutzUiState.asStateFlow()

    private val _musicNutzUiState = MutableStateFlow(MusicNutzUiState())
    val musicNutzUiState: StateFlow<MusicNutzUiState> = _musicNutzUiState.asStateFlow()

    // Portal license state — must be declared BEFORE init {} so that
    // loadPortalLicense() (called from init) can assign to these flows.
    private val _portalLicense = MutableStateFlow<PortalLicenseKey?>(null)
    val portalLicense: StateFlow<PortalLicenseKey?> = _portalLicense.asStateFlow()
    private val _portalLicenseStatus = MutableStateFlow(LicenseStatus.NOT_ACTIVATED)
    val portalLicenseStatus: StateFlow<LicenseStatus> = _portalLicenseStatus.asStateFlow()

    init {
        val savedPlaylists = musicNutzStore.loadPlaylists()
        val savedAlbumIds = musicNutzStore.loadSavedAlbumIds()
        val savedDownloads = musicNutzStore.loadDownloadedTracks()
        if (savedPlaylists.isNotEmpty() || savedAlbumIds.isNotEmpty() || savedDownloads.isNotEmpty()) {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                playlists = savedPlaylists,
                savedAlbumIds = savedAlbumIds,
                downloadedTracks = savedDownloads,
            )
        }
        loadPortalLicense()
    }

    private val _magNutzUiState = MutableStateFlow(TorrentUiState())
    val magNutzUiState: StateFlow<TorrentUiState> = _magNutzUiState.asStateFlow()

    private val _externalStreamsMatches = MutableStateFlow<List<ExternalStreamMatch>>(emptyList())
    val externalStreamsMatches: StateFlow<List<ExternalStreamMatch>> = _externalStreamsMatches.asStateFlow()
    private val _externalStreamsLoading = MutableStateFlow(false)
    val externalStreamsLoading: StateFlow<Boolean> = _externalStreamsLoading.asStateFlow()
    private val _externalStreamsError = MutableStateFlow<String?>(null)
    val externalStreamsError: StateFlow<String?> = _externalStreamsError.asStateFlow()
    private val _externalStreamsSelectedCategory = MutableStateFlow("football")
    val externalStreamsSelectedCategory: StateFlow<String> = _externalStreamsSelectedCategory.asStateFlow()

    fun loadPortalLicense() {
        viewModelScope.launch {
            val license = portalLicenseManager.getSavedLicense()
            _portalLicense.value = license
            _portalLicenseStatus.value = portalLicenseManager.checkStatus(license)
        }
    }

    fun activatePortalLicense(key: String): LicenseResult {
        val result = portalLicenseManager.verifyKey(key)
        when (result) {
            is LicenseResult.Success -> {
                portalLicenseManager.saveActivation(result.license)
                _portalLicense.value = result.license
                _portalLicenseStatus.value = portalLicenseManager.checkStatus(result.license)
            }
            is LicenseResult.Failure -> {
                // Error is contained in result.message
            }
        }
        return result
    }

    val externalStreamsCategories = listOf(
        "football" to "Football",
        "basketball" to "Basketball",
        "tennis" to "Tennis",
        "mma" to "MMA"
    )

    fun loadExternalStreams(category: String) {
        _externalStreamsSelectedCategory.value = category
        _externalStreamsLoading.value = true
        _externalStreamsError.value = null
        viewModelScope.launch {
            val matches = ExternalStreamsClient.getMatches(category)
            _externalStreamsMatches.value = matches
            _externalStreamsLoading.value = false
            if (matches.isEmpty()) _externalStreamsError.value = "No matches found"
        }
    }

    fun loadExternalStreamsMatches() = loadExternalStreams("football")

    fun setExternalStreamsCategory(category: String) = loadExternalStreams(category)

    private val _vodForActiveSource = MutableStateFlow<List<com.robbdeeze.nuviotv.domain.model.IptvVodItem>>(emptyList())
    val vodForActiveSource: StateFlow<List<com.robbdeeze.nuviotv.domain.model.IptvVodItem>> = _vodForActiveSource.asStateFlow()

    private val _seriesForActiveSource = MutableStateFlow<List<com.robbdeeze.nuviotv.domain.model.IptvSeries>>(emptyList())
    val seriesForActiveSource: StateFlow<List<com.robbdeeze.nuviotv.domain.model.IptvSeries>> = _seriesForActiveSource.asStateFlow()

    fun loadVodForSource(source: com.robbdeeze.nuviotv.domain.model.IptvSource) {
        viewModelScope.launch {
            _vodForActiveSource.value = iptvRepository.getVod(source)
        }
    }

    fun loadSeriesForSource(source: com.robbdeeze.nuviotv.domain.model.IptvSource) {
        viewModelScope.launch {
            _seriesForActiveSource.value = iptvRepository.getSeries(source)
        }
    }

    fun setSubScreen(screen: HubSubScreen) {
        _subScreen.value = screen
    }

    fun requestReset() {
        _subScreen.value = HubSubScreen.Hub
        viewModelScope.launch {
            _resetEvent.emit(Unit)
        }
    }

    fun loadIptvChannels(source: IptvSource) {
        iptvLoadJob?.cancel()
        _iptvLoading.value = true
        _iptvChannels.value = emptyList()
        _iptvLoadProgress.value = "Loading..."
        iptvLoadJob = viewModelScope.launch {
            val seen = mutableSetOf<String>()
            val channels = mutableListOf<IptvChannel>()
            iptvRepository.getChannelsFlow(source).collect { ch ->
                val key = ch.url
                if (key !in seen) {
                    seen.add(key)
                    channels.add(ch)
                    if (channels.size % 100 == 0) {
                        _iptvChannels.value = channels.toList()
                        _iptvLoadProgress.value = "Loaded ${channels.size} channels..."
                    }
                }
            }
            _iptvChannels.value = channels.toList()
            _iptvCategories.value = channels.mapNotNull { it.categoryName }.distinct().sorted()
            _iptvLoadProgress.value = ""
            _iptvLoading.value = false
        }
    }

    fun addIptvSource(name: String, url: String, type: String, epgUrl: String? = null) {
        viewModelScope.launch {
            iptvRepository.addSource(IptvSource(name, url, type, epgUrl))
        }
    }

    fun removeIptvSource(url: String) {
        viewModelScope.launch {
            iptvRepository.removeSource(url)
        }
    }

    fun refreshIptvSource(name: String, url: String, type: String) {
        viewModelScope.launch {
            iptvRepository.removeSource(url)
            iptvRepository.addSource(IptvSource(name, url, type))
            _activeIptvSource.value = null
            _activeIptvSource.value = IptvSource(name, url, type)
        }
    }

    fun toggleIptvFavorite(channel: IptvChannel, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) {
                iptvRepository.addFavorite(channel)
            } else {
                iptvRepository.removeFavorite(channel.id)
            }
        }
    }

    fun loadAllIptvChannels(sources: List<IptvSource>) {
        iptvLoadJob?.cancel()
        _iptvLoading.value = true
        _iptvChannels.value = emptyList()
        _iptvLoadProgress.value = "Loading..."
        iptvLoadJob = viewModelScope.launch {
            val seen = mutableSetOf<String>()
            val channels = mutableListOf<IptvChannel>()
            for (source in sources) {
                iptvRepository.getChannelsFlow(source).collect { ch ->
                    val key = ch.url
                    if (key !in seen) {
                        seen.add(key)
                        channels.add(ch)
                        if (channels.size % 100 == 0) {
                            _iptvChannels.value = channels.toList()
                            _iptvLoadProgress.value = "Loaded ${channels.size} channels..."
                        }
                    }
                }
            }
            _iptvChannels.value = channels.toList()
            _iptvCategories.value = channels.mapNotNull { it.categoryName }.distinct().sorted()
            _iptvLoadProgress.value = ""
            _iptvLoading.value = false
        }
    }

    fun loadAllIptvChannelsForQuick(sources: List<IptvSource>) {
        viewModelScope.launch {
            val seen = mutableSetOf<String>()
            val all = mutableListOf<IptvChannel>()
            for (source in sources) {
                iptvRepository.getChannelsFlow(source).collect { ch ->
                    if (ch.url !in seen) { seen.add(ch.url); all.add(ch) }
                }
            }
            _allIptvChannels.value = all.toList()
        }
    }

    fun loadIptvChannelsWithName(source: IptvSource) {
        _activeIptvSourceName.value = source.name
        loadIptvChannels(source)
    }

    fun setIptvSearchQuery(query: String) {
        _iptvSearchQuery.value = query
    }

    fun loadSportsScoreboard(league: SportLeague) {
        viewModelScope.launch {
            _activeSportLeague = league
            val cacheKey = "scoreboard_${league.id}"
            if (isCacheValid(cacheKey)) {
                _sportsEvents.value = sportsCache[cacheKey]!!.data
                return@launch
            }
            _sportsLoading.value = true
            var events = emptyList<SportEvent>()

            // "now" league: aggregate all live events across every league instead
            // of hitting ESPN's invalid now/_ scoreboard slug.
            if (league.id == "now") {
                loadAllLiveEvents()
                events = _allUpcomingEvents.value
                if (events.isNotEmpty()) sportsCache[cacheKey] = CacheEntry(events)
                _sportsEvents.value = events
                _sportsLoading.value = false
                return@launch
            }

            // PPV/Special Events: skip ESPN, search YouTube directly
            if (league.id == "ppv") {
                try {
                    val queries = listOf(
                        "PPV events 2026 highlights", "UFC PPV", "WrestleMania highlights",
                        "${league.abbreviation} PPV", "boxing PPV highlights", "MMA PPV"
                    )
                    val seen = mutableSetOf<String>()
                    for (q in queries) {
                        val results = PlatformYouTubeSearch.search(q)
                        for (v in results) {
                            if (v.videoId in seen) continue
                            seen.add(v.videoId)
                            events = events + SportEvent(
                                id = "ppv_${v.videoId}", name = v.title, date = "", status = "Highlights",
                                homeTeam = SportTeam(id = "", name = v.channelName, displayName = v.channelName),
                                awayTeam = SportTeam(id = "", name = "PPV", displayName = "PPV Event"),
                                leagueAbbreviation = "PPV",
                            )
                            if (events.size >= 20) break
                        }
                        if (events.size >= 20) break
                    }
                } catch (e: Exception) { e.printStackTrace() }
                _sportsEvents.value = events
                _sportsLoading.value = false
                return@launch
            }

            // 1. Try ESPN
            val combatOnly = league.id in setOf("mma", "ufc", "bkfc", "powerslap", "boxing", "pfl")
            if (!combatOnly || league.id in setOf("ufc", "pfl")) {
                try {
                val parts = league.slug.split("/")
                val sport = parts[0]
                val leagueName = parts[1]
                val response = sportsClient.getScoreboard(sport, leagueName)
                events = response.events?.mapNotNull { e ->
                    val competition = e.competitions?.firstOrNull()
                    val homeCompetitor = competition?.competitors?.firstOrNull { it.homeAway == "home" }
                    val awayCompetitor = competition?.competitors?.firstOrNull { it.homeAway == "away" }
                    if (homeCompetitor == null || awayCompetitor == null) return@mapNotNull null
                    SportEvent(
                        id = e.id, name = e.name, date = e.date, status = e.status.type.detail,
                        homeTeam = SportTeam(id = homeCompetitor.team?.id ?: "", name = homeCompetitor.team?.name ?: "", displayName = homeCompetitor.team?.displayName ?: "", logoUrl = homeCompetitor.team?.logo),
                        awayTeam = SportTeam(id = awayCompetitor.team?.id ?: "", name = awayCompetitor.team?.name ?: "", displayName = awayCompetitor.team?.displayName ?: "", logoUrl = awayCompetitor.team?.logo),
                        homeScore = homeCompetitor.score, awayScore = awayCompetitor.score,
                        leagueAbbreviation = league.abbreviation,
                    )
                } ?: emptyList()
            } catch (e: Exception) { e.printStackTrace() }

                // 1b. Enrich fighting event images with Wikipedia thumbnails
                if (events.isNotEmpty() && league.id in setOf("ufc", "boxing", "pfl")) {
                for (i in events.indices) {
                    val ev = events[i]
                    if (ev.homeTeam.logoUrl != null && ev.awayTeam.logoUrl != null) continue
                    val thumbnail = WikipediaClient.getThumbnail(ev.name)
                    if (thumbnail != null) {
                        events = events.toMutableList().apply {
                            set(i, ev.copy(
                                homeTeam = ev.homeTeam.copy(logoUrl = ev.homeTeam.logoUrl ?: thumbnail),
                                awayTeam = ev.awayTeam.copy(logoUrl = ev.awayTeam.logoUrl ?: thumbnail),
                            ))
                        }
                    }
                }
            }
            }

            // 2. If empty, try TheSportsDB
            if (events.isEmpty()) {
                val leagueId = theSportsDbClient.getLeagueId(league.slug)
                if (leagueId.isNotEmpty()) {
                    try {
                        val upcoming = theSportsDbClient.getUpcomingEvents(leagueId)
                        upcoming?.events?.forEach { dbEvent ->
                            events = events + SportEvent(
                                id = dbEvent.idEvent, name = dbEvent.strEvent, date = dbEvent.dateEvent,
                                status = if (dbEvent.intHomeScore != null) "Final" else "Scheduled",
                                homeTeam = SportTeam(id = "", name = dbEvent.strHomeTeam, displayName = dbEvent.strHomeTeam),
                                awayTeam = SportTeam(id = "", name = dbEvent.strAwayTeam, displayName = dbEvent.strAwayTeam),
                                homeScore = dbEvent.intHomeScore, awayScore = dbEvent.intAwayScore,
                                leagueAbbreviation = league.abbreviation,
                            )
                        }
                        val past = theSportsDbClient.getPastEvents(leagueId)
                        past?.events?.forEach { dbEvent ->
                            if (events.none { it.id == dbEvent.idEvent }) {
                                events = events + SportEvent(
                                    id = dbEvent.idEvent, name = dbEvent.strEvent, date = dbEvent.dateEvent,
                                    status = "Final",
                                    homeTeam = SportTeam(id = "", name = dbEvent.strHomeTeam, displayName = dbEvent.strHomeTeam),
                                    awayTeam = SportTeam(id = "", name = dbEvent.strAwayTeam, displayName = dbEvent.strAwayTeam),
                                    homeScore = dbEvent.intHomeScore, awayScore = dbEvent.intAwayScore,
                                    leagueAbbreviation = league.abbreviation,
                                )
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            // 3. Calendar iCal feeds (fixtur.es / UFC-cal) for real scheduled fixtures
            if (events.isEmpty()) {
                try {
                    val calendarEvents = com.robbdeeze.nuviotv.data.sports.calendar.SportsCalendarRepository
                        .upcomingFor(league.id, league.abbreviation, hoursWindow = 60 * 24L)
                    if (calendarEvents.isNotEmpty()) {
                        events = calendarEvents
                    }
                } catch (_: Exception) {}
            }

            // 3b. MMA umbrella: aggregate all combat sport sync events
            if (league.id == "mma") {
                val combatIds = listOf("ufc", "pfl", "bkfc", "boxing", "powerslap")
                val combined = mutableListOf<SportEvent>()
                combatIds.forEach { cid ->
                    val syncEvents = _sync2CalEvents.value[cid] ?: return@forEach
                    syncEvents.take(12).forEachIndexed { i, ev ->
                        val teams = splitSyncTeams(ev.title)
                        combined.add(
                            SportEvent(
                                id = "mma_${cid}_$i", name = ev.title, date = ev.startTime,
                                status = if (isUpcomingSoon(ev.startTime)) "Scheduled" else "Scheduled",
                                homeTeam = teams.first, awayTeam = teams.second,
                                leagueAbbreviation = when (cid) {
                                    "ufc" -> "UFC"; "pfl" -> "PFL"; "bkfc" -> "BKFC";
                                    "boxing" -> "BOX"; else -> "SLAP"
                                },
                            )
                        )
                    }
                }
                if (combined.isNotEmpty()) {
                    events = combined.sortedBy { it.date }.take(40)
                }
            }

            // 4. Sync2Cal JSON events (reliable for NBA/WNBA/MLB/etc even off-season)
            if (events.isEmpty()) {
                try {
                    val syncEvents = _sync2CalEvents.value[league.id] ?: emptyList()
                    if (syncEvents.isNotEmpty()) {
                        events = syncEvents.take(40).mapIndexed { i, ev ->
                            val teams = splitSyncTeams(ev.title)
                            SportEvent(
                                id = "s2c_${league.id}_$i", name = ev.title, date = ev.startTime,
                                status = "Scheduled",
                                homeTeam = teams.first, awayTeam = teams.second,
                                leagueAbbreviation = league.abbreviation,
                            )
                        }
                    }
                } catch (_: Exception) {}
            }

            // 5. If still empty, search YouTube for league highlights
            if (events.isEmpty()) {
                try {
                    val highlights = PlatformYouTubeSearch.search("${league.name} highlights 2026")
                    events = highlights.take(10).mapIndexed { i, v ->
                        SportEvent(
                            id = "yt_$i", name = v.title, date = "", status = "Highlights",
                            homeTeam = SportTeam(id = "", name = v.channelName, displayName = v.channelName),
                            awayTeam = SportTeam(id = "", name = "Highlights", displayName = "Highlights"),
                            leagueAbbreviation = league.abbreviation,
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            // 6. YouTube trending fallback when all else fails
            if (events.isEmpty()) {
                try {
                    val trending = PlatformYouTubeSearch.search("${league.name} 2026")
                    events = trending.take(12).mapIndexed { i, v ->
                        SportEvent(
                            id = "yt_fallback_$i", name = v.title, date = "", status = "Highlights",
                            homeTeam = SportTeam(id = "", name = v.channelName, displayName = v.channelName),
                            awayTeam = SportTeam(id = "", name = league.name, displayName = league.name),
                            leagueAbbreviation = league.abbreviation,
                        )
                    }
                } catch (_: Exception) {}
            }

            _sportsError.value = if (events.isEmpty()) "No events available for ${league.name}" else null
            _sportsEvents.value = events
            if (events.isNotEmpty()) {
                sportsCache[cacheKey] = CacheEntry(events)
            } else {
                sportsCache.remove(cacheKey)
            }
            _sportsLoading.value = false
        }
    }

    fun loadLeagueHighlights(league: SportLeague) {
        viewModelScope.launch {
            if (_sportVideosLoading.value) return@launch
            _sportVideosLoading.value = true
            try {
                val searchTerm = when (league.id) {
                    "mma" -> "UFC PFL BKFC boxing highlights"
                    "ufc" -> "UFC highlights"
                    "bkfc" -> "BKFC bare knuckle highlights"
                    "powerslap" -> "Power Slap highlights"
                    "boxing" -> "boxing highlights"
                    "pfl" -> "PFL highlights"
                    "ppv" -> "MMA PPV highlights"
                    else -> "${league.name} highlights"
                }
                val existingEventNames = _sportsEvents.value.map { it.name.lowercase() }.toSet()
                val results = com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch.search(searchTerm)
                val seenIds = mutableSetOf<String>()
                val videos = results.filter { v ->
                    if (v.videoId in seenIds) return@filter false
                    seenIds.add(v.videoId)
                    val lower = v.title.lowercase()
                    existingEventNames.none { lower.contains(it) || it.contains(lower) }
                }.take(10).map {
                    SportEventVideo(
                        videoId = it.videoId, title = it.title, thumbnailUrl = it.thumbnailUrl,
                        channelName = it.channelName, durationSeconds = it.durationSeconds,
                        category = league.abbreviation,
                    )
                }
                _sportEventVideos.value = videos
            } catch (_: Exception) {
                _sportEventVideos.value = emptyList()
            }
            _sportVideosLoading.value = false
        }
    }

    fun selectTeam(teamName: String, teamLogo: String?) {
        _selectedTeam.value = TeamDetailState(teamName = teamName, teamLogo = teamLogo)
    }

    fun clearTeamSelection() {
        _selectedTeam.value = null
    }

    fun selectSportEvent(event: SportEvent) {
        _selectedSportEvent.value = event
        loadMatchedChannels(event)
    }

    fun loadAllLiveEvents() {
        viewModelScope.launch {
            _allLiveLoading.value = true
            val liveEvents = mutableListOf<SportEvent>()
            val upcomingEvents = mutableListOf<SportEvent>()
            val nowUtc = java.time.Instant.now()
            val oneWeekLater = nowUtc.plusSeconds(604800) // 7 days
            val allLeagues = _sportsLeagues.value.filter { it.id != "now" }
            for (league in allLeagues) {
                try {
                    val parts = league.slug.split("/")
                    if (parts.size < 2) continue
                    val response = sportsClient.getScoreboard(parts[0], parts[1])
                    val parsed = response.events?.mapNotNull { e ->
                        val c = e.competitions?.firstOrNull() ?: return@mapNotNull null
                        val h = c.competitors?.firstOrNull { it.homeAway == "home" } ?: return@mapNotNull null
                        val a = c.competitors?.firstOrNull { it.homeAway == "away" } ?: return@mapNotNull null
                        SportEvent(id = e.id, name = e.name, date = e.date, status = e.status.type.detail,
                            homeTeam = SportTeam(id = h.team?.id ?: "", name = h.team?.name ?: "", displayName = h.team?.displayName ?: "", logoUrl = h.team?.logo),
                            awayTeam = SportTeam(id = a.team?.id ?: "", name = a.team?.name ?: "", displayName = a.team?.displayName ?: "", logoUrl = a.team?.logo),
                            homeScore = h.score, awayScore = a.score, leagueAbbreviation = league.abbreviation)
                    } ?: emptyList()
                    for (event in parsed) {
                        if (event.isLive) {
                            liveEvents.add(event)
                        } else {
                            try {
                                val eventTime = java.time.Instant.parse(event.date)
                                if (eventTime in nowUtc..oneWeekLater) {
                                    upcomingEvents.add(event)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}
            }
            // Search YouTube for sports news
            try {
                val news = PlatformYouTubeSearch.search("sports news 2026 today")
                news.take(4).forEachIndexed { i, yt ->
                    val newsEvent = SportEvent(
                        id = "news_$i", name = yt.title, date = "", status = "News",
                        homeTeam = SportTeam(id = "", name = yt.channelName, displayName = yt.channelName),
                        awayTeam = SportTeam(id = "", name = "Sports News", displayName = "Sports News"),
                        leagueAbbreviation = "NEWS",
                    )
                    upcomingEvents.add(newsEvent)
                }
            } catch (_: Exception) {}

            // Enrich upcoming with iCal calendar fixtures for leagues with no ESPN data
            try {
                val calendarUpcoming =
                    com.robbdeeze.nuviotv.data.sports.calendar.SportsCalendarRepository.allUpcoming()
                val existingIds = upcomingEvents.map { it.id }.toSet()
                calendarUpcoming.forEach { calEvent ->
                    if (calEvent.id !in existingIds) upcomingEvents.add(calEvent)
                }
            } catch (_: Exception) {}
            // Store in shared bridge for player overlay
            SportsNowStore.liveEvents = liveEvents
            _allLiveEvents.value = liveEvents
            _allUpcomingEvents.value = upcomingEvents.sortedBy { it.date }
            _allLiveLoading.value = false
        }
    }

    fun loadSync2CalEvents() {
        viewModelScope.launch {
            if (_sync2CalLoading.value) return@launch
            _sync2CalLoading.value = true
            try {
                val allResults = mutableMapOf<String, List<Sync2CalEvent>>()
                val allTvChannels = mutableMapOf<Long, List<Sync2CalTvChannel>>()
                for (mapping in Sync2CalMappings.leagueMappings) {
                    try {
                        val category = Sync2CalClient.lookupBySlug(mapping.sync2calSlug)
                        if (category != null) {
                            val events = Sync2CalClient.getFilteredEvents(category.uuid)
                            val enriched = events.map { it.copy(title = it.title, startTime = it.startTime) }
                            val tvChannels = enriched.associate { ev -> ev.id to extractTvChannels(ev) }
                            allResults[mapping.leagueId] = enriched
                            allTvChannels.putAll(tvChannels)
                        }
                    } catch (_: Exception) {}
                }
                _sync2CalEvents.value = allResults
                _sync2CalTvChannels.value = allTvChannels
            } catch (_: Exception) {}
            _sync2CalLoading.value = false
            _activeSportLeague?.let { selected ->
                sportsCache.remove("scoreboard_${selected.id}")
                loadSportsScoreboard(selected)
            }
        }
    }

    /**
     * Loads upcoming games grouped by local day for the Sports calendar. Aggregates
     * iCal fixture feeds (fixtur.es / UFC-cal) plus Sync2Cal JSON events so every
     * registered league gets day-grouped schedule entries.
     */
    fun loadSportsCalendar(hoursAhead: Long = 14 * 24L) {
        viewModelScope.launch {
            if (_sportsCalendarLoading.value) return@launch
            _sportsCalendarLoading.value = true
            val now = java.time.Instant.now()
            val events = mutableListOf<SportEvent>()

            // 1. iCal fixture feeds across all registered leagues
            try {
                val calEvents =
                    com.robbdeeze.nuviotv.data.sports.calendar.SportsCalendarRepository
                        .allUpcoming(now = now, hoursWindow = hoursAhead)
                events.addAll(calEvents)
            } catch (_: Exception) {}

            // 2. Sync2Cal JSON events keyed by league (reliable even off-season)
            try {
                val sync = _sync2CalEvents.value
                if (sync.isNotEmpty()) {
                    sync.forEach { (leagueId, syncEvents) ->
                        val abbr = Sync2CalMappings.leagueNameFromId(leagueId)
                        syncEvents.take(40).forEachIndexed { i, ev ->
                            val teams = splitSyncTeams(ev.title)
                            events.add(
                                SportEvent(
                                    id = "s2c_cal_${leagueId}_$i", name = ev.title, date = ev.startTime,
                                    status = "Scheduled",
                                    homeTeam = teams.first, awayTeam = teams.second,
                                    leagueAbbreviation = abbr,
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            val grouped: List<SportCalendarDay> = events
                .mapNotNull { e ->
                    val d = eventStartDate(e.date) ?: return@mapNotNull null
                    d to e
                }
                .filter { (d, _) -> !d.isBefore(java.time.LocalDate.now()) }
                .sortedWith(compareBy({ it.first }, { it.second.date }))
                .groupBy({ it.first }, { it.second })
                .map { (d, evs) -> SportCalendarDay(date = d, events = evs.distinctBy { it.id }.take(12)) }

            _sportsCalendarDays.value = grouped
            _sportsCalendarLoading.value = false
        }
    }

    private fun eventStartDate(dateStr: String): java.time.LocalDate? =
        try {
            java.time.Instant.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        } catch (_: Exception) {
            null
        }

    private fun isUpcomingSoon(dateStr: String): Boolean {
        return try {
            val t = java.time.Instant.parse(dateStr)
            t >= java.time.Instant.now() && t <= java.time.Instant.now().plusSeconds(7 * 86400L)
        } catch (_: Exception) { false }
    }

    private fun splitSyncTeams(title: String): Pair<SportTeam, SportTeam> {
        val parts = title.split(Regex("""\s+vs\.?\s+""", RegexOption.IGNORE_CASE))
        return if (parts.size >= 2) {
            val home = parts[0].trim().replace(Regex("""^[^a-zA-Z0-9]+"""), "").ifBlank { "Home Team" }
            val away = parts.drop(1).joinToString(" ").trim().replace(Regex("""[^a-zA-Z0-9 ]+$"""), "").ifBlank { "Away Team" }
            SportTeam(id = "", name = home, displayName = home) to SportTeam(id = "", name = away, displayName = away)
        } else {
            SportTeam(id = "", name = title, displayName = title) to SportTeam(id = "", name = "TBD", displayName = "TBD")
        }
    }

    private fun extractTvChannels(event: Sync2CalEvent): List<Sync2CalTvChannel> {
        if (event.description.isNullOrBlank()) return emptyList()
        val tvLine = Regex("""TV:\s*([^\n]+)""", RegexOption.IGNORE_CASE)
            .find(event.description)?.groupValues?.getOrNull(1) ?: return emptyList()
        return tvLine.split(Regex(""",\s*|\s*/\s*|\s+&\s+"""))
            .map { it.trim().replace("^[^a-zA-Z0-9]+".toRegex(), "").takeIf { it.isNotBlank() } }
            .filterNotNull()
            .map { Sync2CalTvChannel(name = it) }
    }

    fun loadDaddyLiveEvents() {
        viewModelScope.launch {
            _daddyLiveLoading.value = true
            val events = DaddyLiveClient.fetchEvents()
            _daddyLiveEvents.value = events
            _daddyLiveLoading.value = false
        }
    }

    fun loadUfcUpcoming(league: SportLeague = _sportsLeagues.value.find { it.id == "ufc" } ?: _sportsLeagues.value.first()) {
        viewModelScope.launch {
            _ufcUpcomingLoading.value = true
            val allDbEvents = mutableListOf<TheSportsDbEvent>()
            val leagueId = theSportsDbClient.getLeagueId(league.slug)
            if (leagueId.isNotEmpty()) {
                try {
                    val upcoming = theSportsDbClient.getUpcomingEvents(leagueId)
                    upcoming?.events?.filter { it.strEvent.isNotBlank() }?.let { allDbEvents.addAll(it) }
                    val past = theSportsDbClient.getPastEvents(leagueId)
                    past?.events?.filter { it.strEvent.isNotBlank() }?.take(10)?.let { allDbEvents.addAll(it) }
                } catch (_: Exception) {}
            }
            // Add YouTube highlights for this sport
            val searchTerm = when (league.id) {
                "ufc" -> "UFC MMA"
                "pfl" -> "PFL MMA"
                "boxing" -> "Boxing"
                else -> league.name
            }
            try {
                val highlights = PlatformYouTubeSearch.search("$searchTerm highlights 2026")
                highlights.take(5).forEach { yt ->
                    allDbEvents.add(TheSportsDbEvent(
                        idEvent = "yt_${yt.videoId}",
                        strEvent = yt.title,
                        strHomeTeam = yt.channelName,
                        strAwayTeam = "Highlights",
                        dateEvent = "",
                        strThumb = yt.thumbnailUrl,
                    ))
                }
            } catch (_: Exception) {}
            _ufcUpcomingEvents.value = allDbEvents.take(30)
            _ufcUpcomingLoading.value = false
        }
    }

    fun clearSportSelection() {
        _selectedSportEvent.value = null
        _matchedChannels.value = emptyList()
    }

    fun loadMatchedChannels(event: SportEvent) {
        viewModelScope.launch {
            _sportsChannelLoading.value = true
            val allChannels = mutableListOf<Pair<IptvChannel, String>>()
            iptvSources.first().forEach { source ->
                try {
                    val channels = iptvRepository.getChannels(source)
                    channels.forEach { ch -> allChannels.add(ch to source.name) }
                } catch (_: Exception) {}
            }
            val matches = allChannels.flatMap { (ch, srcName) ->
                GameToChannelMatcher.matchChannels(event, listOf(ch), srcName)
            }.map { mc ->
                val reg = detectRegion(mc.channel)
                mc.copy(region = reg)
            }
            _matchedChannels.value = matches
            _sportsChannelLoading.value = false
        }
    }

    fun matchSportEventChannels(channelNames: List<String>) {
        viewModelScope.launch {
            _sportsChannelLoading.value = true
            val sources = iptvSources.first()
            val matched = mutableListOf<MatchedChannel>()
            for (src in sources) {
                try {
                    val chs = iptvRepository.getChannels(src)
                    for (ch in chs) {
                        if (matched.size >= MAX_SPORT_CHANNEL_MATCHES) break
                        val chLower = ch.name.lowercase()
                        val matchName = channelNames.firstOrNull { cname ->
                            val n = cname.lowercase().trim()
                            chLower.contains(n) || n.contains(chLower)
                        }
                        if (matchName != null) {
                            matched.add(MatchedChannel(ch, MatchType.LEAGUE, src.name))
                        }
                    }
                } catch (_: Exception) {}
                if (matched.size >= MAX_SPORT_CHANNEL_MATCHES) break
            }
            _matchedChannels.value = matched.distinctBy { it.channel.url }.take(MAX_SPORT_CHANNEL_MATCHES)
            _sportsChannelLoading.value = false
        }
    }

    private fun detectRegion(channel: IptvChannel): String {
        val cn = channel.name.lowercase()
        val cat = (channel.categoryName ?: "").lowercase()
        return when {
            cn.contains("bbc") || cn.contains("itv") || cn.contains("channel 4") || cn.contains("sky sports") ||
            cn.contains("bt sport") || cn.contains("uk") || cn.contains("british") ||
            cat.contains("uk") || cat.contains("united kingdom") -> "UK"
            cn.contains("cbc") || cn.contains("tsn") || cn.contains("sportsnet") ||
            cn.contains("canada") || cn.contains("toronto") ||
            cat.contains("canada") || cat.contains("canadian") -> "CA"
            cn.contains("fox") || cn.contains("espn") || cn.contains("cbs") || cn.contains("nbc") ||
            cn.contains("abc") || cn.contains("nfl network") || cn.contains("nba tv") ||
            cn.contains("mlb network") || cn.contains("nhl network") ||
            cn.contains("us") || cn.contains("american") ||
            cat.contains("us") || cat.contains("usa") -> "US"
            else -> "Other"
        }
    }

    fun searchSportVideos(event: SportEvent, isFuture: Boolean) {
        viewModelScope.launch {
            _sportVideosLoading.value = true
            _sportEventVideos.value = emptyList()
            val team1 = event.awayTeam.displayName
            val team2 = event.homeTeam.displayName
            val queries = if (isFuture) {
                listOf(
                    "$team1 vs $team2 preview",
                    "$team1 vs $team2 predictions",
                    "$team1 vs $team2 weigh in",
                )
            } else {
                listOf(
                    "$team1 vs $team2 highlights",
                    "$team1 vs $team2 full fight",
                    "$team1 vs $team2 post fight analysis",
                    "$team2 vs $team1 highlights",
                )
            }
            val allVideos = mutableListOf<SportEventVideo>()
            val seen = mutableSetOf<String>()
            for (query in queries) {
                try {
                    val results = PlatformYouTubeSearch.search(query)
                    for (v in results) {
                        if (v.videoId !in seen) {
                            seen.add(v.videoId)
                            allVideos.add(SportEventVideo(
                                videoId = v.videoId,
                                title = v.title,
                                thumbnailUrl = v.thumbnailUrl,
                                channelName = v.channelName,
                                durationSeconds = v.durationSeconds,
                                category = query.substringAfterLast(" ").ifEmpty { "highlights" },
                            ))
                        }
                    }
                } catch (_: Exception) {}
                if (allVideos.size >= 30) break
            }
            _sportEventVideos.value = allVideos
            _sportVideosLoading.value = false
        }
    }

    fun setSportRegionFilter(region: String) {
        _sportRegionFilter.value = region
    }

    fun setActiveEventTab(tab: EventTab) {
        _activeEventTab.value = tab
    }

    fun startSportsAutoRefresh(league: SportLeague) {
        sportsRefreshJob?.cancel()
        sportsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(45_000)
                loadSportsScoreboard(league)
            }
        }
    }

    fun stopSportsAutoRefresh() {
        sportsRefreshJob?.cancel()
        sportsRefreshJob = null
    }

    // --- VidNutz ---

    fun loadVidNutzVideos(category: VidNutzCategory) {
        viewModelScope.launch {
            _vidNutzUiState.value = _vidNutzUiState.value.copy(
                selectedCategory = category,
                videos = emptyList(),
                searchResults = null,
                searchQuery = "",
                currentPage = 1,
                hasMore = true,
                isLoading = true,
            )
            val videos = vidNutzRepository.getVideosByCategory(category, 1)
            _vidNutzUiState.value = _vidNutzUiState.value.copy(
                videos = videos,
                isLoading = false,
                hasMore = videos.isNotEmpty(),
            )
        }
    }

    fun searchVidNutz(query: String) {
        viewModelScope.launch {
            _vidNutzUiState.value = _vidNutzUiState.value.copy(
                searchQuery = query,
                searchResults = null,
                searchCurrentPage = 1,
                searchHasMore = true,
            )
            if (query.isNotBlank()) {
                val results = vidNutzRepository.searchVideos(query, 1)
                _vidNutzUiState.value = _vidNutzUiState.value.copy(
                    searchResults = results,
                    searchHasMore = results.isNotEmpty(),
                )
            }
        }
    }

    fun loadMoreVidNutz() {
        val state = _vidNutzUiState.value
        if (state.isLoadingMore || !state.hasMore) return
        val isSearching = state.searchQuery.isNotBlank()
        val nextPage = if (isSearching) state.searchCurrentPage + 1 else state.currentPage + 1
        viewModelScope.launch {
            _vidNutzUiState.value = _vidNutzUiState.value.copy(isLoadingMore = true)
            val more = if (isSearching) {
                vidNutzRepository.searchVideos(state.searchQuery, nextPage)
            } else {
                vidNutzRepository.getVideosByCategory(state.selectedCategory, nextPage)
            }
            if (more.isNotEmpty()) {
                if (isSearching) {
                    _vidNutzUiState.value = _vidNutzUiState.value.copy(
                        searchResults = (state.searchResults ?: emptyList()) + more,
                        searchCurrentPage = nextPage,
                        searchHasMore = true,
                        isLoadingMore = false,
                    )
                } else {
                    _vidNutzUiState.value = _vidNutzUiState.value.copy(
                        videos = state.videos + more,
                        currentPage = nextPage,
                        hasMore = true,
                        isLoadingMore = false,
                    )
                }
            } else {
                _vidNutzUiState.value = _vidNutzUiState.value.copy(
                    isLoadingMore = false,
                    hasMore = false,
                    searchHasMore = false,
                )
            }
        }
    }

    // --- MusicNutz ---

    fun loadMusicNutzTracks(category: MusicNutzCategory) {
        viewModelScope.launch {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                selectedCategory = category,
                tracks = emptyList(),
                searchResults = null,
                searchQuery = "",
                currentPage = 1,
                hasMore = true,
                isLoading = true,
            )
            val tracks = musicNutzRepository.getTracksByCategory(category, 1)
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                tracks = tracks,
                isLoading = false,
                hasMore = tracks.isNotEmpty(),
            )
        }
    }

    fun loadMusicNutzAlbums(category: MusicNutzCategory) {
        viewModelScope.launch {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                selectedCategory = category,
                albums = emptyList(),
                albumResults = null,
                searchQuery = "",
                albumPage = 1,
                albumHasMore = true,
                isLoadingAlbums = true,
            )
            val albums = musicNutzRepository.getAlbumsByCategory(category, 1)
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                albums = albums,
                isLoadingAlbums = false,
                albumHasMore = albums.isNotEmpty(),
            )
        }
    }

    fun searchMusicNutzTracks(query: String) {
        viewModelScope.launch {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                searchQuery = query,
                searchResults = null,
                searchCurrentPage = 1,
                searchHasMore = true,
            )
            if (query.isNotBlank()) {
                val results = musicNutzRepository.searchTracks(query, 1)
                _musicNutzUiState.value = _musicNutzUiState.value.copy(
                    searchResults = results,
                    searchHasMore = results.isNotEmpty(),
                )
            }
        }
    }

    fun searchMusicNutzAlbums(query: String) {
        viewModelScope.launch {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                searchQuery = query,
                albumResults = null,
                albumPage = 1,
                albumHasMore = true,
            )
            if (query.isNotBlank()) {
                val results = musicNutzRepository.searchAlbums(query, 1)
                _musicNutzUiState.value = _musicNutzUiState.value.copy(
                    albumResults = results,
                    albumHasMore = results.isNotEmpty(),
                )
            }
        }
    }

    fun selectAlbum(album: MusicAlbum) {
        viewModelScope.launch {
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                selectedAlbum = album,
                isLoadingAlbumTracks = true,
            )
            val tracks = musicNutzRepository.getAlbumTracks(album.id)
            _musicNutzUiState.value = _musicNutzUiState.value.copy(
                albumTracks = tracks,
                isLoadingAlbumTracks = false,
            )
        }
    }

    fun dismissAlbumDetail() {
        _musicNutzUiState.value = _musicNutzUiState.value.copy(
            selectedAlbum = null,
            albumTracks = emptyList(),
        )
    }

    suspend fun resolveMusicTrackYoutubeId(track: MusicTrack): String? {
        return musicNutzRepository.resolveYoutubeId(track.title, track.artistName)
    }

    fun playMusicTrack(
        track: MusicTrack,
        onPlayChannel: (IptvChannel) -> Unit,
    ) {
        viewModelScope.launch {
            _playerLoadingMessage.value = "Resolving music..."
            try {
                val ytId = resolveMusicTrackYoutubeId(track)
                if (ytId != null) {
                    _playerLoadingMessage.value = "Preparing stream..."
                    val result = YouTubeStreamResolver.resolveStreamResult(ytId)
                    if (result != null) {
                        onPlayChannel(IptvChannel(
                            id = ytId, name = "${track.artistName} - ${track.title}",
                            url = result.videoUrl, logoUrl = track.albumCover,
                            audioUrl = result.audioUrl, qualities = result.qualities,
                        ))
                    }
                }
            } catch (_: Exception) {}
            _playerLoadingMessage.value = null
        }
    }

    // --- MusicNutz Playlists ---

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val playlist = MusicPlaylist(name = name.trim())
        val state = _musicNutzUiState.value
        val updated = state.playlists + playlist
        _musicNutzUiState.value = state.copy(
            playlists = updated,
            showCreatePlaylistDialog = false,
            newPlaylistName = "",
        )
        musicNutzStore.savePlaylists(updated)
    }

    fun showCreatePlaylistDialog(show: Boolean) {
        _musicNutzUiState.value = _musicNutzUiState.value.copy(
            showCreatePlaylistDialog = show,
            newPlaylistName = "",
        )
    }

    fun setNewPlaylistName(name: String) {
        _musicNutzUiState.value = _musicNutzUiState.value.copy(newPlaylistName = name)
    }

    fun deletePlaylist(playlistId: String) {
        val state = _musicNutzUiState.value
        val updated = state.playlists.filter { it.id != playlistId }
        _musicNutzUiState.value = state.copy(
            playlists = updated,
            selectedPlaylist = if (state.selectedPlaylist?.id == playlistId) null else state.selectedPlaylist,
        )
        musicNutzStore.savePlaylists(updated)
    }

    fun selectPlaylist(playlist: MusicPlaylist?) {
        _musicNutzUiState.value = _musicNutzUiState.value.copy(selectedPlaylist = playlist)
    }

    fun showAddToPlaylistDialog(show: Boolean, track: MusicTrack? = null) {
        _musicNutzUiState.value = _musicNutzUiState.value.copy(
            showAddToPlaylistDialog = show,
            pendingTrackForPlaylist = track,
        )
    }

    fun addTrackToPlaylist(playlistId: String, track: MusicTrack) {
        val state = _musicNutzUiState.value
        val updated = state.playlists.map { pl ->
            if (pl.id == playlistId && pl.tracks.none { it.id == track.id }) {
                pl.copy(tracks = pl.tracks + track)
            } else pl
        }
        _musicNutzUiState.value = state.copy(
            playlists = updated,
            showAddToPlaylistDialog = false,
            pendingTrackForPlaylist = null,
        )
        musicNutzStore.savePlaylists(updated)
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        val state = _musicNutzUiState.value
        val updated = state.playlists.map { pl ->
            if (pl.id == playlistId) pl.copy(tracks = pl.tracks.filter { it.id != trackId })
            else pl
        }
        _musicNutzUiState.value = state.copy(playlists = updated)
        musicNutzStore.savePlaylists(updated)
    }

    // --- MusicNutz Downloads ---

    fun downloadTrack(track: MusicTrack) {
        viewModelScope.launch {
            val state = _musicNutzUiState.value
            _musicNutzUiState.value = state.copy(downloadingTrackIds = state.downloadingTrackIds + track.id)
            kotlinx.coroutines.delay(2000)
            val downloadDir = musicNutzStore.ensureDownloadDir()
            val fileName = "${track.id}_${track.title.take(40).replace(Regex("[^a-zA-Z0-9_-]"), "")}.mp3"
            val localPath = downloadDir.resolve(fileName).absolutePath
            val downloaded = MusicDownloadedTrack(
                trackId = track.id,
                title = track.title,
                artistName = track.artistName,
                albumCover = track.albumCover,
                localPath = localPath,
            )
            val newState = _musicNutzUiState.value
            val updatedDownloads = newState.downloadedTracks + downloaded
            _musicNutzUiState.value = newState.copy(
                downloadedTracks = updatedDownloads,
                downloadingTrackIds = newState.downloadingTrackIds - track.id,
            )
            musicNutzStore.saveDownloadedTracks(updatedDownloads)
        }
    }

    fun deleteDownloadedTrack(trackId: Long) {
        val state = _musicNutzUiState.value
        val updated = state.downloadedTracks.filter { it.trackId != trackId }
        _musicNutzUiState.value = state.copy(downloadedTracks = updated)
        musicNutzStore.saveDownloadedTracks(updated)
    }

    // --- MusicNutz Saved Albums ---

    fun toggleSavedAlbum(albumId: Long) {
        val state = _musicNutzUiState.value
        val updated = if (albumId in state.savedAlbumIds) {
            state.savedAlbumIds - albumId
        } else {
            state.savedAlbumIds + albumId
        }
        _musicNutzUiState.value = state.copy(savedAlbumIds = updated)
        musicNutzStore.saveSavedAlbumIds(updated)
    }

    fun isAlbumSaved(albumId: Long): Boolean = albumId in _musicNutzUiState.value.savedAlbumIds

    // --- MusicNutz Mode Update ---

    fun setMusicNutzMode(mode: MusicNutzMode) {
        val state = _musicNutzUiState.value
        _musicNutzUiState.value = state.copy(mode = mode, searchQuery = "", searchResults = null, albumResults = null)
        when (mode) {
            MusicNutzMode.ALBUMS -> if (state.albums.isEmpty()) loadMusicNutzAlbums(state.selectedCategory)
            MusicNutzMode.TRACKS -> if (state.tracks.isEmpty()) loadMusicNutzTracks(state.selectedCategory)
            else -> {}
        }
    }

    // --- MagNutz ---

    fun initMagNutz() {
        viewModelScope.launch {
            try {
                magNutzRepository.startTorrServer()
            } catch (e: Exception) {
                _magNutzUiState.value = _magNutzUiState.value.copy(
                    errorMessage = "Failed to start TorrServer: ${e.message}"
                )
            }
            magNutzRepository.startPolling()
        }
    }

    fun disposeMagNutz() {
        magNutzRepository.stopPolling()
    }

    val magNutzTorrents: StateFlow<List<TorrentItem>> = magNutzRepository.torrents

    fun addMagnet(magnetUri: String) {
        viewModelScope.launch {
            _magNutzUiState.value = _magNutzUiState.value.copy(isLoading = true, errorMessage = null)
            val hash = magNutzRepository.addMagnet(magnetUri)
            if (hash == null) {
                _magNutzUiState.value = _magNutzUiState.value.copy(
                    isLoading = false,
                    errorMessage = "Invalid magnet URI or failed to add"
                )
            } else {
                _magNutzUiState.value = _magNutzUiState.value.copy(
                    isLoading = false,
                    showAddDialog = false,
                    magnetInput = "",
                )
            }
        }
    }

    fun setMagNutzFilter(status: TorrentStatus?) {
        _magNutzUiState.value = _magNutzUiState.value.copy(filter = status)
    }

    fun selectTorrent(torrent: TorrentItem?) {
        _magNutzUiState.value = _magNutzUiState.value.copy(selectedTorrent = torrent)
    }

    fun showMagNutzAddDialog(show: Boolean) {
        _magNutzUiState.value = _magNutzUiState.value.copy(showAddDialog = show, magnetInput = "", errorMessage = null)
    }

    fun setMagNutzMagnetInput(input: String) {
        _magNutzUiState.value = _magNutzUiState.value.copy(magnetInput = input)
    }

    fun pauseTorrent(torrentId: String) {
        viewModelScope.launch { magNutzRepository.pauseTorrent(torrentId) }
    }

    fun resumeTorrent(torrentId: String) {
        viewModelScope.launch { magNutzRepository.resumeTorrent(torrentId) }
    }

    fun removeTorrent(torrentId: String) {
        viewModelScope.launch {
            magNutzRepository.removeTorrent(torrentId)
            _magNutzUiState.value = _magNutzUiState.value.copy(selectedTorrent = null)
        }
    }

    fun dismissMagNutzError() {
        _magNutzUiState.value = _magNutzUiState.value.copy(errorMessage = null)
    }

    // --- Sports Standings ---

    fun loadStandings(league: SportLeague) {
        viewModelScope.launch {
            _standingsLoading.value = true
            _standingsError.value = null
            _standingsEntries.value = emptyList()
            val parts = league.slug.split("/")
            if (parts.size < 2 || league.id in setOf("mma", "bkfc", "powerslap", "ppv")) {
                _standingsLoading.value = false
                _standingsError.value = "No standings available for ${league.name}"
                return@launch
            }
            try {
                val response: com.robbdeeze.nuviotv.data.remote.dto.EspnStandingsResponse = sportsClient.getStandings(parts[0], parts[1])
                val containers: List<com.robbdeeze.nuviotv.data.remote.dto.EspnStandingContainer>? = response.standings
                val allEntries = containers?.flatMap { c -> c.entries ?: emptyList() } ?: emptyList()
                if (allEntries.isEmpty()) {
                    _standingsError.value = "No standings available for ${league.name}"
                } else {
                    _standingsEntries.value = allEntries
                }
            } catch (e: Exception) {
                _standingsError.value = "Failed to load standings. Tap Retry to try again."
            }
            _standingsLoading.value = false
        }
    }
}

data class TeamDetailState(
    val teamName: String,
    val teamLogo: String?,
)
