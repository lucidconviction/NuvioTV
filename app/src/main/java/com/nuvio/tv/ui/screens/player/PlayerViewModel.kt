package com.robbdeeze.nuviotv.ui.screens.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.robbdeeze.nuviotv.core.debrid.DirectDebridResolver
import com.robbdeeze.nuviotv.core.debrid.DirectDebridStreamPreparer
import com.robbdeeze.nuviotv.core.plugin.PluginManager
import com.robbdeeze.nuviotv.core.torrent.TorrentService
import com.robbdeeze.nuviotv.core.torrent.TorrentSettings
import com.robbdeeze.nuviotv.data.local.AudioDelayRouteDataStore
import com.robbdeeze.nuviotv.data.local.PlayerSettingsDataStore
import com.robbdeeze.nuviotv.data.local.DeviceLocalPlayerPreferences
import com.robbdeeze.nuviotv.data.local.StreamLinkCacheDataStore
import com.robbdeeze.nuviotv.data.local.StreamBadgeSettingsDataStore
import com.robbdeeze.nuviotv.data.repository.ParentalGuideRepository
import com.robbdeeze.nuviotv.data.repository.SkipIntroRepository
import com.robbdeeze.nuviotv.data.repository.TraktEpisodeMappingService
import com.robbdeeze.nuviotv.data.repository.TraktScrobbleService
import com.robbdeeze.nuviotv.domain.repository.AddonRepository
import com.robbdeeze.nuviotv.domain.repository.MetaRepository
import com.robbdeeze.nuviotv.domain.repository.StreamRepository
import com.robbdeeze.nuviotv.domain.repository.WatchProgressRepository
import com.robbdeeze.nuviotv.core.tmdb.TmdbService
import com.robbdeeze.nuviotv.core.tmdb.TmdbMetadataService
import com.robbdeeze.nuviotv.data.local.TmdbSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchProgressRepository: WatchProgressRepository,
    private val metaRepository: MetaRepository,
    private val streamRepository: StreamRepository,
    private val addonRepository: AddonRepository,
    private val pluginManager: PluginManager,
    private val subtitleRepository: com.robbdeeze.nuviotv.domain.repository.SubtitleRepository,
    private val parentalGuideRepository: ParentalGuideRepository,
    private val traktScrobbleService: TraktScrobbleService,
    private val traktEpisodeMappingService: TraktEpisodeMappingService,
    private val skipIntroRepository: SkipIntroRepository,
    private val playerSettingsDataStore: PlayerSettingsDataStore,
    private val deviceLocalPlayerPreferences: DeviceLocalPlayerPreferences,
    private val streamLinkCacheDataStore: StreamLinkCacheDataStore,
    private val streamBadgeSettingsDataStore: StreamBadgeSettingsDataStore,
    private val bingeGroupCacheDataStore: com.robbdeeze.nuviotv.data.local.BingeGroupCacheDataStore,
    private val layoutPreferenceDataStore: com.robbdeeze.nuviotv.data.local.LayoutPreferenceDataStore,
    private val watchedItemsPreferences: com.robbdeeze.nuviotv.data.local.WatchedItemsPreferences,
    private val trackPreferenceDataStore: com.robbdeeze.nuviotv.data.local.TrackPreferenceDataStore,
    private val audioDelayRouteDataStore: AudioDelayRouteDataStore,
    private val torrentService: TorrentService,
    private val torrentSettings: TorrentSettings,
    private val tmdbService: TmdbService,
    private val tmdbMetadataService: TmdbMetadataService,
    private val tmdbSettingsDataStore: TmdbSettingsDataStore,
    private val trailerPlayerPool: com.robbdeeze.nuviotv.core.player.TrailerPlayerPool,
    private val directDebridResolver: DirectDebridResolver,
    private val directDebridStreamPreparer: DirectDebridStreamPreparer,
    private val streamBadgePresentation: com.robbdeeze.nuviotv.core.streams.StreamBadgePresentation,
    private val playbackIssueReportRepository: com.robbdeeze.nuviotv.data.repository.PlaybackIssueReportRepository,
    private val externalPlaybackTracker: com.robbdeeze.nuviotv.core.player.ExternalPlaybackTracker,
    private val subtitleFileCache: com.robbdeeze.nuviotv.core.player.SubtitleFileCache,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    init {
        // Release trailer player codec resources so the full-screen player can
        // claim hardware decoders without contention (prevents black screen).
        trailerPlayerPool.yield()
    }

    private val controller = PlayerRuntimeController(
        context = context,
        watchProgressRepository = watchProgressRepository,
        metaRepository = metaRepository,
        streamRepository = streamRepository,
        addonRepository = addonRepository,
        pluginManager = pluginManager,
        subtitleRepository = subtitleRepository,
        parentalGuideRepository = parentalGuideRepository,
        traktScrobbleService = traktScrobbleService,
        traktEpisodeMappingService = traktEpisodeMappingService,
        skipIntroRepository = skipIntroRepository,
        playerSettingsDataStore = playerSettingsDataStore,
        deviceLocalPlayerPreferences = deviceLocalPlayerPreferences,
        streamLinkCacheDataStore = streamLinkCacheDataStore,
        streamBadgeSettingsDataStore = streamBadgeSettingsDataStore,
        bingeGroupCacheDataStore = bingeGroupCacheDataStore,
        layoutPreferenceDataStore = layoutPreferenceDataStore,
        watchedItemsPreferences = watchedItemsPreferences,
        trackPreferenceDataStore = trackPreferenceDataStore,
        audioDelayRouteDataStore = audioDelayRouteDataStore,
        torrentService = torrentService,
        torrentSettings = torrentSettings,
        tmdbService = tmdbService,
        tmdbMetadataService = tmdbMetadataService,
        tmdbSettingsDataStore = tmdbSettingsDataStore,
        directDebridResolver = directDebridResolver,
        directDebridStreamPreparer = directDebridStreamPreparer,
        streamBadgePresentation = streamBadgePresentation,
        playbackIssueReportRepository = playbackIssueReportRepository,
        savedStateHandle = savedStateHandle,
        scope = viewModelScope
    )

    val uiState: StateFlow<PlayerUiState>
        get() = controller.uiState

    val playbackTimeline: StateFlow<PlaybackTimelineState>
        get() = controller.playbackTimeline

    val exoPlayer: ExoPlayer?
        get() = controller.exoPlayer

    fun getCurrentStreamUrl(): String = controller.getCurrentStreamUrl()

    fun getCurrentHeaders(): Map<String, String> = controller.getCurrentHeaders()

    fun stopAndRelease() {
        controller.stopAndRelease()
    }

    fun scheduleHideControls() {
        controller.scheduleHideControls()
    }

    fun onUserInteraction() {
        controller.onUserInteraction()
    }

    fun hideControls() {
        controller.hideControls()
    }

    fun attachHostActivity(activity: android.app.Activity?) {
        controller.attachHostActivity(activity)
    }

    fun attachMpvView(view: NuvioMpvSurfaceView?) {
        controller.attachMpvView(view)
    }

    fun pauseForLifecycle() {
        controller.pauseForLifecycle()
    }

    fun resumeForLifecycle() {
        controller.resumeForLifecycle()
    }

    fun startInitialPlaybackIfNeeded() {
        controller.startInitialPlaybackIfNeeded()
    }

    fun onEvent(event: PlayerEvent) {
        controller.onEvent(event)
    }

    fun consumePendingExitReason() {
        controller.consumePendingExitReason()
    }

    override fun onCleared() {
        controller.onCleared()
        // Allow the trailer player to be re-created when returning to home screen.
        trailerPlayerPool.reclaim()
        super.onCleared()
    }

    /**
     * Save watch progress returned by an external player after "Open in External Player".
     * Uses the controller's current content metadata (contentId, season, episode, etc.)
     * which are still available since the controller hasn't been cleared yet.
     */
    fun saveExternalPlayerProgress(positionMs: Long, durationMs: Long?) {
        val effectiveDuration = durationMs ?: controller.playbackTimeline.value.duration
        controller.saveWatchProgressInternal(
            position = positionMs,
            duration = effectiveDuration
        )
    }

    /**
     * Launch the current stream in an external player via the centralized tracker.
     * The tracker handles progress saving independently of PlayerScreen lifecycle.
     */
    fun launchInExternalPlayer(activityContext: Context, resumePositionMs: Long) {
        val url = controller.getCurrentStreamUrl()
        val metadata = com.robbdeeze.nuviotv.core.player.ExternalPlaybackMetadata(
            contentId = controller.contentId ?: return,
            contentType = controller.contentType ?: "movie",
            contentName = controller.contentName ?: controller.title,
            poster = controller.poster,
            backdrop = controller.backdrop,
            logo = controller.logo,
            videoId = controller.currentVideoId ?: controller.contentId ?: return,
            season = controller.currentSeason,
            episode = controller.currentEpisode,
            episodeTitle = controller.currentEpisodeTitle,
            year = controller.year
        )

        // Pass already-loaded addon subtitles if forward setting is enabled
        val subtitleInputs = if (controller.uiState.value.subtitleStyle.preferredLanguage.trim().lowercase() != "none") {
            val addonSubtitles = controller.uiState.value.addonSubtitles
            if (addonSubtitles.isNotEmpty()) {
                addonSubtitles.map {
                    com.robbdeeze.nuviotv.core.player.SubtitleInput(
                        url = it.url,
                        name = "${it.getDisplayLanguage()} - ${it.addonName}",
                        lang = it.lang
                    )
                }
            } else null
        } else null

        // Cache subtitle files locally and launch player in background
        viewModelScope.launch {
            val cachedSubtitles = subtitleInputs?.let { subtitleFileCache.cacheSubtitles(it) }

            externalPlaybackTracker.launchPlayer(
                metadata = metadata,
                url = url,
                title = metadata.buildPlayerTitle(),
                headers = controller.getCurrentHeaders(),
                resumePositionMs = resumePositionMs,
                subtitles = cachedSubtitles,
                context = activityContext
            )
        }
    }
}
