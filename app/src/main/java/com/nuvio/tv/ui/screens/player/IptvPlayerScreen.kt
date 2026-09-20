package com.robbdeeze.nuviotv.ui.screens.player

import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowStore
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.data.local.ChannelHistoryStore
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowPushStore
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowSlotPicker

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.IptvEpgEntry
import com.robbdeeze.nuviotv.domain.model.YoutubeQuality
import com.robbdeeze.nuviotv.data.trailer.YoutubeChunkedDataSourceFactory
import com.robbdeeze.nuviotv.ui.components.TrailerPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MergingMediaSource
import com.robbdeeze.nuviotv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IptvPlayerUiState(
    val isPlaying: Boolean = true,
    val isBuffering: Boolean = false,
    val showControls: Boolean = true,
    val showChannelSwitcher: Boolean = false,
    val showChannelHistory: Boolean = false,
    val currentChannel: IptvChannel? = null,
    val channels: List<IptvChannel> = emptyList(),
    val allChannels: List<IptvChannel> = emptyList(),
    val allCategories: List<String> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val history: List<IptvChannel> = emptyList(),
    val previousChannel: IptvChannel? = null,
    val channelName: String = "",
    val logoUrl: String? = null,
    val playbackPositionMs: Long = 0L,
    val playbackDurationMs: Long = 0L,
    val currentEpg: IptvEpgEntry? = null,
    val nextEpg: IptvEpgEntry? = null,
    val epgMap: Map<String, List<IptvEpgEntry>> = emptyMap()
)

@HiltViewModel
class IptvPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val iptvRepository: IptvRepository,
    private val channelHistoryStore: ChannelHistoryStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IptvPlayerUiState())
    val uiState: StateFlow<IptvPlayerUiState> = _uiState.asStateFlow()

    var exoPlayer: ExoPlayer? = null

    init {
        val streamUrl = savedStateHandle.get<String>("streamUrl") ?: ""
        val channelName = java.net.URLDecoder.decode(savedStateHandle.get<String>("channelName") ?: "", "UTF-8")
        val channelId = savedStateHandle.get<String>("channelId") ?: ""
        val logoUrl = java.net.URLDecoder.decode(savedStateHandle.get<String>("logoUrl") ?: "", "UTF-8")

        val channel = IptvChannel(
            id = channelId.ifEmpty { streamUrl },
            name = channelName.ifEmpty { "Channel" },
            url = streamUrl,
            logoUrl = logoUrl.ifEmpty { null },
        )

        val preloadedChannels = IptvPlayerStore.channels.toList()
        val currentIndex = preloadedChannels.indexOfFirst { it.url == streamUrl }
        val startChannels = if (currentIndex >= 0) preloadedChannels else listOf(channel)
        val startChannel = startChannels.getOrElse(currentIndex.coerceAtLeast(0)) { channel }

        _uiState.value = IptvPlayerUiState(
            currentChannel = startChannel,
            channels = startChannels,
            allChannels = preloadedChannels,
            allCategories = preloadedChannels.mapNotNull { it.categoryName }.distinct().sorted(),
            channelName = startChannel.name,
            logoUrl = startChannel.logoUrl,
        )

        viewModelScope.launch {
            channelHistoryStore.addChannelToHistory(startChannel)
        }
        loadFavorites()
        loadHistory()
        loadAllChannels()
        loadEpg()

        IptvPlayerStore.clear()
    }

    private fun loadEpg() {
        viewModelScope.launch {
            val sources = iptvRepository.getSources().first()
            val epgMap = mutableMapOf<String, List<IptvEpgEntry>>()
            for (source in sources) {
                val entries = iptvRepository.getEpg(source)
                epgMap.putAll(entries)
            }
            _uiState.value = _uiState.value.copy(epgMap = epgMap)
            updateChannelEpg()
        }
    }

    private fun updateChannelEpg() {
        val ch = _uiState.value.currentChannel ?: return
        val entries = _uiState.value.epgMap[ch.id] ?: emptyList()
        val now = System.currentTimeMillis()
        val current = entries.firstOrNull { it.startTimeMs <= now && now < it.endTimeMs }
        val next = entries.firstOrNull { it.startTimeMs > now }
        _uiState.value = _uiState.value.copy(currentEpg = current, nextEpg = next)
    }

    private fun loadAllChannels() {
        viewModelScope.launch {
            val sources = iptvRepository.getSources().first()
            val seen = mutableSetOf<String>()
            val allCh = mutableListOf<IptvChannel>()
            for (source in sources) {
                iptvRepository.getChannelsFlow(source).collect { ch ->
                    if (ch.url !in seen) { seen.add(ch.url); allCh.add(ch) }
                }
            }
            _uiState.value = _uiState.value.copy(
                allChannels = allCh.toList(),
                allCategories = allCh.mapNotNull { it.categoryName }.distinct().sorted()
            )
        }
    }

    fun playChannel(channel: IptvChannel, showControlsAfter: Boolean = true) {
        val player = exoPlayer ?: return
        val prev = _uiState.value.currentChannel
        playOnPlayer(player, channel)
        player.play()

        _uiState.value = _uiState.value.copy(
            currentChannel = channel,
            previousChannel = if (prev != null && prev.url != channel.url) prev else _uiState.value.previousChannel,
            channelName = channel.name,
            logoUrl = channel.logoUrl,
            isPlaying = true,
            showControls = showControlsAfter,
            showChannelSwitcher = false,
            showChannelHistory = false,
        )

        viewModelScope.launch {
            channelHistoryStore.addChannelToHistory(channel)
            loadHistory()
            updateChannelEpg()
        }
    }

    private fun playOnPlayer(player: ExoPlayer, channel: IptvChannel) {
        try {
            val videoUrl = channel.url
            val audioUrl = channel.audioUrl
            if (!audioUrl.isNullOrBlank()) {
                val mediaSourceFactory = DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
                val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                player.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                player.setMediaItem(MediaItem.fromUri(videoUrl))
            }
            player.prepare()
        } catch (e: Exception) {
            Log.e("IptvPlayerVM", "playOnPlayer failed: ${e.message}")
            player.setMediaItem(MediaItem.fromUri(channel.url))
            player.prepare()
        }
    }

    fun switchToIndex(index: Int) {
        val chs = _uiState.value.allChannels.ifEmpty { _uiState.value.channels }
        if (index in chs.indices) {
            playChannel(chs[index], showControlsAfter = false)
        }
    }

    fun setBuffering(buffering: Boolean) {
        _uiState.value = _uiState.value.copy(isBuffering = buffering)
    }

    fun switchToLastChannel() {
        val prev = _uiState.value.previousChannel ?: return
        playChannel(prev)
    }

    fun channelUp() {
        val chs = _uiState.value.allChannels.ifEmpty { _uiState.value.channels }
        val idx = chs.indexOf(_uiState.value.currentChannel)
        if (idx >= 0) playChannel(chs[(idx + 1) % chs.size], showControlsAfter = false)
    }

    fun channelDown() {
        val chs = _uiState.value.allChannels.ifEmpty { _uiState.value.channels }
        val idx = chs.indexOf(_uiState.value.currentChannel)
        if (idx >= 0) playChannel(chs[(idx - 1 + chs.size) % chs.size], showControlsAfter = false)
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            player.play()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    fun toggleControls() {
        _uiState.value = _uiState.value.copy(
            showControls = !_uiState.value.showControls,
            showChannelSwitcher = false,
            showChannelHistory = false,
        )
    }

    fun showControls() {
        _uiState.value = _uiState.value.copy(showControls = true)
    }

    fun hideControls() {
        _uiState.value = _uiState.value.copy(
            showControls = false,
            showChannelSwitcher = false,
            showChannelHistory = false,
        )
    }

    fun toggleChannelSwitcher() {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            showChannelSwitcher = !cur.showChannelSwitcher,
            showChannelHistory = false,
            showControls = true,
        )
    }

    fun toggleChannelHistory() {
        val cur = _uiState.value
        _uiState.value = cur.copy(
            showChannelHistory = !cur.showChannelHistory,
            showChannelSwitcher = false,
            showControls = true,
        )
    }

    fun toggleFavorite(channel: IptvChannel) {
        val isFav = channel.id in _uiState.value.favoriteIds
        viewModelScope.launch {
            if (isFav) {
                iptvRepository.removeFavorite(channel.id)
            } else {
                iptvRepository.addFavorite(channel)
            }
            loadFavorites()
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            iptvRepository.getFavorites().collect { favs ->
                _uiState.value = _uiState.value.copy(
                    favoriteIds = favs.map { it.id }.toSet()
                )
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            channelHistoryStore.historyFlow.collect { hist ->
                _uiState.value = _uiState.value.copy(history = hist)
            }
        }
    }

    fun scheduleHideControls() {
        viewModelScope.launch {
            delay(5000)
            _uiState.value = _uiState.value.copy(showControls = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun IptvPlayerScreen(
    onBackPress: () -> Unit,
    viewModel: IptvPlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val containerFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }

    var showEndedOverlay by remember { mutableStateOf(false) }
    var endedCountdown by remember { mutableStateOf(3) }
    val exoPlayerInstance = remember {
        val ep = ExoPlayer.Builder(context).build()
        ep.repeatMode = Player.REPEAT_MODE_OFF
        ep.playWhenReady = true
        ep.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                viewModel.setBuffering(state == Player.STATE_BUFFERING)
                if (state == Player.STATE_ENDED) {
                    showEndedOverlay = true
                }
            }
        })
        ep
    }

    LaunchedEffect(Unit) {
        viewModel.exoPlayer = exoPlayerInstance
        val channel = uiState.currentChannel ?: return@LaunchedEffect
        try {
            val videoUrl = channel.url
            if (!channel.audioUrl.isNullOrBlank()) {
                val mediaSourceFactory = DefaultMediaSourceFactory(YoutubeChunkedDataSourceFactory())
                val videoSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(channel.audioUrl))
                exoPlayerInstance.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                exoPlayerInstance.setMediaItem(MediaItem.fromUri(videoUrl))
            }
            exoPlayerInstance.prepare()
            exoPlayerInstance.playWhenReady = true
        } catch (e: Exception) {
            Log.e("IptvPlayerInit", "Player init failed: ${e.message}")
            exoPlayerInstance.setMediaItem(MediaItem.fromUri(channel.url))
            exoPlayerInstance.prepare()
            exoPlayerInstance.playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayerInstance.release()
            viewModel.exoPlayer = null
        }
    }

    fun doHideControls() {
        viewModel.hideControls()
    }

    // Channel number input state
    val scope = rememberCoroutineScope()
    var channelJumpBuffer by remember { mutableStateOf("") }
    var showChannelJump by remember { mutableStateOf(false) }
    var gameChannelsPopup by remember { mutableStateOf<Pair<String, List<IptvChannel>>?>(null) }
    var showMultiSlotPicker by remember { mutableStateOf(false) }
    var overlayTab by remember { mutableStateOf("channels") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var qcPopup by remember { mutableStateOf<Pair<String, List<IptvChannel>>?>(null) }
    // Channel info bar state
    var showChannelInfo by remember { mutableStateOf(false) }
    var channelInfoName by remember { mutableStateOf("") }
    var channelInfoLogo by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.currentChannel) {
        val ch = uiState.currentChannel ?: return@LaunchedEffect
        channelInfoName = ch.name
        channelInfoLogo = ch.logoUrl
        showChannelInfo = true
        delay(2500)
        showChannelInfo = false
    }
    val endedNextChannelName by remember(uiState.currentChannel, uiState.allChannels) {
        val chs = uiState.allChannels.ifEmpty { uiState.channels }
        val idx = uiState.currentChannel?.let { chs.indexOf(it) } ?: -1
        val next = if (idx >= 0 && chs.size > 1) chs[(idx + 1) % chs.size].name else null
        mutableStateOf(next)
    }

    // Countdown for stream-ended overlay
    LaunchedEffect(showEndedOverlay) {
        if (showEndedOverlay) {
            endedCountdown = 3
            while (endedCountdown > 0) {
                delay(1000)
                endedCountdown--
            }
            showEndedOverlay = false
            // Save position before switching
            val pos = exoPlayerInstance.currentPosition
            if (pos > 0 && IptvPlayerStore.launchedFromSlotIndex >= 0) {
                val streamId = IptvPlayerStore.currentChannel()?.let { "stream_${it.id}_${IptvPlayerStore.launchedFromSlotIndex}" }
                if (streamId != null) MultiWindowStore.saveSeekPosition(streamId, pos)
            }
            viewModel.channelUp()
        }
    }

    // Buffering stall: skip after 10s
    LaunchedEffect(uiState.isBuffering) {
        if (uiState.isBuffering) {
            delay(10000)
            if (uiState.isBuffering) {
                viewModel.channelUp()
            }
        }
    }

    fun addToFirstAvailableSlot() {
        val ch = uiState.currentChannel ?: return
        val usedSlots = MultiWindowStore.streams.map { it.slotIndex }.toSet()
        var slot = -1
        for (i in 0 until MultiWindowStore.MAX_PLAYERS) {
            if (i !in usedSlots) { slot = i; break }
        }
        if (slot >= 0) {
            MultiWindowStore.addToSlot(ch, slot)
            val count = MultiWindowStore.streams.size
            toastMessage = "Added ($count of ${MultiWindowStore.MAX_PLAYERS})"
        } else {
            toastMessage = "MultiWindow is full"
        }
    }
    BackHandler {
        if (gameChannelsPopup != null) {
            gameChannelsPopup = null
        } else if (uiState.showChannelSwitcher || uiState.showChannelHistory) {
            viewModel.hideControls()
        } else if (showChannelJump) {
            showChannelJump = false
            channelJumpBuffer = ""
        } else if (uiState.showControls && uiState.previousChannel != null) {
            viewModel.switchToLastChannel()
        } else {
            // Save seek position before returning to MultiNutz
            val pos = exoPlayerInstance.currentPosition
            if (pos > 0 && IptvPlayerStore.launchedFromSlotIndex >= 0) {
                val ch = uiState.currentChannel
                if (ch != null) {
                    val streamId = "stream_${ch.id}_${IptvPlayerStore.launchedFromSlotIndex}"
                    MultiWindowStore.saveSeekPosition(streamId, pos)
                }
            }
            onBackPress()
        }
    }

    // Focus first play button when controls appear
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(100)
            playFocusRequester.requestFocus()
        }
    }

    // Auto-hide: cancel previous coroutine
    LaunchedEffect(uiState.showControls, uiState.showChannelSwitcher, uiState.showChannelHistory, gameChannelsPopup) {
        if (uiState.showControls && !uiState.showChannelSwitcher && !uiState.showChannelHistory && gameChannelsPopup == null) {
            delay(5000)
            viewModel.hideControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                // Cancel ended overlay on any key
                if (showEndedOverlay) { showEndedOverlay = false; return@onKeyEvent true }
                when (keyEvent.nativeKeyEvent.keyCode) {
                    // When overlay is showing, let inner composables handle all navigation
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (uiState.showChannelSwitcher) false
                        else if (!uiState.showControls) { viewModel.showControls(); true }
                        else if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) { viewModel.togglePlayPause(); true }
                        else false
                    }
                    // DPAD UP/DOWN when no controls → change channels; when overlay → pass through
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (uiState.showChannelSwitcher) false
                        else if (!uiState.showControls) {
                            if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) viewModel.channelUp()
                            else viewModel.channelDown()
                            true
                        } else false
                    }
                    // Dedicated channel keys
                    KeyEvent.KEYCODE_CHANNEL_UP -> { viewModel.channelUp(); true }
                    KeyEvent.KEYCODE_CHANNEL_DOWN -> { viewModel.channelDown(); true }
                    // Number keys → channel jump
                    KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
                    KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
                    KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
                    KeyEvent.KEYCODE_9 -> {
                        val digit = keyEvent.nativeKeyEvent.keyCode - KeyEvent.KEYCODE_0
                        channelJumpBuffer += digit
                        showChannelJump = true
                        // Try to jump after 3 digits or timeout
                        val idx = channelJumpBuffer.toIntOrNull()
                        if (idx != null && idx > 0 && idx <= uiState.channels.size) {
                            viewModel.switchToIndex(idx - 1)
                            channelJumpBuffer = ""
                            showChannelJump = false
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayerInstance
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Channel info bar (brief overlay when switching channels)
        AnimatedVisibility(visible = showChannelInfo, enter = fadeIn(tween(200)), exit = fadeOut(tween(500)), modifier = Modifier.align(Alignment.TopCenter)) {
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))
                ).padding(start = 16.dp, top = 5.dp, end = 16.dp, bottom = 32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (channelInfoLogo != null) {
                        AsyncImage(model = channelInfoLogo, contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(channelInfoName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val currentEpg = uiState.currentEpg
                        val nextEpg = uiState.nextEpg
                        if (currentEpg != null) {
                            Text(currentEpg.title, color = Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (nextEpg != null) {
                                Text("Next: ${nextEpg.title}", color = Color(0xFF888888), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        } else {
                            Text("Now Playing", color = Color(0xFF888888), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Loading spinner
        if (uiState.isBuffering) {
            Box(Modifier.fillMaxSize().align(Alignment.Center), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4A90D9), strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
            }
        }

        // Stream ended overlay
        if (showEndedOverlay) {
            Box(Modifier.fillMaxSize().background(Color(0x99000000)).align(Alignment.Center), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stream Ended", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Next: $endedNextChannelName", color = Color(0xFF4A90D9), fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("$endedCountdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Press any key to cancel", color = Color(0xFF888888), fontSize = 12.sp)
                }
            }
        }

        // Channel info overlay (top)
        if (uiState.showControls) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(start = 16.dp, top = 5.dp, end = 16.dp, bottom = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onBackPress,
                        modifier = Modifier
                            .onFocusChanged { backFocused = it.isFocused }
                            .border(
                                width = if (backFocused) 2.dp else 0.dp,
                                color = if (backFocused) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = uiState.channelName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val currentEpg = uiState.currentEpg
                        val nextEpg = uiState.nextEpg
                        if (currentEpg != null) {
                            Text(
                                text = currentEpg.title,
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nextEpg != null) {
                                Text(
                                    text = "Next: ${nextEpg.title}",
                                    color = Color(0xFF888888),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.currentChannel != null) {
                                val idx = uiState.channels.indexOf(uiState.currentChannel)
                                if (idx >= 0 && uiState.channels.size > 1) {
                                    Text(
                                        text = "${idx + 1} / ${uiState.channels.size}",
                                        color = Color.LightGray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Control buttons (bottom)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlBtn(Icons.Default.SkipPrevious, "Previous Channel", onClick = { viewModel.channelDown() })
                Spacer(modifier = Modifier.width(16.dp))
                ControlBtn(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (uiState.isPlaying) "Pause" else "Play",
                    onClick = { viewModel.togglePlayPause() },
                    focusRequester = playFocusRequester
                )
                Spacer(modifier = Modifier.width(16.dp))
                ControlBtn(Icons.Default.SkipNext, "Next Channel", onClick = { viewModel.channelUp() })
                Spacer(modifier = Modifier.width(24.dp))
                ControlBtn(Icons.AutoMirrored.Filled.List, "Channel List", onClick = { overlayTab = "channels"; viewModel.toggleChannelSwitcher() })
                Spacer(modifier = Modifier.width(8.dp))
                val isFav = uiState.currentChannel?.let { it.id in uiState.favoriteIds } ?: false
                ControlBtn(
                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (isFav) "Unfavorite" else "Favorite",
                    tint = if (isFav) Color.Red else Color.White,
                    onClick = {
                        uiState.currentChannel?.let { viewModel.toggleFavorite(it) }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ControlBtn(Icons.Default.History, "History", onClick = { overlayTab = "history"; viewModel.toggleChannelSwitcher() })
                Spacer(modifier = Modifier.width(8.dp))
                ControlBtn(Icons.Default.Add, "Add to MultiNutz", tint = Color(0xFF4A90D9), onClick = { addToFirstAvailableSlot() })
            }
        }

        // Channel jump overlay
        if (showChannelJump) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Go to channel", color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = channelJumpBuffer.ifEmpty { "?" },
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (channelJumpBuffer.isNotEmpty()) {
                        val idx = channelJumpBuffer.toIntOrNull()
                        if (idx != null && idx > 0 && idx <= uiState.channels.size) {
                            Text(
                                text = uiState.channels[idx - 1].name,
                                color = Color(0xFFB0B0B0),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Game channel options popup
        gameChannelsPopup?.let { (eventName, matches) ->
            BackHandler(enabled = gameChannelsPopup != null) { gameChannelsPopup = null }
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)).focusable()
                    .clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { gameChannelsPopup = null }),
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White), modifier = Modifier.width(460.dp).heightIn(max = 520.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📺 $eventName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            var closeFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { gameChannelsPopup = null }, modifier = Modifier.size(28.dp).onFocusChanged { closeFocused = it.isFocused }) {
                                Icon(Icons.Default.Clear, null, tint = if (closeFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Choose a channel to watch", color = Color(0xFF888888), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        val gcListFocus = remember { FocusRequester() }
                        LaunchedEffect(gameChannelsPopup) { delay(150); runCatching { gcListFocus.requestFocus() } }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().focusRequester(gcListFocus)) {
                            items(matches, key = { it.url }) { match ->
                                var rowFocused by remember { mutableStateOf(false) }
                                Card(
                                    onClick = {
                                        gameChannelsPopup = null
                                        viewModel.playChannel(match)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (rowFocused) Color(0xFF2E2E2E) else Color(0xFF111111)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(if (rowFocused) 2.dp else 0.dp, if (rowFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { rowFocused = it.isFocused }
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(match.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (match.categoryName != null) {
                                                Text(match.categoryName!!, color = Color(0xFF888888), fontSize = 11.sp)
                                            }
                                        }
                                        Text("Play ▸", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Channel / Favorites / History overlay (right side)
        if (uiState.showChannelSwitcher) {
            var searchQuery by remember { mutableStateOf("") }
            var selectedCategory by remember { mutableStateOf<String?>(null) }
            val firstItemFocusRequester = remember { FocusRequester() }
            val searchFocusRequester = remember { FocusRequester() }
            LaunchedEffect(uiState.showChannelSwitcher) { delay(200); firstItemFocusRequester.requestFocus() }
            BackHandler { viewModel.hideControls() }
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color(0x66000000)).pointerInput(Unit) { detectTapGestures { viewModel.hideControls() } })
                Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(360.dp).background(Color(0xFF0D1117).copy(alpha = 0.3f)).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
                    Column(Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                            listOf("channels" to "Channels", "fav" to "Favorites", "history" to "History", "quick" to "Quick").forEach { (key, label) ->
                                var tabFocused by remember { mutableStateOf(false) }
                                Surface(onClick = { overlayTab = key; searchQuery = ""; selectedCategory = null }, shape = RoundedCornerShape(8.dp),
                                    color = if (overlayTab == key) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                    border = BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.weight(1f).height(36.dp).onFocusChanged { tabFocused = it.isFocused }
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label, color = if (overlayTab == key) Color.White else Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                            }
                        }
                        if (overlayTab == "channels") {
                            var searchFocused by remember { mutableStateOf(false) }
                            LaunchedEffect(searchFocused) { if (searchFocused) searchFocusRequester.requestFocus() }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                                    .focusRequester(searchFocusRequester)
                                    .border(if (searchFocused) 1.5.dp else 0.dp, if (searchFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                    .onFocusChanged { searchFocused = it.isFocused }.padding(horizontal = 12.dp).focusable(),
                                decorationBox = { innerTextField: @Composable () -> Unit ->
                                    Box {
                                        if (searchQuery.isEmpty()) Text("Search channels...", color = Color(0xFF666666), fontSize = 14.sp)
                                        innerTextField()
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { })
                            )
                            Spacer(Modifier.height(8.dp))
                            // Category filter row
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                item {
                                    var allFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { selectedCategory = null }, shape = RoundedCornerShape(16.dp),
                                        color = if (selectedCategory == null) Color(0xFF4A90D9) else if (allFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (allFocused) 2.dp else 0.dp, if (allFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(28.dp).onFocusChanged { allFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text("All", color = if (selectedCategory == null) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                                }
                                items(uiState.allCategories) { cat ->
                                    var catFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { selectedCategory = cat }, shape = RoundedCornerShape(16.dp),
                                        color = if (selectedCategory == cat) Color(0xFF4A90D9) else if (catFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (catFocused) 2.dp else 0.dp, if (catFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(28.dp).onFocusChanged { catFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(cat, color = if (selectedCategory == cat) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
                                }
                            }
                        }
                        if (overlayTab == "quick") {
                            var qcRegion by remember { mutableStateOf("All") }
                            val qcTabs = listOf("All", "US", "UK", "CA", "Premium", "Bay Area", "PPV Events", "Sports", "News")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                items(qcTabs) { tab ->
                                    var tabFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { qcRegion = tab }, shape = RoundedCornerShape(16.dp),
                                        color = if (qcRegion == tab) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(28.dp).onFocusChanged { tabFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) { Text(tab, color = if (qcRegion == tab) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                                }
                            }
                            val filtered = com.robbdeeze.nuviotv.data.iptv.QuickChannelList.all.filter { qc ->
                                when (qcRegion) {
                                    "All" -> true
                                    "US" -> "US" in qc.regions
                                    "UK" -> "UK" in qc.regions
                                    "CA" -> "CA" in qc.regions
                                    "Premium" -> "premium" in qc.tags
                                    "Bay Area" -> "bay-area" in qc.regions
                                    "PPV Events" -> "ppv" in qc.tags || qc.displayName.contains("ppv", true) || qc.displayName.contains("pay per view", true) || qc.aliases.any { it.contains("ppv", true) || it.contains("box office", true) }
                                    "Sports" -> "sports" in qc.tags
                                    "News" -> "news" in qc.tags
                                    else -> true
                                }
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize().focusable()) {
                                items(filtered, key = { it.displayName }) { quickCh ->
                                    var rowFocused by remember { mutableStateOf(false) }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                            .onFocusChanged { rowFocused = it.isFocused }
                                            .background(if (rowFocused) Color(0xFF252525) else Color.Transparent, RoundedCornerShape(8.dp))
                                            .border(if (rowFocused) 1.5.dp else 0.dp, if (rowFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val matches = uiState.allChannels.filter { ch ->
                                                    com.robbdeeze.nuviotv.data.iptv.QuickChannelList.matches(quickCh, ch)
                                                }
                                                if (matches.isNotEmpty()) {
                                                    qcPopup = quickCh.displayName to matches
                                                } else {
                                                    toastMessage = "${quickCh.displayName} not found"
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp)
                                    ) {
                                        Text(quickCh.displayName, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        val fullList = when (overlayTab) {
                            "fav" -> uiState.allChannels.filter { it.id in uiState.favoriteIds }
                            "history" -> uiState.history
                            else -> uiState.allChannels
                        }
                        val filteredByCat = if (overlayTab == "channels" && selectedCategory != null) fullList.filter { it.categoryName == selectedCategory } else fullList
                        val displayList = if (searchQuery.isNotBlank()) filteredByCat.filter { it.name.contains(searchQuery, ignoreCase = true) } else filteredByCat
                        val channelListState = remember { LazyListState() }
                        LaunchedEffect(uiState.showChannelSwitcher) {
                            if (uiState.showChannelSwitcher) {
                                delay(300)
                                val idx = displayList.indexOfFirst { it.url == uiState.currentChannel?.url }
                                if (idx >= 0) channelListState.animateScrollToItem(idx)
                            }
                        }
                        LazyColumn(state = channelListState, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize().focusable()) {
                            itemsIndexed(displayList, key = { _, ch -> ch.url }) { index, channel ->
                                val isCurrent = uiState.currentChannel?.url == channel.url
                                val isFav = channel.id in uiState.favoriteIds
                                var isFocused by remember { mutableStateOf(false) }
                                var isPreviewPlaying by remember { mutableStateOf(false) }
                                var previewEnded by remember { mutableStateOf(false) }
                                var previewJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                                val scope = rememberCoroutineScope()

                                LaunchedEffect(isFocused) {
                                    if (isFocused && channel.url.isNotBlank() && !previewEnded) {
                                        previewJob?.cancel()
                                        previewJob = scope.launch {
                                            isPreviewPlaying = true
                                            delay(3_000L)
                                            isPreviewPlaying = false
                                            previewEnded = true
                                        }
                                    } else if (!isFocused) {
                                        previewJob?.cancel()
                                        isPreviewPlaying = false
                                    }
                                }

                                DisposableEffect(isFocused) {
                                    onDispose {
                                        previewJob?.cancel()
                                        isPreviewPlaying = false
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .background(if (isCurrent) Color(0xFF2E2E2E) else if (isFocused) Color(0xFF252525) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .border(if (isFocused) 1.5.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.playChannel(channel) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    // Thumbnail with 3s stream preview confined inside
                                    Box(modifier = Modifier.width(48.dp).height(32.dp)) {
                                        if (isPreviewPlaying && channel.url.isNotBlank()) {
                                            TrailerPlayer(
                                                trailerUrl = channel.url,
                                                isPlaying = true,
                                                onEnded = { isPreviewPlaying = false; previewEnded = true },
                                                muted = true,
                                                cropToFill = true,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                        if (channel.logoUrl != null) {
                                            AsyncImage(
                                                model = channel.logoUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(channel.name, color = if (isCurrent) Color(0xFF00FF00) else Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    var favBtnFocused by remember { mutableStateOf(false) }
                                    IconButton(onClick = { viewModel.toggleFavorite(channel) }, modifier = Modifier.size(28.dp).onFocusChanged { favBtnFocused = it.isFocused }) {
                                        Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) Color.Red else if (favBtnFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick channel popup
        if (qcPopup != null) {
            val (qcName, qcMatches) = qcPopup!!
            BackHandler(enabled = qcPopup != null) { qcPopup = null }
            Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { qcPopup = null }), contentAlignment = Alignment.Center) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White), modifier = Modifier.width(400.dp).heightIn(max = 500.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(qcName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            var closeFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { qcPopup = null }, modifier = Modifier.size(28.dp).onFocusChanged { closeFocused = it.isFocused }) {
                                Icon(Icons.Default.Clear, null, tint = if (closeFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${qcMatches.size} match${if (qcMatches.size != 1) "es" else ""} found", color = Color(0xFF888888), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        val qcListFocus = remember { FocusRequester() }
                        LaunchedEffect(qcPopup) { delay(100); runCatching { qcListFocus.requestFocus() } }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().focusRequester(qcListFocus)) {
                            items(qcMatches, key = { it.url }) { match ->
                                var rowFocused by remember { mutableStateOf(false) }
                                Card(
                                    onClick = {
                                        qcPopup = null
                                        viewModel.playChannel(match)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (rowFocused) Color(0xFF2E2E2E) else Color(0xFF111111)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(if (rowFocused) 2.dp else 0.dp, if (rowFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { rowFocused = it.isFocused }
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(match.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (match.categoryName != null) {
                                                Text(match.categoryName!!, color = Color(0xFF888888), fontSize = 11.sp)
                                            }
                                        }
                                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF4A90D9), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Toast
        if (toastMessage != null) {
            LaunchedEffect(toastMessage) { delay(2000); toastMessage = null }
            Box(Modifier.fillMaxSize().background(Color(0x44000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                Box(Modifier.background(Color(0xCC000000), RoundedCornerShape(16.dp)).padding(horizontal = 32.dp, vertical = 20.dp)) {
                    Text(toastMessage!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
    }

}

@Composable
private fun ControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(48.dp)
            .background(
                if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                CircleShape
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
    }
}
