package com.nuvio.tv.ui.screens.player

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.nuvio.tv.data.local.ChannelHistoryStore
import com.nuvio.tv.ui.screens.player.SportsNowStore
import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IptvPlayerUiState(
    val isPlaying: Boolean = true,
    val showControls: Boolean = true,
    val showChannelSwitcher: Boolean = false,
    val showChannelHistory: Boolean = false,
    val currentChannel: IptvChannel? = null,
    val channels: List<IptvChannel> = emptyList(),
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

        IptvPlayerStore.clear()
    }

    fun playChannel(channel: IptvChannel) {
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
            showControls = true,
            showChannelSwitcher = false,
            showChannelHistory = false,
        )

        viewModelScope.launch {
            channelHistoryStore.addChannelToHistory(channel)
            loadHistory()
        }
    }

    fun switchToIndex(index: Int) {
        val channels = _uiState.value.channels
        if (index in channels.indices) {
            playChannel(channels[index])
        }
    }

    fun switchToLastChannel() {
        val prev = _uiState.value.previousChannel ?: return
        playChannel(prev)
    }

    fun channelUp() {
        val idx = _uiState.value.channels.indexOf(_uiState.value.currentChannel)
        if (idx >= 0) switchToIndex((idx + 1) % _uiState.value.channels.size)
    }

    fun channelDown() {
        val idx = _uiState.value.channels.indexOf(_uiState.value.currentChannel)
        if (idx >= 0) switchToIndex((idx - 1 + _uiState.value.channels.size) % _uiState.value.channels.size)
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

    val exoPlayerInstance = remember {
        val ep = ExoPlayer.Builder(context)
            .build()
        ep.repeatMode = Player.REPEAT_MODE_OFF
        ep.playWhenReady = true
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
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (uiState.showControls && !uiState.showChannelSwitcher && !uiState.showChannelHistory) {
                            viewModel.togglePlayPause()
                            true
                        } else false
                    }
                    // DPAD UP/DOWN when overlay is hidden → show controls
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!uiState.showControls && !uiState.showChannelSwitcher) {
                            viewModel.showControls()
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
                    .padding(start = 16.dp, top = 48.dp, end = 16.dp, bottom = 24.dp)
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
                    onClick = { viewModel.togglePlayPause() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                ControlBtn(Icons.Default.SkipNext, "Next Channel", onClick = { viewModel.channelUp() })
                Spacer(modifier = Modifier.width(24.dp))
                ControlBtn(Icons.AutoMirrored.Filled.List, "Channel List", onClick = { viewModel.toggleChannelSwitcher() })
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
                ControlBtn(Icons.Default.History, "History", onClick = { viewModel.toggleChannelHistory() })
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
                    Spacer(Modifier.height(12.dp))
                    if (SportsNowStore.liveEvents.isEmpty()) {
                        Text("No live games right now", color = Color(0xFF888888))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(320.dp)) {
                            items(SportsNowStore.liveEvents) { event ->
                                var isFocused by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                                        .background(if (isFocused) Color(0xFF2E2E2E) else Color(0xFF111111), RoundedCornerShape(8.dp))
                                        .clickable { showLiveGames = false; onBackPress() }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row {
                                            Text(event.leagueAbbreviation, color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(8.dp))
                                            Text(event.status, color = Color(0xFF00FF00), fontSize = 11.sp)
                                        }
                                    }
                                    Text("${event.awayScore ?: "-"} - ${event.homeScore ?: "-"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
                                    Text("Switch", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Channel Switcher / History overlay
        AnimatedVisibility(
            visible = uiState.showChannelSwitcher || uiState.showChannelHistory,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ChannelSwitcherPanel(
                channels = uiState.channels,
                currentChannel = uiState.currentChannel,
                favoriteIds = uiState.favoriteIds,
                history = uiState.history,
                showHistory = uiState.showChannelHistory,
                onSwitchChannel = { channel -> viewModel.playChannel(channel) },
                onToggleFavorite = { channel -> viewModel.toggleFavorite(channel) },
                onClose = {
                    viewModel.hideControls()
                }
            )
        }
    }

}

@Composable
private fun ControlBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
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
