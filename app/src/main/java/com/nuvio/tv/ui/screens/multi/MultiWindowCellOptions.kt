package com.robbdeeze.nuviotv.ui.screens.multi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robbdeeze.nuviotv.data.iptv.QuickChannelList
import com.robbdeeze.nuviotv.domain.model.IptvChannel

private val qcTabs = listOf("All", "US", "UK", "CA", "Premium", "Sports", "News")

enum class CellOptionsSection { Main, ChannelPicker, Info }

@Composable
fun MultiWindowCellOptions(
    streamId: String,
    onDismiss: () -> Unit,
    allChannels: List<IptvChannel>,
    historyChannels: List<IptvChannel>,
    favoriteChannels: List<IptvChannel>,
    onPlayChannel: (IptvChannel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val store = MultiWindowStore
    val stream = store.streams.find { it.id == streamId } ?: run {
        onDismiss()
        return
    }

    var section by remember { mutableStateOf(CellOptionsSection.Main) }
    var pickerType by remember { mutableStateOf<String?>(null) }
    val currentVolume = store.volumes[streamId] ?: 1f
    val currentResizeMode = store.resizeModes[streamId] ?: RESIZE_FIT
    val isAudioFocus = store.audioFocusId == streamId

    val focusManager = LocalFocusManager.current
    val tabsFr = remember { FocusRequester() }
    val volumeFr = remember { FocusRequester() }
    val scalingFr = remember { FocusRequester() }
    val actionsFr = remember { FocusRequester() }
    val closeFr = remember { FocusRequester() }

    LaunchedEffect(Unit) { tabsFr.requestFocus() }

    Column(modifier = modifier.fillMaxWidth()) {
        when (section) {
            CellOptionsSection.Main -> {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Cell Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("MultiNutz Window ${stream.slotIndex + 1}", color = Color(0xFFc1c7d2), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Quick Nav Tabs (CH / History / Fav) — Stitch style
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().focusProperties { down = volumeFr }) {
                    QuickNavTab("CH", Icons.Default.List, Color(0xFFa0caff), { pickerType = "ch"; section = CellOptionsSection.ChannelPicker }, focusReq = tabsFr)
                    QuickNavTab("History", Icons.Default.History, Color(0xFFc1c7d2), { pickerType = "history"; section = CellOptionsSection.ChannelPicker })
                    QuickNavTab("Fav", Icons.Default.Favorite, Color(0xFFc1c7d2), { pickerType = "fav"; section = CellOptionsSection.ChannelPicker })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Discrete Volume Pills
                Text("Volume", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().focusProperties { up = tabsFr; down = scalingFr }
                ) {
                    val volumeLevels = listOf("0%" to 0f, "25%" to 0.25f, "50%" to 0.5f, "75%" to 0.75f, "100%" to 1f)
                    volumeLevels.forEachIndexed { i, (label, vol) ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isActive = if (vol == 0f) currentVolume == 0f
                                      else currentVolume in (vol - 0.12f)..(vol + 0.12f)
                        Surface(
                            onClick = { store.setVolume(streamId, vol) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isActive) Color(0xFF4A90D9) else if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color.White else Color(0xFF333333)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .then(if (i == 0) Modifier.focusRequester(volumeFr) else Modifier)
                                .onFocusChanged { isFocused = it.isFocused }
                        ) {
                            Text(
                                label,
                                color = if (isActive) Color.White else Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scaling pills
                Text("Scaling", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().focusProperties { up = volumeFr; down = actionsFr }
                ) {
                    val modes = listOf(
                        "Fill" to RESIZE_FILL,
                        "Fit" to RESIZE_FIT,
                        "16:9" to RESIZE_FIXED_WIDTH,
                        "4:3" to RESIZE_FIXED_HEIGHT,
                        "Zoom" to RESIZE_ZOOM
                    )
                    modes.forEachIndexed { i, (label, mode) ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isActive = currentResizeMode == mode
                        Surface(
                            onClick = { store.setResizeMode(streamId, mode) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isActive) Color(0xFF4A90D9) else if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) Color.White else Color(0xFF333333)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .then(if (i == 0) Modifier.focusRequester(scalingFr) else Modifier)
                                .onFocusChanged { isFocused = it.isFocused }
                        ) {
                            Text(
                                label,
                                color = if (isActive) Color.White else Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons row: Fullscreen, Refresh, Info, Close
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().focusProperties { up = scalingFr; down = closeFr }
                ) {
                    // Fullscreen — opens in main ExoPlayer
                    ActionButton("Fullscreen", Icons.Default.Fullscreen, Color(0xFF00A572), focusReq = actionsFr) {
                        onPlayChannel(stream.channel)
                        onDismiss()
                    }
                    // Refresh
                    ActionButton("Refresh", Icons.Default.Refresh, Color(0xFF4A90D9), onClick = {
                        store.refreshStream(streamId)
                        onDismiss()
                    })
                    // Info
                    ActionButton("Info", Icons.Default.Info, Color(0xFF888888), onClick = {
                        section = CellOptionsSection.Info
                    })
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Channel
                var closeFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = {
                        store.remove(streamId)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (closeFocused) Color(0xFF4A1A1A) else Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (closeFocused) 2.dp else 1.dp,
                        color = if (closeFocused) Color.Red else Color(0xFF333333)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .focusRequester(closeFr)
                        .focusProperties { up = actionsFr }
                        .onFocusChanged { closeFocused = it.isFocused }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Close Channel", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }

            CellOptionsSection.Info -> {
                val infoBackFr = remember { FocusRequester() }
                LaunchedEffect(Unit) { infoBackFr.requestFocus() }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(onClick = { section = CellOptionsSection.Main; focusManager.moveFocus(FocusDirection.Up) }, modifier = Modifier
                        .size(32.dp)
                        .focusRequester(infoBackFr)
                        .onFocusChanged { backFocused = it.isFocused }
                        .border(if (backFocused) 2.dp else 0.dp, if (backFocused) Color.White else Color.Transparent, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Stream Info", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                Column {
                    InfoRow("Channel", stream.channel.name)
                    InfoRow("URL", stream.channel.url)
                    val handleId = store.playerHandleIds[streamId]
                    val player = handleId?.let { MultiWindowPlayerManager.getPlayer(it) }
                    InfoRow("Duration", if (player != null) "${player.duration / 1000}s" else "N/A")
                    InfoRow("Buffered", if (player != null) "${player.bufferedPercentage}%" else "N/A")
                    InfoRow("Volume", "${((store.volumes[streamId] ?: 1f) * 100).toInt()}%")
                    InfoRow("Audio Focus", if (store.audioFocusId == streamId) "Yes" else "No")
                    InfoRow("Slot", "${stream.slotIndex + 1}")
                    InfoRow("Playing", if (stream.isPlaying) "Yes" else "No")
                }
                Spacer(Modifier.height(8.dp))
                var backBtnFocused by remember { mutableStateOf(false) }
                Surface(onClick = { section = CellOptionsSection.Main }, shape = RoundedCornerShape(8.dp),
                    color = if (backBtnFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(if (backBtnFocused) 2.dp else 1.dp, if (backBtnFocused) Color.White else Color(0xFF333333)),
                    modifier = Modifier.fillMaxWidth().height(36.dp).onFocusChanged { backBtnFocused = it.isFocused }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            CellOptionsSection.ChannelPicker -> {
                var cpPickerTab by remember { mutableStateOf(pickerType ?: "ch") }
                var cpSearch by remember { mutableStateOf("") }
                var qcRegion by remember { mutableStateOf("All") }
                val pickerBackFr = remember { FocusRequester() }
                val searchFr = remember { FocusRequester() }
                LaunchedEffect(Unit) { pickerBackFr.requestFocus() }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    var backFocused by remember { mutableStateOf(false) }
                    IconButton(onClick = { section = CellOptionsSection.Main; pickerType = null; focusManager.moveFocus(FocusDirection.Up) }, modifier = Modifier.size(32.dp).focusRequester(pickerBackFr).onFocusChanged { backFocused = it.isFocused }.border(if (backFocused) 2.dp else 0.dp, if (backFocused) Color.White else Color.Transparent, RoundedCornerShape(50))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Channels", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                // Tabs
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    listOf("ch" to "Channels", "fav" to "Favorites", "history" to "History", "quick" to "Quick").forEach { (key, label) ->
                        var tabFocused by remember { mutableStateOf(false) }
                        Surface(onClick = { cpPickerTab = key; cpSearch = "" }, shape = RoundedCornerShape(8.dp),
                            color = if (cpPickerTab == key) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            border = androidx.compose.foundation.BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                            modifier = Modifier.weight(1f).height(30.dp).onFocusChanged { tabFocused = it.isFocused }
                        ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(label, color = if (cpPickerTab == key) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                    }
                }
                if (cpPickerTab == "quick") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        items(qcTabs) { tab ->
                            var tabFocused by remember { mutableStateOf(false) }
                            Surface(onClick = { qcRegion = tab }, shape = RoundedCornerShape(16.dp),
                                color = if (qcRegion == tab) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                border = androidx.compose.foundation.BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                                modifier = Modifier.height(26.dp).onFocusChanged { tabFocused = it.isFocused }
                            ) { Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) { Text(tab, color = if (qcRegion == tab) Color.White else Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
                        }
                    }
                    val qcFiltered = QuickChannelList.all.filter { qc ->
                        when (qcRegion) {
                            "All" -> true; "US" -> "US" in qc.regions; "UK" -> "UK" in qc.regions; "CA" -> "CA" in qc.regions
                            "Premium" -> "premium" in qc.tags; "Sports" -> "sports" in qc.tags; "News" -> "news" in qc.tags
                            else -> true
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(qcFiltered, key = { it.displayName }) { qc ->
                            var rowFocused by remember { mutableStateOf(false) }
                            val streamSlot = store.streams.find { it.id == streamId }?.slotIndex ?: 0
Surface(
                                onClick = {
                                    val matches = allChannels.filter { ch -> QuickChannelList.matches(qc, ch) }
                                    if (matches.isNotEmpty()) { store.addToSlot(matches.first(), streamSlot, streamId); onDismiss() }
                                },
                                shape = RoundedCornerShape(6.dp), color = if (rowFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                                border = androidx.compose.foundation.BorderStroke(if (rowFocused) 2.dp else 0.dp, if (rowFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().height(36.dp).onFocusChanged { rowFocused = it.isFocused }
                            ) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(qc.displayName, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    val m = allChannels.count { ch -> QuickChannelList.matches(qc, ch) }
                                    Text("$m", color = if (m > 0) Color(0xFF4A90D9) else Color(0xFF666666), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    val rawChannels = when (cpPickerTab) {
                        "fav" -> favoriteChannels; "history" -> historyChannels; else -> allChannels
                    }
                    // Search
                    var sf by remember { mutableStateOf(false) }
                    LaunchedEffect(sf) { if (sf) searchFr.requestFocus() }
                    BasicTextField(
                        value = cpSearch, onValueChange = { cpSearch = it }, singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth().height(30.dp).background(if (sf) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .border(if (sf) 1.5.dp else 0.dp, if (sf) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                            .onFocusChanged { sf = it.isFocused }.padding(horizontal = 8.dp).focusRequester(searchFr),
                        decorationBox = { itf -> Box { if (cpSearch.isEmpty()) Text("Search...", color = Color(0xFF555555), fontSize = 12.sp); itf() } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { })
                    )
                    Spacer(Modifier.height(6.dp))
                    // Channel list
                    val filtered = if (cpSearch.isBlank()) rawChannels.take(200) else rawChannels.filter { it.name.contains(cpSearch, ignoreCase = true) }.take(200)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(filtered, key = { it.url }) { ch ->
                            var isFocused by remember { mutableStateOf(false) }
                            val streamSlot = store.streams.find { it.id == streamId }?.slotIndex ?: 0
                            Surface(
                                onClick = { store.addToSlot(ch, streamSlot, streamId); onDismiss() },
                                shape = RoundedCornerShape(6.dp), color = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                                border = androidx.compose.foundation.BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().height(40.dp).onFocusChanged { isFocused = it.isFocused }
                            ) {
                                Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(ch.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text("Slot ${streamSlot + 1}", color = Color(0xFF4A90D9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", color = Color(0xFF888888), fontSize = 11.sp, modifier = Modifier.width(80.dp))
        Text(value, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RowScope.ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    focusReq: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isFocused) color.copy(alpha = 0.2f) else Color(0xFF1A1A1A),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else color.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .then(if (focusReq != null) Modifier.focusRequester(focusReq) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (isFocused) Color.White else color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (isFocused) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RowScope.QuickNavTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    focusReq: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF272a2f), contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f).height(64.dp)
            .then(if (focusReq != null) Modifier.focusRequester(focusReq) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (isFocused) Color.White else iconColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label.uppercase(), color = if (isFocused) Color.White else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}
