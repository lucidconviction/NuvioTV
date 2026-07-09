package com.nuvio.tv.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.data.youtube.PlatformYouTubeSearch
import com.nuvio.tv.domain.GameToChannelMatcher
import com.nuvio.tv.domain.model.*
import com.nuvio.tv.ui.screens.player.SportsNowStore
import com.nuvio.tv.domain.repository.IptvRepository
import com.nuvio.tv.domain.repository.MusicNutzRepository
import com.nuvio.tv.domain.repository.VidNutzRepository
import com.nuvio.tv.data.remote.api.SportsClient
import com.nuvio.tv.data.remote.api.TheSportsDbClient
import com.nuvio.tv.data.remote.dto.TheSportsDbEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HubSubScreen { Hub, Iptv, Sports, VidNutz, MusicNutz }

@HiltViewModel
class RobbdeezeNutzHubViewModel @Inject constructor(
    private val iptvRepository: IptvRepository,
    private val sportsClient: SportsClient,
    private val vidNutzRepository: VidNutzRepository,
    private val musicNutzRepository: MusicNutzRepository,
    private val theSportsDbClient: TheSportsDbClient,
) : ViewModel() {

    private val _subScreen = MutableStateFlow(HubSubScreen.Hub)
    val subScreen: StateFlow<HubSubScreen> = _subScreen.asStateFlow()

    private val _resetEvent = MutableSharedFlow<Unit>(replay = 0)
    val resetEvent: SharedFlow<Unit> = _resetEvent.asSharedFlow()

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
    private var iptvLoadJob: Job? = null

    private val _sportsEvents = MutableStateFlow<List<SportEvent>>(emptyList())
    val sportsEvents = _sportsEvents.asStateFlow()
    private val _sportsLoading = MutableStateFlow(false)
    val sportsLoading = _sportsLoading.asStateFlow()
    private val _allLiveEvents = MutableStateFlow<List<SportEvent>>(emptyList())
    val allLiveEvents: StateFlow<List<SportEvent>> = _allLiveEvents.asStateFlow()
    private val _allLiveLoading = MutableStateFlow(false)
    val allLiveLoading: StateFlow<Boolean> = _allLiveLoading.asStateFlow()

    private val _sportsLeagues = MutableStateFlow(
        listOf(
            SportLeague("now", "⚡ Sports Now", "NOW", "now/_"),
            SportLeague("ufc", "UFC MMA", "UFC", "mma/ufc"),
            SportLeague("boxing", "Boxing", "BOX", "boxing/_"),
            SportLeague("pfl", "PFL MMA", "PFL", "mma/pfl"),
            SportLeague("ppv", "PPV / Special Events", "PPV", "ppv/_"),
            SportLeague("nfl", "NFL Football", "NFL", "football/nfl"),
            SportLeague("nba", "NBA Basketball", "NBA", "basketball/nba"),
            SportLeague("mlb", "MLB Baseball", "MLB", "baseball/mlb"),
            SportLeague("nhl", "NHL Hockey", "NHL", "hockey/nhl"),
            SportLeague("soccer", "MLS Soccer", "MLS", "soccer/usa.1")
        )
    )
    val sportsLeagues = _sportsLeagues.asStateFlow()

    private val _selectedSportEvent = MutableStateFlow<SportEvent?>(null)
    val selectedSportEvent: StateFlow<SportEvent?> = _selectedSportEvent.asStateFlow()

    private val _matchedChannels = MutableStateFlow<List<MatchedChannel>>(emptyList())
    val matchedChannels: StateFlow<List<MatchedChannel>> = _matchedChannels.asStateFlow()

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

    private var sportsRefreshJob: Job? = null

    private val _vidNutzUiState = MutableStateFlow(VidNutzUiState())
    val vidNutzUiState: StateFlow<VidNutzUiState> = _vidNutzUiState.asStateFlow()

    private val _musicNutzUiState = MutableStateFlow(MusicNutzUiState())
    val musicNutzUiState: StateFlow<MusicNutzUiState> = _musicNutzUiState.asStateFlow()

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

    fun addIptvSource(name: String, url: String, type: String) {
        viewModelScope.launch {
            iptvRepository.addSource(IptvSource(name, url, type))
        }
    }

    fun removeIptvSource(url: String) {
        viewModelScope.launch {
            iptvRepository.removeSource(url)
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

    fun loadIptvChannelsWithName(source: IptvSource) {
        _activeIptvSourceName.value = source.name
        loadIptvChannels(source)
    }

    fun setIptvSearchQuery(query: String) {
        _iptvSearchQuery.value = query
    }

    fun loadSportsScoreboard(league: SportLeague) {
        viewModelScope.launch {
            _sportsLoading.value = true
            var events = emptyList<SportEvent>()

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

            // 3. If still empty, search YouTube for league highlights
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

            _sportsEvents.value = events
            _sportsLoading.value = false
        }
    }

    fun selectSportEvent(event: SportEvent) {
        _selectedSportEvent.value = event
        loadMatchedChannels(event)
    }

    fun loadAllLiveEvents() {
        viewModelScope.launch {
            _allLiveLoading.value = true
            val allEvents = mutableListOf<SportEvent>()
            val allLeagues = _sportsLeagues.value.filter { it.id != "now" }
            for (league in allLeagues) {
                try {
                    val parts = league.slug.split("/")
                    if (parts.size < 2) continue
                    val response = sportsClient.getScoreboard(parts[0], parts[1])
                    val live = response.events?.mapNotNull { e ->
                        val c = e.competitions?.firstOrNull() ?: return@mapNotNull null
                        val h = c.competitors?.firstOrNull { it.homeAway == "home" } ?: return@mapNotNull null
                        val a = c.competitors?.firstOrNull { it.homeAway == "away" } ?: return@mapNotNull null
                        if (!e.status.type.detail.uppercase().contains("IN PROGRESS") && !e.status.type.detail.uppercase().contains("LIVE")) return@mapNotNull null
                        SportEvent(id = e.id, name = e.name, date = e.date, status = e.status.type.detail,
                            homeTeam = SportTeam(id = h.team?.id ?: "", name = h.team?.name ?: "", displayName = h.team?.displayName ?: "", logoUrl = h.team?.logo),
                            awayTeam = SportTeam(id = a.team?.id ?: "", name = a.team?.name ?: "", displayName = a.team?.displayName ?: "", logoUrl = a.team?.logo),
                            homeScore = h.score, awayScore = a.score, leagueAbbreviation = league.abbreviation)
                    } ?: emptyList()
                    allEvents.addAll(live)
                } catch (_: Exception) {}
            }
            // Store in shared bridge for player overlay
            SportsNowStore.liveEvents = allEvents
            _allLiveEvents.value = allEvents
            _allLiveLoading.value = false
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

    fun setMusicNutzMode(mode: MusicNutzMode) {
        val state = _musicNutzUiState.value
        _musicNutzUiState.value = state.copy(mode = mode, searchQuery = "", searchResults = null, albumResults = null)
        if (mode == MusicNutzMode.ALBUMS && state.albums.isEmpty()) {
            loadMusicNutzAlbums(state.selectedCategory)
        } else if (mode == MusicNutzMode.TRACKS && state.tracks.isEmpty()) {
            loadMusicNutzTracks(state.selectedCategory)
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
}
