package com.robbdeeze.nuviotv.ui.screens.player

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import com.robbdeeze.nuviotv.data.sports.DaddyLiveClient
import com.robbdeeze.nuviotv.ui.screens.player.SportsNowStore
import com.robbdeeze.nuviotv.domain.model.IptvChannel
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

        val allChannels = IptvPlayerStore.channels.toList()
        val currentIndex = allChannels.indexOfFirst { it.url == streamUrl }
        val startChannels = if (currentIndex >= 0) allChannels else listOf(channel)
        val startChannel = startChannels.getOrElse(currentIndex.coerceAtLeast(0)) { channel }

        _uiState.value = IptvPlayerUiState(
            currentChannel = startChannel,
            channels = startChannels,
            channelName = startChannel.name,
            logoUrl = startChannel.logoUrl,
        )

        viewModelScope.launch {
            channelHistoryStore.addChannelToHistory(startChannel)
        }
        loadFavorites()
        loadHistory()
        loadAllChannels()

        IptvPlayerStore.clear()
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
        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
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
        val mediaItem = MediaItem.fromUri(channel.url)
        exoPlayerInstance.setMediaItem(mediaItem)
        exoPlayerInstance.prepare()
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
    var channelJumpBuffer by remember { mutableStateOf("") }
    var showChannelJump by remember { mutableStateOf(false) }
    var showLiveGames by remember { mutableStateOf(false) }
    var dlEvents by remember { mutableStateOf<List<com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent>>(emptyList()) }
    LaunchedEffect(Unit) { dlEvents = DaddyLiveClient.fetchEvents() }
    var showMultiSlotPicker by remember { mutableStateOf(false) }
    var overlayTab by remember { mutableStateOf("channels") }
    var toastMessage by remember { mutableStateOf<String?>(null) }
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
        if (uiState.showChannelSwitcher || uiState.showChannelHistory) {
            viewModel.hideControls()
        } else if (showChannelJump) {
            showChannelJump = false
            channelJumpBuffer = ""
        } else if (uiState.showControls && uiState.previousChannel != null) {
            viewModel.switchToLastChannel()
        } else {
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
    LaunchedEffect(uiState.showControls, uiState.showChannelSwitcher, uiState.showChannelHistory) {
        if (uiState.showControls && !uiState.showChannelSwitcher && !uiState.showChannelHistory) {
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
                        Text("Now Playing", color = Color(0xFF888888), fontSize = 12.sp)
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
                Spacer(modifier = Modifier.width(8.dp))
                ControlBtn(Icons.Default.PlayArrow, "Live Games", tint = if (SportsNowStore.liveEvents.isNotEmpty()) Color(0xFF00FF00) else Color.White, onClick = { showLiveGames = !showLiveGames })
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

        // Live Games overlay
        val dlLive = dlEvents.filter { it.isLive }
        AnimatedVisibility(
            visible = showLiveGames,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showLiveGames = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
                        .clickable(enabled = false) {}
                ) {
                    Text("Live Games", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("${dlLive.size} live · ${dlEvents.size - dlLive.size} upcoming", color = Color(0xFF888888), fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    val combined = dlLive.take(20)
                    if (combined.isEmpty() && SportsNowStore.liveEvents.isEmpty()) {
                        Text("No live games right now", color = Color(0xFF888888))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(340.dp)) {
                            items(combined, key = { it.id }) { dlEvent ->
                                var isFocused by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                                        .background(if (isFocused) Color(0xFF2E2E2E) else Color(0xFF111111), RoundedCornerShape(8.dp))
                                        .clickable {
                                            showLiveGames = false
                                            val matchCh = uiState.allChannels.firstOrNull { ch ->
                                                dlEvent.channels.any { c ->
                                                    ch.name.lowercase().contains(c.name.lowercase().trim()) ||
                                                    c.name.lowercase().trim().contains(ch.name.lowercase())
                                                }
                                            }
                                            if (matchCh != null) viewModel.playChannel(matchCh)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dlEvent.eventName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(dlEvent.category.take(16), color = Color(0xFF4A90D9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(8.dp))
                                            Text(dlEvent.localTime, color = Color(0xFF888888), fontSize = 10.sp)
                                        }
                                        val chDisplay = dlEvent.channels.take(3).map { it.name }.joinToString(", ")
                                        Text("📺 $chDisplay", color = Color(0xFFB0B0B0), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("Switch ▸", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            listOf("channels" to "Channels", "fav" to "Favorites", "history" to "History").forEach { (key, label) ->
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
                            itemsIndexed(displayList, key = { _, ch -> ch.id }) { index, channel ->
                                val isCurrent = uiState.currentChannel?.url == channel.url
                                val isFav = channel.id in uiState.favoriteIds
                                var isFocused by remember { mutableStateOf(false) }
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
