package com.robbdeeze.nuviotv.ui.screens.multi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.ui.screens.player.IptvPlayerStore
import kotlinx.coroutines.launch

@Composable
fun MultiWindowGrid(
    onBrowseChannels: () -> Unit,
    onOpenSlotPicker: (IptvChannel?) -> Unit,
    onOpenCellOptions: (String) -> Unit,
    onPlayChannel: (IptvChannel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val store = MultiWindowStore
    val streams = store.streams.toList()
    val count = streams.size
    val layout = resolveLayout(count)
    val isFullScreen = store.fullScreenStreamId != null
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bookmarkStore = remember { MultiWindowBookmarkStore(context) }
    var showControls by remember { mutableStateOf(true) }

    // Auto-hide controls after 5s
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(5000)
            showControls = false
        }
    }

    Column(modifier = modifier.fillMaxSize().onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && !showControls) {
            showControls = true; true
        } else false
    }) {
        // Top header bar: Live/Pause + Layout Pills
        AnimatedVisibility(visible = showControls) {
        if (count > 0) {
            val liveCount = streams.count { it.isPlaying }
            val pausedCount = count - liveCount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var liveFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { showControls = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (liveCount > 0) Color(0xFF1A3A1A) else Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (liveFocused) 2.dp else 1.dp,
                        color = if (liveFocused) Color.White else if (liveCount > 0) Color(0xFF00FF00).copy(alpha = 0.5f) else Color(0xFF333333)
                    ),
                    modifier = Modifier
                        .onFocusChanged { liveFocused = it.isFocused }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(if (liveCount > 0) Color(0xFF00FF00) else Color(0xFF444444), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Live $liveCount", color = if (liveCount > 0) Color.White else Color(0xFF666666), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = { showControls = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (pausedCount > 0) Color(0xFF2A2A1A) else Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (pausedCount > 0) Color(0xFFFFAA00).copy(alpha = 0.3f) else Color(0xFF333333))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("⏸", fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("Paused $pausedCount", color = if (pausedCount > 0) Color(0xFFCCCCCC) else Color(0xFF666666), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Layout Pills inline
                if (count > 0) {
                    LayoutPillsRow(
                        count = count,
                        currentLayout = if (store.layoutLocked) store.currentLayout else null,
                        onSelectLayout = {
                            showControls = true
                            store.setLayout(it)
                        },
                        bookmarks = emptyList(),
                        onOpenBookmarks = { },
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                }
            }
        }

        }
        Spacer(modifier = Modifier.height(12.dp))

        // Grid — single rendering path to prevent VideoCell recreation on fullscreen toggle
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(enabled = !showControls) { showControls = true }
        ) {
            if (count == 0 || layout == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Multi-Window",
                            color = Color(0xFF666666),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add IPTV channels to watch multiple streams",
                            color = Color(0xFF444444),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val displaySlots = if (isFullScreen && store.fullScreenStreamId != null) {
                    val idx = streams.indexOfFirst { it.id == store.fullScreenStreamId }
                    if (idx >= 0) listOf(SlotPos(row = 0, col = 0, rowSpan = 1, colSpan = 1, index = idx)) else emptyList()
                } else {
                    calculateSlots(count, layout)
                }
                val totalRows = displaySlots.maxOfOrNull { it.row + it.rowSpan } ?: 1
                val totalCols = displaySlots.maxOfOrNull { it.col + it.colSpan } ?: 1

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until totalRows) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (col in 0 until totalCols) {
                                val slot = displaySlots.find { it.row == row && it.col == col }
                                if (slot != null && slot.index < streams.size) {
                                    val stream = streams[slot.index]
                                    val cellModifier = if (isFullScreen) Modifier.weight(1f) else Modifier.weight(slot.colSpan.toFloat())
                                    key(stream.id) {
                                        VideoCell(
                                            stream = stream,
                                            colSpan = slot.colSpan,
                                            onOpenCellOptions = onOpenCellOptions,
                            onPlayChannel = onPlayChannel,
                            modifier = cellModifier
                                        )
                                    }
                                } else if (slot != null) {
                                    Box(modifier = Modifier.weight(slot.colSpan.toFloat()))
                                } else {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom controls (auto-hide after 5s)
        AnimatedVisibility(visible = showControls) {
            Column {
                // Global Action Bar
                if (count > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlobalActionBar(streams = streams, store = store, onOpenSlotPicker = onOpenSlotPicker, onAnyAction = { showControls = true })
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Add Channel Button
                if (count < MultiWindowStore.MAX_PLAYERS && !isFullScreen) {
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (isFocused) 1.04f else 1f,
                        animationSpec = tween(150),
                        label = "addBtnScale"
                    )
                    OutlinedButton(
                        onClick = { showControls = true; onBrowseChannels() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isFocused) 2.dp else 1.dp,
                            color = if (isFocused) Color.White else Color(0xFF4A90D9)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFocused) Color(0xFF1A2A3A) else Color(0xFF0D1B2A)
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF4A90D9))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Browse Channels (${count}/${MultiWindowStore.MAX_PLAYERS})", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalActionBar(
    streams: List<WindowStream>,
    store: MultiWindowStore,
    onOpenSlotPicker: (IptvChannel?) -> Unit,
    onAnyAction: () -> Unit = {},
) {
    val anyPlaying = streams.any { it.isPlaying }
    val allMuted = store.volumes.values.all { it == 0f } || store.volumes.isEmpty()
    val bmScope = rememberCoroutineScope()
    val bmContext = androidx.compose.ui.platform.LocalContext.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        GlobalActionButton(
            label = if (allMuted) "Unmute All" else "Mute All",
            icon = Icons.Default.VolumeUp,
            color = if (allMuted) Color(0xFFFF6666) else Color(0xFF00FF00),
            isActive = !allMuted,
            onClick = { onAnyAction(); if (allMuted) store.streams.forEach { s -> store.setVolume(s.id, 1f) } else store.muteAll() }
        )
        GlobalActionButton(
            label = "Refresh All",
            icon = Icons.Default.Refresh,
            color = Color(0xFF4A90D9),
            isActive = false,
            onClick = { onAnyAction(); store.streams.toList().forEach { s -> store.refreshStream(s.id) } }
        )
        GlobalActionButton(
            label = if (anyPlaying) "Pause All" else "Play All",
            icon = if (anyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            color = Color(0xFFFFAA00),
            isActive = !anyPlaying,
            onClick = { onAnyAction(); if (anyPlaying) store.pauseAll() else store.playAll() }
        )
        GlobalActionButton(
            label = "Close All",
            icon = Icons.Default.Close,
            color = Color.Red,
            isActive = false,
            onClick = { onAnyAction(); store.closeAll() }
        )
    }
}

@Composable
private fun RowScope.GlobalActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val bgColor = when {
        isFocused && isActive -> color.copy(alpha = 0.3f)
        isFocused -> color.copy(alpha = 0.2f)
        isActive -> color.copy(alpha = 0.15f)
        else -> Color(0xFF1A1A1A)
    }
    val borderColor = when {
        isFocused -> Color.White
        isActive -> color
        else -> color.copy(alpha = 0.4f)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused || isActive) 2.dp else 1.dp,
            color = borderColor
        ),
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = if (isFocused) Color.White else if (isActive) color else color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Text(label, color = if (isFocused) Color.White else if (isActive) color else color.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun LayoutPillsRow(
    count: Int,
    currentLayout: MultiWindowLayout?,
    onSelectLayout: (MultiWindowLayout?) -> Unit,
    bookmarks: List<MultiWindowLayout>,
    onOpenBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validLayouts = getValidLayouts(count)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item {
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.08f else 1f,
                animationSpec = tween(150),
                label = "autoPill"
            )
            val isActive = currentLayout == null && count > 0
            Surface(
                onClick = { onSelectLayout(null) },
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) Color(0xFF4A90D9) else Color(0xFF1A1A1A),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else Color(0xFF333333)
                ),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                Text(
                    "Auto",
                    color = if (isActive) Color.White else Color(0xFF888888),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(validLayouts) { layout ->
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isFocused) 1.08f else 1f,
                animationSpec = tween(150),
                label = "layoutPill"
            )
            val isActive = layout == currentLayout
            val isEnabled = count >= layout.requiredStreams()
            Surface(
                onClick = { if (isEnabled) onSelectLayout(layout) },
                shape = RoundedCornerShape(20.dp),
                color = if (isActive) Color(0xFF4A90D9) else Color(0xFF1A1A1A),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color.White else if (!isEnabled) Color(0xFF333333) else Color(0xFF333333)
                ),
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = if (isEnabled) 1f else 0.4f
                    }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        layout.label,
                        color = if (isActive) Color.White else Color(0xFF888888),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 16.dp, end = if (isEnabled) 16.dp else 4.dp, top = 8.dp, bottom = 8.dp)
                    )
                    if (!isEnabled) {
                        Text("🔒", fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                    }
                }
            }
        }
        // Bookmarks button
        if (bookmarks.isNotEmpty()) {
            item {
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isFocused) 1.08f else 1f,
                    animationSpec = tween(150),
                    label = "bookmarkPill"
                )
                Surface(
                    onClick = onOpenBookmarks,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) Color.White else Color(0xFF333333)
                    ),
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, null, tint = Color(0xFF888888), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Bookmarks", color = Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// Helper to determine how many streams a layout needs
private fun MultiWindowLayout.requiredStreams(): Int = when (this) {
    MultiWindowLayout.V2_STACK, MultiWindowLayout.TWO_X_1, MultiWindowLayout.ONE_X_2 -> 2
    MultiWindowLayout.ONE_PLUS_2, MultiWindowLayout.TWO_PLUS_1, MultiWindowLayout.THREE_VERT -> 3
    MultiWindowLayout.TWO_X_2, MultiWindowLayout.ONE_DASH_2_DASH_1 -> 4
    MultiWindowLayout.ONE_PLUS_4, MultiWindowLayout.FOUR_PLUS_1, MultiWindowLayout.THREE_PLUS_2 -> 5
    MultiWindowLayout.THREE_X_2, MultiWindowLayout.ONE_DASH_2_DASH_2_DASH_1, MultiWindowLayout.TWO_X_3 -> 6
    MultiWindowLayout.ONE_DASH_3_DASH_3, MultiWindowLayout.ONE_PLUS_6 -> 7
    MultiWindowLayout.FOUR_X_2, MultiWindowLayout.ONE_DASH_3_DASH_3_DASH_1, MultiWindowLayout.TWO_X_4 -> 8
    MultiWindowLayout.THREE_X_3 -> 9
}

@Composable
private fun VideoCell(
    stream: WindowStream,
    colSpan: Int,
    onOpenCellOptions: (String) -> Unit,
    onPlayChannel: (IptvChannel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val store = MultiWindowStore
    val context = LocalContext.current
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = tween(150),
        label = "cellScale"
    )
    val isAudioFocus = store.audioFocusId == stream.id
    val resizeMode = store.resizeModes[stream.id] ?: RESIZE_FIT

    // Stream health
    val healthFlow = remember(stream.id) { MultiWindowPlayerManager.getHealthFlow(stream.id) }
    val health by healthFlow.collectAsState()


    // Initialize or get player handle (reactive to refreshStream updates)
    val currentHandleId = store.playerHandleIds[stream.id]
    val playerHandle = remember(stream.id, currentHandleId, stream.channel.url) {
        MultiWindowPlayerManager.init(context)
        val existing = currentHandleId?.let { MultiWindowPlayerManager.getPlayer(it) }
        if (existing != null) {
            PlayerHandle(currentHandleId!!)
        } else {
            val handle = MultiWindowPlayerManager.createPlayer(stream.id, stream.channel.url)
            store.playerHandleIds[stream.id] = handle.id
            handle
        }
    }

    // Lifecycle
    DisposableEffect(stream.id) {
        val handleId = store.playerHandleIds[stream.id]
        if (handleId != null) {
            if (isAudioFocus) {
                MultiWindowPlayerManager.setVolume(handleId, store.volumes[stream.id] ?: 1f)
            } else {
                MultiWindowPlayerManager.setVolume(handleId, 0f)
            }
        }
        onDispose {
            MultiWindowPlayerManager.releasePlayerForStream(stream.id)
            store.playerHandleIds.remove(stream.id)
        }
    }

    // Sync play/pause
    LaunchedEffect(stream.isPlaying) {
        val handleId = store.playerHandleIds[stream.id] ?: return@LaunchedEffect
        if (stream.isPlaying) {
            MultiWindowPlayerManager.play(handleId)
        } else {
            MultiWindowPlayerManager.pause(handleId)
        }
    }

    // Sync audio focus
    LaunchedEffect(isAudioFocus) {
        val handleId = store.playerHandleIds[stream.id] ?: return@LaunchedEffect
        if (isAudioFocus) {
            MultiWindowPlayerManager.setVolume(handleId, store.volumes[stream.id] ?: 1f)
        } else {
            MultiWindowPlayerManager.setVolume(handleId, 0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 12f else 4f
            }
            .border(
                width = when {
                    isFocused -> 2.dp
                    isAudioFocus -> 2.dp
                    else -> 0.dp
                },
                color = when {
                    isFocused -> Color.White
                    isAudioFocus -> Color(0xFF4A90D9)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable { store.setAudioFocus(stream.id); onOpenCellOptions(stream.id) }
    ) {
        // Video surface
        MultiWindowVideoSurface(
            playerHandle = playerHandle,
            resizeMode = resizeMode,
            modifier = Modifier.fillMaxSize()
        )

        // Slot label (top-left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color(0xB3000000))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                "${stream.slotIndex + 1} ${stream.channel.name}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Stream health indicator
        val healthColor = when (health) {
            StreamHealth.Playing -> Color(0xFF00FF00)
            StreamHealth.Buffering -> Color(0xFFFFAA00)
            StreamHealth.Error -> Color.Red
            StreamHealth.Idle -> Color(0xFF444444)
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(8.dp)
                .background(healthColor, CircleShape)
        )

        // Play/Pause button (center)
        if (isFocused) {
            IconButton(
                onClick = { store.togglePlayPause(stream.id) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .background(Color(0x99000000), CircleShape)
            ) {
                Icon(
                    imageVector = if (stream.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (stream.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Audio indicator
        if (isAudioFocus) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Audio",
                tint = Color(0xFF4A90D9),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 4.dp)
                    .size(12.dp)
            )
        }

        // Fullscreen button (top-right) — plays in main ExoPlayer
        if (isFocused) {
            IconButton(
                onClick = {
                    IptvPlayerStore.launchedFromSlotIndex = stream.slotIndex
                    onPlayChannel(stream.channel)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 14.dp)
                    .size(28.dp)
                    .background(Color(0x99000000), CircleShape)
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Menu button (bottom-left)
        if (isFocused) {
            IconButton(
                onClick = { onOpenCellOptions(stream.id) },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(28.dp)
                    .background(Color(0x99000000), CircleShape)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
