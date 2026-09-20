package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.vector.ImageVector
import com.robbdeeze.nuviotv.ui.components.FocusMarqueeText
import com.robbdeeze.nuviotv.ui.components.TrailerPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.data.remote.api.ExternalStreamsClient
import com.robbdeeze.nuviotv.data.portalnutz.PortalNutzEntry
import com.robbdeeze.nuviotv.data.portalnutz.PortalNutzScraper
import com.robbdeeze.nuviotv.BuildConfig
import androidx.compose.ui.res.stringResource
import com.robbdeeze.nuviotv.R
import com.robbdeeze.nuviotv.data.local.LicenseStatus
import com.robbdeeze.nuviotv.data.local.LicenseResult
import com.robbdeeze.nuviotv.data.local.PortalLicenseKey
import com.robbdeeze.nuviotv.data.remote.dto.EspnStandingEntry
import com.robbdeeze.nuviotv.data.sports.YouTubeStreamResolver
import com.robbdeeze.nuviotv.data.sports.PpvStClient
import com.robbdeeze.nuviotv.data.sports.StreamedPkClient
import com.robbdeeze.nuviotv.data.sports.StreamSports99Client
import com.robbdeeze.nuviotv.ui.screens.sports.TvSportsLayout
import com.robbdeeze.nuviotv.ui.screens.sports.DpadCard
import com.robbdeeze.nuviotv.ui.screens.sports.TvDaddyLiveCard
import com.robbdeeze.nuviotv.data.youtube.VideoSuggestionEngine
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.model.*
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowCellOptions
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowGrid
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowSlotPicker
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowSlotsStore
import com.robbdeeze.nuviotv.data.local.ChannelHistoryStore
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowPushStore
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowSlotPicker
import com.robbdeeze.nuviotv.ui.screens.multi.MultiWindowStore
import com.robbdeeze.nuviotv.ui.screens.player.IptvPlayerStore
import com.robbdeeze.nuviotv.ui.screens.player.SportsNowStore
import com.robbdeeze.nuviotv.ui.screens.hub.ExternalStreamsSubScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RobbdeezeNutzHubScreen(
    returnToIptvTrigger: Boolean = false,
    onPlayChannel: (IptvChannel) -> Unit,
    onBackPress: () -> Unit,
    viewModel: RobbdeezeNutzHubViewModel = hiltViewModel()
) {
    val subScreen by viewModel.subScreen.collectAsState()

    // Store return target before navigating to player
    fun storeReturnTarget(target: String) {
        IptvPlayerStore.returnToSubScreen = target
    }

    // Track active sub-screen so player knows where to return
    LaunchedEffect(subScreen) {
        IptvPlayerStore.returnToSubScreen = when (subScreen) {
            HubSubScreen.Iptv -> "Iptv"
            HubSubScreen.Sports -> "Sports"
            HubSubScreen.VidNutz -> "VidNutz"
            HubSubScreen.MusicNutz -> "MusicNutz"
            HubSubScreen.Multi -> "Multi"
            HubSubScreen.MagNutz -> "MagNutz"
            else -> "Iptv"
        }
    }

    LaunchedEffect(returnToIptvTrigger) {
        if (returnToIptvTrigger) {
            val target = when (IptvPlayerStore.returnToSubScreen) {
                "Iptv" -> HubSubScreen.Iptv
                "Sports" -> HubSubScreen.Sports
                "VidNutz" -> HubSubScreen.VidNutz
                "MusicNutz" -> HubSubScreen.MusicNutz
                "Multi" -> HubSubScreen.Multi
                "MagNutz" -> HubSubScreen.MagNutz
                else -> HubSubScreen.Iptv
            }
            viewModel.setSubScreen(target)
            
            // Restore VidNutz scroll/context if returning from a video
            val ctx = viewModel.restoreVidNutzReturnContext()
            if (ctx != null) {
                viewModel.setVidNutzScrollPosition(ctx.scrollPosition)
                viewModel.setVidNutzInSearchMode(ctx.isInSearchMode)
                viewModel.setVidNutzSearchQuery(ctx.searchQuery)
                viewModel.setVidNutzSelectedCategory(ctx.selectedCategory)
                viewModel.clearVidNutzReturnContext()
            }
        }
    }
    val scope = rememberCoroutineScope()

    // Auto-navigate to MagNutz when a magnet link is received from another app
    LaunchedEffect(Unit) {
        if (RobbdeezeNutzHubViewModel.pendingMagnetUri != null) {
            viewModel.setSubScreen(HubSubScreen.MagNutz)
        }
    }

    // Tab Reset flow
    LaunchedEffect(Unit) {
        viewModel.resetEvent.collect {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    }

    val hasMultiStreams = MultiWindowStore.streams.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Hubz title header
            if (subScreen == HubSubScreen.Hub) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0D1B2A), Color(0xFF000000))))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hub_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
            // MultiNutz header bar (when streams active and not on Multi sub-screen)
            if (subScreen != HubSubScreen.Multi && hasMultiStreams) {
                var barFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { viewModel.setSubScreen(HubSubScreen.Multi) },
                    shape = RoundedCornerShape(0.dp),
                    color = if (barFocused) Color(0xFF1A2A3A) else Color(0xFF0D1B2A),
                    border = BorderStroke(if (barFocused) 1.dp else 0.dp, Color(0xFF4A90D9)),
                    modifier = Modifier.fillMaxWidth().height(40.dp).onFocusChanged { barFocused = it.isFocused }
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color(0xFF00FF00), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("MultiNutz: ${MultiWindowStore.streams.size} stream${if (MultiWindowStore.streams.size != 1) "s" else ""} active", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text("Switch ▸", color = if (barFocused) Color.White else Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Screen Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (subScreen) {
                    HubSubScreen.Hub -> {
                        HubScreenContent(
                            onSelectScreen = { viewModel.setSubScreen(it) }
                        )
                    }
                    HubSubScreen.Iptv -> {
                        IptvSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                    HubSubScreen.Sports -> {
                        SportsSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                    HubSubScreen.VidNutz -> {
                        VidNutzSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                    HubSubScreen.MusicNutz -> {
                        MusicNutzSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                    HubSubScreen.MagNutz -> {
                        MagNutzSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                    HubSubScreen.Multi -> {
                        val multiAllChannels by viewModel.iptvChannels.collectAsState()
                        val multiFavs by viewModel.iptvFavorites.collectAsState(emptyList())
                        val multiSources by viewModel.iptvSources.collectAsState(emptyList())
                        MultiWindowSubScreen(
                            allChannels = multiAllChannels,
                            favoriteChannels = multiFavs,
                            iptvSources = multiSources,
                            onPlayChannel = onPlayChannel,
                            onLoadIptvChannels = { source -> viewModel.loadIptvChannelsWithName(source) },
                            onNavigateToIptv = { viewModel.setSubScreen(HubSubScreen.Iptv) }
                        )
                    }
                    HubSubScreen.ExternalStreams -> {
                        ExternalStreamsSubScreen(
                            viewModel = viewModel,
                            onPlayChannel = onPlayChannel
                        )
                    }
                }
            }
        }

        val loadingMsg by viewModel.playerLoadingMessage.collectAsState()
        if (loadingMsg != null) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(loadingMsg!!, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun HubScreenContent(
    onSelectScreen: (HubSubScreen) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                item { HubCard(stringResource(R.string.hub_iptv_nutz), Color(0xFF4A90D9), stringResource(R.string.hub_iptv_nutz), onClick = { onSelectScreen(HubSubScreen.Iptv) }) }
                item { HubCard(stringResource(R.string.hub_sport_nutz), Color(0xFFE8553A), stringResource(R.string.hub_sport_nutz), onClick = { onSelectScreen(HubSubScreen.Sports) }) }
                item { HubCard(stringResource(R.string.hub_video_nutz), Color(0xFF6C5CE7), stringResource(R.string.hub_video_nutz), onClick = { onSelectScreen(HubSubScreen.VidNutz) }) }
                item { HubCard(stringResource(R.string.hub_music_nutz), Color(0xFF00CEC9), stringResource(R.string.hub_music_nutz), onClick = { onSelectScreen(HubSubScreen.MusicNutz) }) }
                item { HubCard(stringResource(R.string.hub_multi_nutz), Color(0xFFE8553A), stringResource(R.string.hub_multi_nutz), onClick = { onSelectScreen(HubSubScreen.Multi) }) }
                item { HubCard(stringResource(R.string.hub_streamz), Color(0xFF00FFC9), stringResource(R.string.hub_streamz), onClick = { onSelectScreen(HubSubScreen.ExternalStreams) }) }
            }
        }
    }
}

@Composable
fun HubCard(
    badge: String,
    badgeColor: Color,
    subtitle: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f),
        label = "cardScale"
    )

    val glassColor = Color.White.copy(alpha = 0.06f)
    val unfocusedBorder = Color.White.copy(alpha = 0.12f)
    val focusedBorder = Color.White.copy(alpha = 0.5f)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = glassColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isFocused) focusedBorder else unfocusedBorder
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 16f else 4f
            }
            .width(140.dp)
            .height(120.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = badge,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(badgeColor, CircleShape)
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingGlassHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    searchPlaceholder: String = "",
    onSearch: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    Box(modifier = modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent))).padding(horizontal = 64.dp, vertical = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (title.isNotEmpty()) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
            }
            Spacer(Modifier.weight(1f))
            if (searchPlaceholder.isNotEmpty()) {
                var sf by remember { mutableStateOf(false) }
                Row(modifier = Modifier.width(320.dp).background(if (sf) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(if (sf) 2.dp else 0.dp, if (sf) Color.White.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(12.dp)).onFocusChanged { sf = it.isFocused }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, "Search", tint = Color(0xFFc1c7d2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it; onSearch?.invoke(it) }, textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White), cursorBrush = SolidColor(Color.White), modifier = Modifier.weight(1f).focusRequester(searchFocusRequester), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(searchQuery) }), decorationBox = { itf -> Box { if (searchQuery.isEmpty()) Text(searchPlaceholder, color = Color(0xFFc1c7d2), style = MaterialTheme.typography.bodyMedium); itf() } })
                }
            }
            trailingContent()
        }
    }
}

@Composable
fun IptvSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val sources by viewModel.iptvSources.collectAsState(emptyList())
    val favorites by viewModel.iptvFavorites.collectAsState(emptyList())
    val channels by viewModel.iptvChannels.collectAsState()
    val loading by viewModel.iptvLoading.collectAsState()
    val progressText by viewModel.iptvLoadProgress.collectAsState()
    val activeSourceName by viewModel.activeIptvSourceName.collectAsState()
    val searchQuery by viewModel.iptvSearchQuery.collectAsState()
    val categories by viewModel.iptvCategories.collectAsState()
    val iptvCtx = androidx.compose.ui.platform.LocalContext.current
    val historyStore = remember { ChannelHistoryStore(iptvCtx) }
    val history by historyStore.historyFlow.collectAsState(initial = emptyList())
    val activeSource by viewModel.activeIptvSource.collectAsState()
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var newSourceType by remember { mutableStateOf("m3u") }
    var newSourceMac by remember { mutableStateOf("") }
    var newSourceUsername by remember { mutableStateOf("") }
    var newSourcePassword by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var channelPopupChannel by remember { mutableStateOf<IptvChannel?>(null) }
    var channelPopupShow by remember { mutableStateOf(false) }
    val popupFocusReq = remember { FocusRequester() }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var qcShowPopup by remember { mutableStateOf<Pair<String, List<IptvChannel>>?>(null) }
    var showCustomQcPopup by remember { mutableStateOf(false) }
    var newQcName by remember { mutableStateOf("") }
    var newQcAliases by remember { mutableStateOf("") }
    var newQcRegions by remember { mutableStateOf("") }
    var newQcTags by remember { mutableStateOf("") }
    var sourceMenuSource by remember { mutableStateOf<IptvSource?>(null) }
    val sourceMenuFocusReq = remember { FocusRequester() }
    
    fun addToFirstAvailableSlot(ch: IptvChannel) {
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
    BackHandler { if (channelPopupShow) { channelPopupShow = false; channelPopupChannel = null } else if (sourceMenuSource != null) { sourceMenuSource = null } else if (activeSource != null) viewModel.setActiveIptvSource(null) else viewModel.setSubScreen(HubSubScreen.Hub) }
    Box(Modifier.fillMaxSize()) {
    LaunchedEffect(activeSource) {
        activeSource?.let {
            if (it.url == "__all__") {
                viewModel.setIptvSearchQuery("")
                showSearch = false
                viewModel.loadAllIptvChannels(sources)
            } else {
                viewModel.setIptvSearchQuery("")
                showSearch = false
                viewModel.loadIptvChannelsWithName(it)
            }
        }
    }

    val allQuickChannels by viewModel.allIptvChannels.collectAsState()
    LaunchedEffect(sources) {
        if (activeSource == null && sources.isNotEmpty()) {
            viewModel.loadAllIptvChannelsForQuick(sources)
        }
    }
    LaunchedEffect(channels) {
        if (channels.isNotEmpty()) {
            viewModel.loadDeadUrls()
            viewModel.runPendingValidation(channels)
        }
    }
    val deadUrlSet by viewModel.deadUrls.collectAsState()
    val validationMsg by viewModel.validationProgress.collectAsState()
    if (activeSource == null) {
        // Sources Dashboard
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(start = 5.dp)
        ) {
            // History Section
            if (history.isNotEmpty()) {
                item {
                    Text("Recently Watched", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(history.take(20)) { channel ->
                            var isFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { channelPopupChannel = channel; channelPopupShow = true },
                                colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(100.dp)
                                    .onFocusChanged { isFocused = it.isFocused }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                                    Box(Modifier.fillMaxSize().align(Alignment.BottomCenter).height(50.dp)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                    Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Favorite Channels Section
            if (favorites.isNotEmpty()) {
                item {
                    Text("Favorite Channels", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favorites) { channel ->
                            var isFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { channelPopupChannel = channel; channelPopupShow = true },
                                colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(100.dp)
                                    .onFocusChanged { isFocused = it.isFocused }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                                    Box(Modifier.fillMaxSize().align(Alignment.BottomCenter).height(50.dp)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                    Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Quick Channels Section
            item {
                var qcRegion by remember { mutableStateOf("All") }
                val qcTabs = listOf("All", "US", "UK", "CA", "Premium", "Sports", "News")
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quick Channels", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        var manageFocused by remember { mutableStateOf(false) }
                        Surface(onClick = { showCustomQcPopup = true }, shape = RoundedCornerShape(6.dp),
                            color = if (manageFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            border = BorderStroke(if (manageFocused) 1.dp else 0.dp, if (manageFocused) Color.White else Color.Transparent),
                            modifier = Modifier.height(26.dp).onFocusChanged { manageFocused = it.isFocused }
                        ) {
                            Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                Text("Manage Custom", color = if (manageFocused) Color.White else Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(qcTabs) { tab ->
                            var tabFocused by remember { mutableStateOf(false) }
                            Surface(onClick = { qcRegion = tab }, shape = RoundedCornerShape(16.dp),
                                color = if (qcRegion == tab) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                border = BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                                modifier = Modifier.height(30.dp).onFocusChanged { tabFocused = it.isFocused }
                            ) {
                                Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                    Text(tab, color = if (qcRegion == tab) Color.White else Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val baseQcList = com.robbdeeze.nuviotv.data.iptv.QuickChannelList.all
                    val customQcList = viewModel.customQuickChannels.value
                    val allQcList = baseQcList + customQcList
                    val filtered = allQcList.filter { qc ->
                        when (qcRegion) {
                            "All" -> true
                            "US" -> "US" in qc.regions
                            "UK" -> "UK" in qc.regions
                            "CA" -> "CA" in qc.regions
                            "Premium" -> "premium" in qc.tags
                            "Sports" -> "sports" in qc.tags
                            "News" -> "news" in qc.tags
                            else -> true
                        }
                    }
                    if (filtered.isEmpty()) {
                        Text("No quick channels for this region", color = Color(0xFF666666), fontSize = 12.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.displayName }) { quickCh ->
                                var qcFocused by remember { mutableStateOf(false) }
                                val qcScale by animateFloatAsState(if (qcFocused) 1.08f else 1f, tween(150), label = "qcScale")
                                Card(
                                    onClick = {
                                        val matches = allQuickChannels.filter { ch ->
                                            com.robbdeeze.nuviotv.data.iptv.QuickChannelList.matches(quickCh, ch)
                                        }
                                        if (matches.isNotEmpty()) {
                                            qcShowPopup = quickCh.displayName to matches
                                        } else {
                                            toastMessage = "${quickCh.displayName} not found in sources"
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (qcFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(if (qcFocused) 2.dp else 0.dp, if (qcFocused) Color.White else Color.Transparent),
                                    modifier = Modifier
                                        .width(150.dp)
                                        .height(70.dp)
                                        .onFocusChanged { qcFocused = it.isFocused }
                                        .graphicsLayer { scaleX = qcScale; scaleY = qcScale }
                                ) {
                                    Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                        Text(quickCh.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sources Management
            item {
                Text("Playlist Sources", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Source cards (horizontal row)
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // View All card
                    if (sources.size > 1) {
                        item {
                            var isFocused by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, tween(150), label = "srcScale")
                            Card(
                                onClick = { viewModel.setActiveIptvSource(IptvSource("All", "__all__", "m3u")) },
                                colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color(0xFF4A90D9) else Color.Transparent),
                                modifier = Modifier
                                    .width(140.dp).height(140.dp)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = if (isFocused) 12f else 0f }
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(40.dp).background(Color(0xFF4A90D9).copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                            Text("A", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("All Sources", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    items(sources) { source ->
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, tween(150), label = "srcScale")
                        val isPortal = source.name.lowercase().startsWith("portal") && source.name.lastOrNull()?.isDigit() == true
                        val portalNum = if (isPortal) source.name.filter { it.isDigit() } else ""
                        Card(
                            onClick = { sourceMenuSource = source },
                            colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                            modifier = Modifier
                                .width(170.dp).height(140.dp)
                                .onFocusChanged { isFocused = it.isFocused }
                                .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = if (isFocused) 12f else 0f }
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                if (isPortal) {
                                    Text("P$portalNum", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 48.sp, modifier = Modifier.align(Alignment.Center))
                                } else {
                                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                                        Text(source.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.weight(1f))
                                        Text(source.type.uppercase(), color = Color(0xFF4A90D9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Add Preset
            item {
                var isFocused by remember { mutableStateOf(false) }
                Card(
                    onClick = {
                        val exists = sources.any { it.url.contains("iptv-org.github.io") }
                        if (!exists) {
                            if (sources.size < 10) viewModel.addIptvSource("IPTV-org Global", "https://iptv-org.github.io/iptv/index.m3u", "m3u") else toastMessage = "Source limit reached (10 max)"
                        }
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF0D1B2A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF4A90D9).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4A90D9).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quick Add: IPTV-org Global", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("5000+ free channels from around the world", color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                        }
                        if (sources.any { it.url.contains("iptv-org.github.io") }) {
                            Text("Added", color = Color(0xFF00FF00), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // PortalNutz
            item {
                var collapsed by remember { mutableStateOf(true) }
                var englishOnly by remember { mutableStateOf(true) }
                var noAdult by remember { mutableStateOf(true) }
                var sportsOnly by remember { mutableStateOf(false) }
                var adultOnly by remember { mutableStateOf(false) }
                var searching by remember { mutableStateOf(false) }
                var searchError by remember { mutableStateOf<String?>(null) }
                var results by remember { mutableStateOf<List<PortalNutzEntry>?>(null) }
                var progressMsg by remember { mutableStateOf("") }
                var keyInput by remember { mutableStateOf("") }
                var keyFocused by remember { mutableStateOf(false) }

                val portalScope = rememberCoroutineScope()
                val portalLicense by viewModel.portalLicense.collectAsState()
                val portalLicenseStatus by viewModel.portalLicenseStatus.collectAsState()
                val hasAccess = portalLicenseStatus == LicenseStatus.VALID || portalLicenseStatus == LicenseStatus.GRACE

                DisposableEffect(Unit) {
                    onDispose {
                        PortalNutzScraper.cancel()
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFF4A90D9).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).background(Color(0xFF4A90D9).copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, null, tint = Color(0xFF4A90D9), modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("PortalNutz", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            var expandFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { collapsed = !collapsed }, modifier = Modifier.onFocusChanged { expandFocused = it.isFocused }) {
                                Icon(if (collapsed) Icons.Default.Add else Icons.Default.Clear, null, tint = if (expandFocused) Color.White else Color(0xFF888888))
                            }
                        }

                        // Paywall UI
                        if (!hasAccess) {
                            if (!collapsed) {
                                Spacer(Modifier.height(12.dp))
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Lock icon + header
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, "Lock", tint = Color(0xFFE8553A), modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("RD NUTZ LISTS LOCKED", color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    // Status message
                                    Text(
                                        when (portalLicenseStatus) {
                                            LicenseStatus.NOT_ACTIVATED -> "Enter a license key to unlock PortalNutz"
                                            LicenseStatus.EXPIRED -> "License expired. Please renew."
                                            LicenseStatus.GRACE -> "License expiring soon (grace period)"
                                            LicenseStatus.WRONG_DEVICE -> "License bound to different device"
                                            LicenseStatus.INVALID -> "Invalid license"
                                            else -> "License required"
                                        },
                                        color = Color(0xFF888888),
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    // Key input
                                    var keyError by remember { mutableStateOf<String?>(null) }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextField(
                                            value = keyInput,
                                            onValueChange = { keyInput = it; keyError = null },
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (keyFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    if (keyError != null) 1.dp else if (keyFocused) 2.dp else 1.dp,
                                                    if (keyError != null) Color(0xFFE8553A) else if (keyFocused) Color(0xFF4A90D9) else Color.White.copy(alpha = 0.2f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                .onFocusChanged { keyFocused = it.isFocused },
                                            label = { Text("NVIO-XXXX-XXXX-XXXX", color = Color(0xFF666666), fontSize = 13.sp) },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = {
                                                 if (keyInput.isNotBlank()) {
                                                     val result = viewModel.activatePortalLicense(keyInput)
                                                     when (result) {
                                                         is LicenseResult.Success -> { /* Success handled by state updates */ }
                                                         is LicenseResult.Failure -> keyError = result.message
                                                     }
                                                 }
                                             })
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        var btnFocused by remember { mutableStateOf(false) }
                                        Button(
                                            onClick = {
                                                    if (keyInput.isNotBlank()) {
                                                        val result = viewModel.activatePortalLicense(keyInput)
                                                        when (result) {
                                                            is LicenseResult.Success -> { /* Success handled by state updates */ }
                                                            is LicenseResult.Failure -> keyError = result.message
                                                        }
                                                    }
                                                },
                                            enabled = keyInput.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (btnFocused) Color(0xFF4A90D9).copy(alpha = 0.8f) else Color(0xFF4A90D9),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color(0xFF333333)
                                            ),
                                            modifier = Modifier.onFocusChanged { btnFocused = it.isFocused }
                                        ) { Text("ACTIVATE") }
                                    }
                                    if (keyError != null) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(keyError!!, color = Color(0xFFE8553A), fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            // Has access - show status banner
                            if (!collapsed) {
                                Spacer(Modifier.height(12.dp))
                                val license = portalLicense
                                val remainingTime = if (license != null) viewModel.portalLicenseManager.getRemainingTime(license) else "Expired"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF4A90D9).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, "Active", tint = Color(0xFF4A90D9), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("RdNutz Active  •  $remainingTime", color = Color(0xFF4A90D9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!collapsed) {
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        var chipFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { englishOnly = !englishOnly }, shape = RoundedCornerShape(16.dp),
                                        color = if (englishOnly) Color(0xFF4A90D9) else if (chipFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (chipFocused) 2.dp else 0.dp, if (chipFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(32.dp).onFocusChanged { chipFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("English", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                                }
                                item {
                                    var chipFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { noAdult = !noAdult }, shape = RoundedCornerShape(16.dp),
                                        color = if (noAdult) Color(0xFF4A90D9) else if (chipFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (chipFocused) 2.dp else 0.dp, if (chipFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(32.dp).onFocusChanged { chipFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("No XXX", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                                }
                                item {
                                    var chipFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { sportsOnly = !sportsOnly }, shape = RoundedCornerShape(16.dp),
                                        color = if (sportsOnly) Color(0xFFE8553A) else if (chipFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (chipFocused) 2.dp else 0.dp, if (chipFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(32.dp).onFocusChanged { chipFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("Sports", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                                }
                                item {
                                    var chipFocused by remember { mutableStateOf(false) }
                                    Surface(onClick = { adultOnly = !adultOnly }, shape = RoundedCornerShape(16.dp),
                                        color = if (adultOnly) Color(0xFFE8553A) else if (chipFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                        border = BorderStroke(if (chipFocused) 2.dp else 0.dp, if (chipFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.height(32.dp).onFocusChanged { chipFocused = it.isFocused }
                                    ) { Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) { Text("XXX", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            var btnFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    if (!searching) {
                                        searching = true
                                        searchError = null
                                        results = null
                                        progressMsg = "Starting..."
                                        portalScope.launch {
                                            PortalNutzScraper.scrape(
                                                filters = PortalNutzScraper.FilterState(
                                                    englishOnly = englishOnly,
                                                    noAdult = noAdult,
                                                    sportsOnly = sportsOnly,
                                                    adultOnly = adultOnly
                                                ),
                                                onEvent = { event ->
                                                    when (event) {
                                                        is PortalNutzScraper.ScrapeEvent.Progress -> progressMsg = event.message
                                                        is PortalNutzScraper.ScrapeEvent.Result -> { results = event.portals; searching = false; progressMsg = "" }
                                                        is PortalNutzScraper.ScrapeEvent.Error -> { searchError = event.message; searching = false; progressMsg = "" }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                                enabled = !searching,
                                colors = ButtonDefaults.buttonColors(containerColor = if (btnFocused) Color(0xFF4A90D9) else Color.White, contentColor = Color.Black, disabledContainerColor = Color(0xFF333333)),
                                modifier = Modifier.align(Alignment.Start).onFocusChanged { btnFocused = it.isFocused }
                            ) { Text(if (searching) "Searching..." else "Search Portals") }
                            if (progressMsg.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(color = Color(0xFF4A90D9), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(progressMsg, color = Color(0xFF888888), fontSize = 12.sp)
                                }
                            }
                            if (searchError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(searchError!!, color = Color(0xFFE8553A), fontSize = 12.sp)
                            }
                            if (results != null && results!!.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                var portalTab by remember { mutableStateOf("Channels") }
                                val portalTabs = listOf("Channels", "Movies", "Series")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(portalTabs) { idx ->
                                        var pt by remember { mutableStateOf(false) }
                                        Surface(
                                            onClick = { portalTab = idx },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (portalTab == idx) Color(0xFF4A90D9) else if (pt) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                            border = BorderStroke(if (pt) 2.dp else 0.dp, if (pt) Color.White else Color.Transparent),
                                            modifier = Modifier.height(32.dp).onFocusChanged { pt = it.isFocused }
                                        ) {
                                            Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                                                Text(idx, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                if (portalTab == "Channels") {
                                    results!!.forEach { entry ->
                                        var isFocused by remember { mutableStateOf(false) }
                                        var added by remember { mutableStateOf(false) }
                                        val displayLabel = "Portal ${entry.label.filter { it.isDigit() }}"
                                        Card(
                                            onClick = {
                                                if (!added) {
                                                    val license = viewModel.portalLicense.value
                                                    val currentPortalCount = sources.count { it.name.lowercase().startsWith("portal") || it.name.lowercase().startsWith("list") }
                                                    if (viewModel.portalLicenseManager.canAddPortal(license, currentPortalCount)) {
                                                        if (sources.size < 10) viewModel.addIptvSource(entry.label, "${entry.url}?username=${entry.username}&password=${entry.password}", "xtream") else toastMessage = "Source limit reached (10 max)"
                                                    } else {
                                                        toastMessage = "Portal limit reached (3 max for free users)"
                                                    }
                                                    added = true
                                                }
                                            },
                                            colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else if (added) Color(0xFF00FF00) else Color.Transparent),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).onFocusChanged { isFocused = it.isFocused }
                                        ) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(displayLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                                Text(if (added) "Added" else "Add", color = if (added) Color(0xFF00FF00) else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else if (portalTab == "Movies") {
                                    val loadedVod by viewModel.vodForActiveSource.collectAsState()
                                    LaunchedEffect(results) {
                                        results!!.forEach { entry ->
                                            val src = IptvSource(entry.label, "${entry.url}?username=${entry.username}&password=${entry.password}", "xtream")
                                            viewModel.loadVodForSource(src)
                                        }
                                    }
                                    if (loadedVod.isEmpty()) {
                                        Text("Loading movies...", color = Color(0xFF888888), fontSize = 12.sp)
                                    } else {
                                        LazyVerticalGrid(columns = GridCells.Fixed(5), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(300.dp)) {
                                            items(loadedVod.take(50)) { vod ->
                                                var vf by remember { mutableStateOf(false) }
                                                Card(
                                                    onClick = { /* TODO: play vod */ },
                                                    colors = CardDefaults.cardColors(containerColor = if (vf) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.onFocusChanged { vf = it.isFocused }
                                                ) {
                                                    Box {
                                                        AsyncImage(model = vod.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                                        Text(vod.name, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val loadedSeries by viewModel.seriesForActiveSource.collectAsState()
                                    LaunchedEffect(results) {
                                        results!!.forEach { entry ->
                                            val src = IptvSource(entry.label, "${entry.url}?username=${entry.username}&password=${entry.password}", "xtream")
                                            viewModel.loadSeriesForSource(src)
                                        }
                                    }
                                    if (loadedSeries.isEmpty()) {
                                        Text("Loading series...", color = Color(0xFF888888), fontSize = 12.sp)
                                    } else {
                                        LazyVerticalGrid(columns = GridCells.Fixed(5), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(300.dp)) {
                                            items(loadedSeries.take(50)) { series ->
                                                var sf by remember { mutableStateOf(false) }
                                                Card(
                                                    onClick = { /* TODO: play series */ },
                                                    colors = CardDefaults.cardColors(containerColor = if (sf) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.onFocusChanged { sf = it.isFocused }
                                                ) {
                                                    Box {
                                                        AsyncImage(model = series.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                                        Text(series.name, color = Color.White, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add New IPTV Source (always open)
            item {
                val nameFocus = remember { FocusRequester() }
                val urlFocus = remember { FocusRequester() }
                val m3uFocus = remember { FocusRequester() }
                val xtreamFocus = remember { FocusRequester() }
                val stalkerFocus = remember { FocusRequester() }
                val addFocus = remember { FocusRequester() }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val tfFocusManager = LocalFocusManager.current
                        Text("Add New IPTV Source", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Name field
                        var nameFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = newSourceName, onValueChange = { newSourceName = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                                .onFocusChanged { nameFocused = it.isFocused }
                                .focusProperties { down = urlFocus }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp) {
                                        when (event.key) {
                                            Key.DirectionDown -> { tfFocusManager.moveFocus(FocusDirection.Down); true }
                                            Key.DirectionUp -> { tfFocusManager.moveFocus(FocusDirection.Up); true }
                                            else -> false
                                        }
                                    } else false
                                },
                            label = { Text("Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (nameFocused) Color(0xFF4A90D9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // URL field
                        var urlFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = newSourceUrl, onValueChange = { newSourceUrl = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(urlFocus)
                                .onFocusChanged { urlFocused = it.isFocused }
                                .focusProperties { down = m3uFocus; up = nameFocus }
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp) {
                                        when (event.key) {
                                            Key.DirectionDown -> { tfFocusManager.moveFocus(FocusDirection.Down); true }
                                            Key.DirectionUp -> { tfFocusManager.moveFocus(FocusDirection.Up); true }
                                            else -> false
                                        }
                                    } else false
                                },
                            label = { Text("URL / Portal Address") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (urlFocused) Color(0xFF4A90D9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Type selector — clickable cards instead of radio buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var m3uFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { newSourceType = "m3u" },
                                colors = CardDefaults.cardColors(containerColor = if (newSourceType == "m3u") Color(0xFF4A90D9) else if (m3uFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (m3uFocused) 2.dp else 1.dp, if (m3uFocused) Color.White else Color(0x33FFFFFF)),
                                modifier = Modifier.width(140.dp)
                                    .focusRequester(m3uFocus).onFocusChanged { m3uFocused = it.isFocused }
                                    .focusProperties { down = xtreamFocus; up = urlFocus }
                            ) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("M3U", color = if (newSourceType == "m3u") Color.White else Color(0xFFB0B0B0), fontWeight = FontWeight.Bold)
                                }
                            }
                            var xtreamFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { newSourceType = "xtream" },
                                colors = CardDefaults.cardColors(containerColor = if (newSourceType == "xtream") Color(0xFF4A90D9) else if (xtreamFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (xtreamFocused) 2.dp else 1.dp, if (xtreamFocused) Color.White else Color(0x33FFFFFF)),
                                modifier = Modifier.width(180.dp)
                                    .focusRequester(xtreamFocus).onFocusChanged { xtreamFocused = it.isFocused }
                                    .focusProperties { down = stalkerFocus; up = m3uFocus }
                            ) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Xtream Codes", color = if (newSourceType == "xtream") Color.White else Color(0xFFB0B0B0), fontWeight = FontWeight.Bold)
                                }
                            }
                            var stalkerFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { newSourceType = "stalker" },
                                colors = CardDefaults.cardColors(containerColor = if (newSourceType == "stalker") Color(0xFF4A90D9) else if (stalkerFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (stalkerFocused) 2.dp else 1.dp, if (stalkerFocused) Color.White else Color(0x33FFFFFF)),
                                modifier = Modifier.width(180.dp)
                                    .focusRequester(stalkerFocus).onFocusChanged { stalkerFocused = it.isFocused }
                                    .focusProperties { down = addFocus; up = xtreamFocus }
                            ) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Stalker Portal", color = if (newSourceType == "stalker") Color.White else Color(0xFFB0B0B0), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Xtream username/password fields
                        if (newSourceType == "xtream") {
                            var userFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = newSourceUsername, onValueChange = { newSourceUsername = it },
                                modifier = Modifier.fillMaxWidth()
                                    .onFocusChanged { userFocused = it.isFocused }
                                    .focusProperties { down = stalkerFocus }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp) {
                                            when (event.key) {
                                                Key.DirectionDown -> { tfFocusManager.moveFocus(FocusDirection.Down); true }
                                                Key.DirectionUp -> { tfFocusManager.moveFocus(FocusDirection.Up); true }
                                                else -> false
                                            }
                                        } else false
                                    },
                                label = { Text("Username") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (userFocused) Color(0xFF4A90D9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var passFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = newSourcePassword, onValueChange = { newSourcePassword = it },
                                modifier = Modifier.fillMaxWidth()
                                    .onFocusChanged { passFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp) {
                                            when (event.key) {
                                                Key.DirectionDown -> { tfFocusManager.moveFocus(FocusDirection.Down); true }
                                                Key.DirectionUp -> { tfFocusManager.moveFocus(FocusDirection.Up); true }
                                                else -> false
                                            }
                                        } else false
                                    },
                                label = { Text("Password") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (passFocused) Color(0xFF4A90D9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // MAC address field (Stalker only)
                        if (newSourceType == "stalker") {
                            var macFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = newSourceMac, onValueChange = { newSourceMac = it },
                                modifier = Modifier.fillMaxWidth()
                                    .onFocusChanged { macFocused = it.isFocused }
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp) {
                                            when (event.key) {
                                                Key.DirectionDown -> { tfFocusManager.moveFocus(FocusDirection.Down); true }
                                                Key.DirectionUp -> { tfFocusManager.moveFocus(FocusDirection.Up); true }
                                                else -> false
                                            }
                                        } else false
                                    },
                                label = { Text("MAC Address (XX:XX:XX:XX:XX:XX)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (macFocused) Color(0xFF4A90D9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Upload M3U button
                        var uploadFocused by remember { mutableStateOf(false) }
                        Card(
                            onClick = {
                                // Launch file picker via context
                            },
                            colors = CardDefaults.cardColors(containerColor = if (uploadFocused) Color(0xFF2E2E2E) else Color(0xFF0D1B2A)),
                            border = BorderStroke(if (uploadFocused) 2.dp else 1.dp, Color(0xFF4A90D9).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { uploadFocused = it.isFocused }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Upload M3U File", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("from device", color = Color(0xFF888888), fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // Add button
                        var addFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                val finalUrl = when (newSourceType) {
                                    "xtream" -> {
                                        val base = newSourceUrl.trim().trimEnd('/')
                                        val user = newSourceUsername.trim()
                                        val pass = newSourcePassword.trim()
                                        if (base.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                                            "${base}?username=$user&password=$pass"
                                        } else newSourceUrl
                                    }
                                    "stalker" -> if (newSourceMac.isNotBlank()) "${newSourceUrl}|mac=${newSourceMac}" else newSourceUrl
                                    else -> newSourceUrl
                                }
                                if (newSourceName.isNotBlank() && finalUrl.isNotBlank()) {
                                    if (sources.size < 10) viewModel.addIptvSource(newSourceName, finalUrl, newSourceType) else toastMessage = "Source limit reached (10 max)"
                                    newSourceName = ""
                                    newSourceUrl = ""
                                    newSourceMac = ""
                                    newSourceUsername = ""
                                    newSourcePassword = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (addFocused) Color(0xFF4A90D9) else Color.White, contentColor = Color.Black),
                            modifier = Modifier.align(Alignment.End).focusRequester(addFocus)
                                .onFocusChanged { addFocused = it.isFocused }
                                .focusProperties { up = stalkerFocus }
                        ) { Text("Add Source") }
                    }
                }
            }
        }
    } else {
        // Channel Browser
        Column(modifier = Modifier.fillMaxSize()) {
            // Progress banner
            if (progressText.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A2A1A), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF00FF00),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(progressText, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("channels appear as they load", color = Color(0xFF888888), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Header with source name, search toggle, back
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(activeSourceName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                // Search toggle
                var searchFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showSearch = !showSearch; if (!showSearch) viewModel.setIptvSearchQuery("") },
                    modifier = Modifier
                        .onFocusChanged { searchFocused = it.isFocused }
                        .border(
                            width = if (searchFocused) 2.dp else 0.dp,
                            color = if (searchFocused) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Clear else Icons.Default.Search,
                        contentDescription = if (showSearch) "Close Search" else "Search",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Collapsible search field
            AnimatedVisibility(visible = showSearch) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setIptvSearchQuery(it) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { viewModel.setIptvSearchQuery(searchQuery) }),
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setIptvSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Validation progress & dead stream filter
            if (validationMsg != null) {
                Text(validationMsg!!, color = if (validationMsg!!.contains("dead")) Color(0xFFFF6666) else Color(0xFF00FF00), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (deadUrlSet.isNotEmpty()) {
                var filterFocused by remember { mutableStateOf(false) }
                Surface(
                    onClick = { viewModel.toggleShowDeadStreams() },
                    shape = RoundedCornerShape(8.dp),
                    color = if (viewModel.showDeadStreams.value) Color(0xFF4A90D9).copy(alpha = 0.2f) else Color(0xFF1A1A1A),
                    border = BorderStroke(if (filterFocused) 2.dp else 0.dp, if (filterFocused) Color.White else Color.Transparent),
                    modifier = Modifier.height(32.dp).width(200.dp).onFocusChanged { filterFocused = it.isFocused }
                ) {
                    Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (viewModel.showDeadStreams.value) "◉" else "◯", color = Color(0xFF4A90D9), fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Show dead streams (${deadUrlSet.size})", color = Color(0xFFB0B0B0), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (searchQuery.isNotBlank()) {
                val searched = channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    .filter { viewModel.showDeadStreams.value || it.url !in deadUrlSet }
                if (searched.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels match \"$searchQuery\"", color = Color(0xFFB0B0B0))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searched, key = { it.url }) { channel ->
                            val isFav = favorites.any { it.id == channel.id }
                            ChannelGridCard(
                                channel = channel,
                                isFavorite = isFav,
                                onClick = {
                                    channelPopupChannel = channel
                                    channelPopupShow = true
                                },
                                onToggleFavorite = { viewModel.toggleIptvFavorite(channel, !isFav) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val filteredChannels = channels.filter { viewModel.showDeadStreams.value || it.url !in deadUrlSet }
                    val grouped = filteredChannels.groupBy { it.categoryName ?: "Uncategorized" }
                    val sortedGroups = grouped.toList().sortedBy { (cat, _) -> cat }
                    items(sortedGroups, key = { (cat, _) -> "cat_$cat" }) { (category, groupChannels) ->
                        var expanded by remember { mutableStateOf(false) }
                        var headerFocused by remember { mutableStateOf(false) }
                        Column {
                            Surface(
                                onClick = { expanded = !expanded },
                                shape = RoundedCornerShape(8.dp),
                                color = if (headerFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                border = BorderStroke(if (headerFocused) 2.dp else 0.dp, if (headerFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { headerFocused = it.isFocused }
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (expanded) "▾" else "▸", color = Color(0xFF888888), fontSize = 14.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text("${groupChannels.size}", color = Color(0xFF4A90D9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (expanded) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                                ) {
                                    items(groupChannels, key = { it.url }) { channel ->
                                        val isFav = favorites.any { it.id == channel.id }
                                        var chFocused by remember { mutableStateOf(false) }
                                        Card(
                                            onClick = {
                                                channelPopupChannel = channel
                                                channelPopupShow = true
                                            },
                                            colors = CardDefaults.cardColors(containerColor = if (chFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(if (chFocused) 2.dp else 0.dp, if (chFocused) Color.White else Color.Transparent),
                                            modifier = Modifier
                                                .width(150.dp)
                                                .height(100.dp)
                                                .onFocusChanged { chFocused = it.isFocused }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                AsyncImage(model = channel.logoUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                                                Box(Modifier.fillMaxSize().align(Alignment.BottomCenter).height(50.dp)
                                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                                                Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp))
                                                if (isFav) {
                                                    Icon(Icons.Default.Favorite, null, tint = Color.Red, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
        // Channel popup overlay
        if (channelPopupShow && channelPopupChannel != null) {
            val ch = channelPopupChannel!!
            LaunchedEffect(Unit) { popupFocusReq.requestFocus() }
            BackHandler { channelPopupShow = false; channelPopupChannel = null }
            Box(Modifier.fillMaxSize().background(Color(0x66000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { channelPopupShow = false; channelPopupChannel = null }), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(32.dp).width(360.dp)) {
                    Text(ch.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 24.dp))
                    // Add Fav
                    val isFav = favorites.any { it.id == ch.id }
                    var favFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { viewModel.toggleIptvFavorite(ch, !isFav); channelPopupShow = false; channelPopupChannel = null }, shape = RoundedCornerShape(12.dp),
                        color = if (favFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                        border = BorderStroke(if (favFocused) 2.dp else 1.dp, if (favFocused) Color.White else Color(0xFFFF6666).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { favFocused = it.isFocused }
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) Color.Red else Color(0xFFFF6666))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isFav) "★ Remove from Favorites" else "☆ Add to Favorites", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Watch
                    var watchFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { channelPopupShow = false; channelPopupChannel = null; val idx = channels.indexOf(ch).coerceAtLeast(0); IptvPlayerStore.setChannels(channels, idx); onPlayChannel(ch) }, shape = RoundedCornerShape(12.dp),
                        color = if (watchFocused) Color(0xFF1A3A1A) else Color(0xFF0D2614),
                        border = BorderStroke(if (watchFocused) 2.dp else 1.dp, if (watchFocused) Color.White else Color(0xFF00AA00).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { watchFocused = it.isFocused }
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF00FF00))
                            Spacer(Modifier.width(8.dp))
                            Text("▶ Watch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Multi-View
                    var mvFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { channelPopupShow = false; channelPopupChannel = null; addToFirstAvailableSlot(ch) }, shape = RoundedCornerShape(12.dp),
                        color = if (mvFocused) Color(0xFF1A2A3A) else Color(0xFF0D1B2A),
                        border = BorderStroke(if (mvFocused) 2.dp else 1.dp, if (mvFocused) Color.White else Color(0xFF4A90D9).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { mvFocused = it.isFocused }.focusRequester(popupFocusReq)
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF4A90D9))
                            Spacer(Modifier.width(8.dp))
                            Text("⊞ Multi-View", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Cancel
                    var cancelFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { channelPopupShow = false; channelPopupChannel = null }, shape = RoundedCornerShape(12.dp),
                        color = if (cancelFocused) Color(0xFF2E1A1A) else Color(0xFF1A1A1A),
                        border = BorderStroke(if (cancelFocused) 2.dp else 1.dp, if (cancelFocused) Color.White else Color(0xFF444444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(44.dp).onFocusChanged { cancelFocused = it.isFocused }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // Source menu overlay
        if (sourceMenuSource != null) {
            val src = sourceMenuSource!!
            LaunchedEffect(Unit) { sourceMenuFocusReq.requestFocus() }
            BackHandler { sourceMenuSource = null }
            Box(Modifier.fillMaxSize().background(Color(0x66000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { sourceMenuSource = null }), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(32.dp).width(360.dp)) {
                    Text(src.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 24.dp))
                    // View Channels
                    var viewFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { sourceMenuSource = null; viewModel.setActiveIptvSource(src) }, shape = RoundedCornerShape(12.dp),
                        color = if (viewFocused) Color(0xFF1A3A1A) else Color(0xFF0D2614),
                        border = BorderStroke(if (viewFocused) 2.dp else 1.dp, if (viewFocused) Color.White else Color(0xFF00AA00).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { viewFocused = it.isFocused }
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, null, tint = Color(0xFF00FF00))
                            Spacer(Modifier.width(8.dp))
                            Text("View Channels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Refresh
                    var refFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { sourceMenuSource = null; viewModel.refreshIptvSource(src.name, src.url, src.type) }, shape = RoundedCornerShape(12.dp),
                        color = if (refFocused) Color(0xFF1A2A3A) else Color(0xFF0D1B2A),
                        border = BorderStroke(if (refFocused) 2.dp else 1.dp, if (refFocused) Color.White else Color(0xFF4A90D9).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { refFocused = it.isFocused }
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, null, tint = Color(0xFF4A90D9))
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Delete with confirmation
                    var showDelSrcConfirm by remember { mutableStateOf(false) }
                    var delFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { showDelSrcConfirm = true }, shape = RoundedCornerShape(12.dp),
                        color = if (delFocused) Color(0xFF2E1A1A) else Color(0xFF1A1A1A),
                        border = BorderStroke(if (delFocused) 2.dp else 1.dp, if (delFocused) Color.White else Color(0xFFFF6666).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { delFocused = it.isFocused }.focusRequester(sourceMenuFocusReq)
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFFF6666))
                            Spacer(Modifier.width(8.dp))
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    if (showDelSrcConfirm) {
                        val confirmFocus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { confirmFocus.requestFocus() }
                        Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable { showDelSrcConfirm = false }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(32.dp).width(360.dp)) {
                                Text("Delete Source?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Delete \"${src.name}\"?", color = Color(0xFFB0B0B0), fontSize = 14.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(24.dp))
                                var cF by remember { mutableStateOf(false) }
                                Surface(onClick = { showDelSrcConfirm = false; sourceMenuSource = null; viewModel.removeIptvSource(src.url) }, shape = RoundedCornerShape(12.dp),
                                    color = if (cF) Color(0xFF3A1A1A) else Color(0xFF1A1A1A),
                                    border = BorderStroke(if (cF) 2.dp else 1.dp, if (cF) Color.White else Color(0xFFFF6666).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { cF = it.isFocused }.focusRequester(confirmFocus)
                                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Yes, Delete", color = Color(0xFFFF6666), fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
                                Spacer(Modifier.height(12.dp))
                                var cF2 by remember { mutableStateOf(false) }
                                Surface(onClick = { showDelSrcConfirm = false }, shape = RoundedCornerShape(12.dp),
                                    color = if (cF2) Color(0xFF2E2E2E) else Color(0xFF111111),
                                    border = BorderStroke(if (cF2) 2.dp else 1.dp, if (cF2) Color.White else Color(0xFF444444).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { cF2 = it.isFocused }
                                ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Validate
                    var valFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { sourceMenuSource = null; viewModel.setActiveIptvSource(src); viewModel.requestValidation() }, shape = RoundedCornerShape(12.dp),
                        color = if (valFocused) Color(0xFF1A2A3A) else Color(0xFF0D1B2A),
                        border = BorderStroke(if (valFocused) 2.dp else 1.dp, if (valFocused) Color.White else Color(0xFF4A90D9).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { valFocused = it.isFocused }
                    ) {
                        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF4A90D9))
                            Spacer(Modifier.width(8.dp))
                            Text("Validate Streams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Cancel
                    var cancelFocused by remember { mutableStateOf(false) }
                    Surface(onClick = { sourceMenuSource = null }, shape = RoundedCornerShape(12.dp),
                        color = if (cancelFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                        border = BorderStroke(if (cancelFocused) 2.dp else 1.dp, if (cancelFocused) Color.White else Color(0xFF444444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().height(44.dp).onFocusChanged { cancelFocused = it.isFocused }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        // Quick Channel popup overlay
        if (qcShowPopup != null) {
            val (qcName, qcMatches) = qcShowPopup!!
            BackHandler { qcShowPopup = null }
            Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { qcShowPopup = null }), contentAlignment = Alignment.Center) {
                var qcPopupFocus by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White),
                    modifier = Modifier.width(400.dp).heightIn(max = 500.dp).onFocusChanged { qcPopupFocus = it.isFocused }
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(qcName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            var closeFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { qcShowPopup = null }, modifier = Modifier.size(28.dp).onFocusChanged { closeFocused = it.isFocused }) {
                                Icon(Icons.Default.Clear, null, tint = if (closeFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${qcMatches.size} match${if (qcMatches.size != 1) "es" else ""} found", color = Color(0xFF888888), fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))
                        val qcListFocus = remember { FocusRequester() }
                        LaunchedEffect(qcShowPopup) { delay(100); qcListFocus.requestFocus() }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().focusRequester(qcListFocus)) {
                            items(qcMatches, key = { it.url }) { match ->
                                var rowFocused by remember { mutableStateOf(false) }
                                Card(
                                    onClick = {
                                        val idx = allQuickChannels.indexOf(match).coerceAtLeast(0)
                                        IptvPlayerStore.setChannels(allQuickChannels, idx)
                                        qcShowPopup = null
                                        onPlayChannel(match)
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
        // Custom Quick Channel management popup
        if (showCustomQcPopup) {
            BackHandler { showCustomQcPopup = false }
            Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { showCustomQcPopup = false }), contentAlignment = Alignment.Center) {
                var qcPopupFocus by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White),
                    modifier = Modifier.width(450.dp).heightIn(max = 560.dp).onFocusChanged { qcPopupFocus = it.isFocused }
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Manage Custom Quick Channels", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            var closeFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { showCustomQcPopup = false }, modifier = Modifier.size(28.dp).onFocusChanged { closeFocused = it.isFocused }) {
                                Icon(Icons.Default.Clear, null, tint = if (closeFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // Add new custom QC
                        Text("Add New", color = Color(0xFF4A90D9), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        var addName by remember { mutableStateOf("") }
                        var addAliases by remember { mutableStateOf("") }
                        var addRegions by remember { mutableStateOf("") }
                        var addTags by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = addName, onValueChange = { addName = it },
                            label = { Text("Display Name", color = Color(0xFF888888)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF4A90D9), unfocusedBorderColor = Color(0xFF444444)),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = addAliases, onValueChange = { addAliases = it },
                            label = { Text("Aliases (comma separated)", color = Color(0xFF888888)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF4A90D9), unfocusedBorderColor = Color(0xFF444444)),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = addRegions, onValueChange = { addRegions = it },
                            label = { Text("Regions (comma separated)", color = Color(0xFF888888)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF4A90D9), unfocusedBorderColor = Color(0xFF444444)),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = addTags, onValueChange = { addTags = it },
                            label = { Text("Tags (comma separated)", color = Color(0xFF888888)) },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color(0xFF4A90D9), unfocusedBorderColor = Color(0xFF444444)),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        var addBtnFocused by remember { mutableStateOf(false) }
                        Surface(onClick = {
                            if (addName.isNotBlank()) {
                                viewModel.addCustomQuickChannel(
                                    QuickChannel(
                                        displayName = addName.trim(),
                                        aliases = addAliases.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                        regions = addRegions.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                        tags = addTags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                    )
                                )
                                addName = ""; addAliases = ""; addRegions = ""; addTags = ""
                            }
                        }, shape = RoundedCornerShape(6.dp),
                            color = if (addBtnFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                            border = BorderStroke(if (addBtnFocused) 1.dp else 0.dp, if (addBtnFocused) Color.White else Color.Transparent),
                            modifier = Modifier.height(36.dp).onFocusChanged { addBtnFocused = it.isFocused }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("+ Add Custom Quick Channel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Existing Custom (${viewModel.customQuickChannels.value.size})", color = Color(0xFF888888), fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        val customQcList = viewModel.customQuickChannels.value
                        if (customQcList.isEmpty()) {
                            Text("No custom quick channels yet", color = Color(0xFF555555), fontSize = 11.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 200.dp)) {
                                items(customQcList, key = { it.displayName }) { qc ->
                                    var rowFocused by remember { mutableStateOf(false) }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(if (rowFocused) Color(0xFF252525) else Color.Transparent, RoundedCornerShape(4.dp)).onFocusChanged { rowFocused = it.isFocused }) {
                                        Column(Modifier.weight(1f)) {
                                            Text(qc.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (qc.aliases.isNotEmpty()) Text(qc.aliases.joinToString(", "), color = Color(0xFF666666), fontSize = 10.sp)
                                        }
                                        IconButton(onClick = { viewModel.removeCustomQuickChannel(qc) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Clear, null, tint = Color(0xFFAA4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        var clearBtnFocused by remember { mutableStateOf(false) }
                        Surface(onClick = { viewModel.clearCustomQuickChannels() }, shape = RoundedCornerShape(6.dp),
                            color = if (clearBtnFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                            border = BorderStroke(if (clearBtnFocused) 1.dp else 0.dp, if (clearBtnFocused) Color.White else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().height(32.dp).onFocusChanged { clearBtnFocused = it.isFocused }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Clear All Custom", color = Color(0xFFAA4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // Toast overlay
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
private fun ChannelGridCard(
    channel: IptvChannel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(150),
        label = "cardScale"
    )
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) Color.White else Color.Transparent
        ),
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed logo background
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
            )
            // Gradient at bottom for name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            // Channel name at bottom
            Text(
                text = channel.name,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
            // Favorite indicator
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SportsSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val leagues by viewModel.sportsLeagues.collectAsState()
    val events by viewModel.sportsEvents.collectAsState()
    val loading by viewModel.sportsLoading.collectAsState()
    val allLiveEvents by viewModel.allLiveEvents.collectAsState()
    val allUpcomingEvents by viewModel.allUpcomingEvents.collectAsState()
    val allLiveLoading by viewModel.allLiveLoading.collectAsState()
    val selectedEvent by viewModel.selectedSportEvent.collectAsState()
    val matchedChannels by viewModel.matchedChannels.collectAsState()
    val channelsLoading by viewModel.sportsChannelLoading.collectAsState()
    val sportVideos by viewModel.sportEventVideos.collectAsState()
    val sportVideosLoading by viewModel.sportVideosLoading.collectAsState()
    val regionFilter by viewModel.sportRegionFilter.collectAsState()
    val activeTab by viewModel.activeEventTab.collectAsState()
    val daddyLiveEvents by viewModel.daddyLiveEvents.collectAsState()
    val daddyLiveLoading by viewModel.daddyLiveLoading.collectAsState()
    val sync2CalEvents by viewModel.sync2CalEvents.collectAsState()
    val sync2CalTvChannels by viewModel.sync2CalTvChannels.collectAsState()
    val sync2CalLoading by viewModel.sync2CalLoading.collectAsState()
    val selectedTeam by viewModel.selectedTeam.collectAsState()
    var selectedLeague by remember { mutableStateOf<SportLeague?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTvMode = maxWidth >= 1024.dp
        if (isTvMode) {
            LaunchedEffect(Unit) {
                if (sync2CalEvents.isEmpty()) {
                    viewModel.loadSync2CalEvents()
                }
                if (viewModel.externalStreamsMatches.value.isEmpty()) {
                    viewModel.loadExternalStreamsMatches()
                }
            }
            LaunchedEffect(selectedLeague) {
                if (selectedLeague != null) {
                    viewModel.loadStandings(selectedLeague!!)
                    viewModel.loadLeagueHighlights(selectedLeague!!)
                }
            }
            val standings by viewModel.standingsEntries.collectAsState()
            val standingsLoading by viewModel.standingsLoading.collectAsState()
            val sportVideos by viewModel.sportEventVideos.collectAsState()
            val sportVideosLoading by viewModel.sportVideosLoading.collectAsState()
            TvSportsLayout(
                leagues = leagues,
                selectedLeague = selectedLeague,
                onSelectLeague = { league ->
                    viewModel.clearSportSelection()
                    selectedLeague = league
                    if (league != null) viewModel.loadSportsScoreboard(league)
                    else viewModel.loadAllLiveEvents()
                },
                allLiveEvents = allLiveEvents,
                allLiveLoading = allLiveLoading,
                leagueEvents = events,
                standings = standings,
                standingsLoading = standingsLoading,
                highlightVideos = sportVideos.map { it.toHighlightVideo() },
                highlightVideosLoading = sportVideosLoading,
                daddyLiveEvents = daddyLiveEvents,
                onRefresh = {
                    viewModel.loadAllLiveEvents()
                    viewModel.loadDaddyLiveEvents()
                },
                onPlayChannel = onPlayChannel,
                onShowChannels = { _, _ -> },
            )
            return@BoxWithConstraints
        }
    }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // SportNutz home video state
    val sportFilters = listOf("All", "NFL", "NBA", "EPL", "MLB", "NHL", "UFC")
    var sportHomeHeroVideos by remember { mutableStateOf<List<VidNutzVideo>>(emptyList()) }
    var sportHomeFilterVideos by remember { mutableStateOf<Map<String, List<VidNutzVideo>>>(emptyMap()) }
    var sportHomeLoading by remember { mutableStateOf(false) }
    var sportHomeError by remember { mutableStateOf<String?>(null) }
    // Track playing state for videos
    var playingVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (sportHomeFilterVideos.isEmpty()) {
            sportHomeLoading = true
            sportHomeError = null
            try {
                val heroResult = VideoSuggestionEngine.suggest("sports highlights", "Sports", 12)
                sportHomeHeroVideos = heroResult.shuffled().take(5)
                val results = coroutineScope {
                    sportFilters.map { filter ->
                        async {
                            val query = when (filter) {
                                "All" -> "sports highlights compilation"
                                "NFL" -> "NFL highlights 2025"
                                "NBA" -> "NBA highlights 2025"
                                "EPL" -> "Premier League highlights 2025"
                                "MLB" -> "MLB highlights 2025"
                                "NHL" -> "NHL highlights 2025"
                                "UFC" -> "UFC highlights 2025"
                                else -> "sports highlights"
                            }
                            filter to VideoSuggestionEngine.suggest(query, "Sports", 16)
                        }
                    }.awaitAll().toMap()
                }
                sportHomeFilterVideos = results
            } catch (e: Exception) {
                sportHomeError = "Couldn't fetch sports videos"
            }
            sportHomeLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (sync2CalEvents.isEmpty()) {
            viewModel.loadSync2CalEvents()
        }
        if (viewModel.externalStreamsMatches.value.isEmpty()) {
            viewModel.loadExternalStreamsMatches()
        }
    }

    BackHandler {
        if (selectedEvent != null) {
            viewModel.clearSportSelection()
        } else if (selectedLeague != null) {
            selectedLeague = null
        } else {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    }
    FloatingGlassHeader(title = "", onBack = {
        if (selectedEvent != null) {
            viewModel.clearSportSelection()
        } else if (selectedLeague != null) {
            selectedLeague = null
        } else {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    })

    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) events
        else events.filter {
            it.homeTeam.displayName.contains(searchQuery, ignoreCase = true) ||
            it.awayTeam.displayName.contains(searchQuery, ignoreCase = true) ||
            it.leagueAbbreviation.contains(searchQuery, ignoreCase = true)
        }
    }

    fun formatLocalDate(utcDate: String): String {
        return try {
            val instant = java.time.Instant.parse(utcDate)
            val local = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            local.toLocalDateTime().let {
                "${it.monthValue}/${it.dayOfMonth} ${it.hour % 12}:${String.format("%02d", it.minute)}${if (it.hour < 12) "AM" else "PM"}"
            }
        } catch (_: Exception) { utcDate.substringBefore("T") }
    }

    fun formatLocalTime(utcDate: String): String {
        return try {
            val instant = java.time.Instant.parse(utcDate)
            val local = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
            val h = local.hour; val m = local.minute
            "${if (h % 12 == 0) 12 else h % 12}:${String.format("%02d", m)}${if (h < 12) "AM" else "PM"}"
        } catch (_: Exception) { "" }
    }

    LaunchedEffect(selectedLeague) {
        selectedLeague?.let {
            if (it.id == "now") {
                viewModel.loadAllLiveEvents()
                viewModel.loadDaddyLiveEvents()
            } else {
                viewModel.loadSportsScoreboard(it)
                viewModel.startSportsAutoRefresh(it)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSportsAutoRefresh() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // League chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
            items(leagues) { league ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedLeague?.id == league.id
                val bgColor = when { isSelected -> Color.White; isFocused -> Color.White.copy(alpha = 0.15f); else -> Color(0xFF272a2f).copy(alpha = 0.3f) }
                val borderC = when { isSelected -> Color.Transparent; isFocused -> Color.White; else -> Color(0xFF414751).copy(alpha = 0.3f) }
                val txtColor = if (isSelected) Color.Black else Color(0xFFc1c7d2)
                Card(onClick = { viewModel.clearSportSelection(); viewModel.clearTeamSelection(); selectedLeague = league }, colors = CardDefaults.cardColors(containerColor = bgColor, contentColor = txtColor),
                    border = BorderStroke(if (isFocused || !isSelected) 1.dp else 0.dp, borderC), shape = RoundedCornerShape(50),
                    modifier = Modifier.onFocusChanged { isFocused = it.isFocused }.padding(vertical = 4.dp)) {
                    Text(league.name, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val currentLeagueVal = selectedLeague
        if (selectedEvent != null) {
            SportEventDetailPanel(
                event = selectedEvent!!,
                matchedChannels = matchedChannels,
                channelsLoading = channelsLoading,
                videos = sportVideos,
                videosLoading = sportVideosLoading,
                regionFilter = regionFilter,
                activeTab = activeTab,
                standingsEntries = viewModel.standingsEntries.collectAsState().value,
                standingsLoading = viewModel.standingsLoading.collectAsState().value,
                standingsError = viewModel.standingsError.collectAsState().value,
                onBack = { viewModel.clearSportSelection() },
                onPlayChannel = onPlayChannel,
                onSetRegion = { viewModel.setSportRegionFilter(it) },
                onSetTab = { viewModel.setActiveEventTab(it) },
                onSearchVideos = { isFuture -> viewModel.searchSportVideos(selectedEvent!!, isFuture) },
                onPlayHighlight = { event ->
                    scope.launch {
                        val query = "${event.homeTeam.displayName} vs ${event.awayTeam.displayName} highlights"
                        val searchResults = PlatformYouTubeSearch.search(query)
                        searchResults.firstOrNull()?.let { ytVideo ->
                            val result = YouTubeStreamResolver.resolveStreamResult(ytVideo.videoId)
                            result?.let { r ->
                                onPlayChannel(
                                    IptvChannel(
                                        id = ytVideo.videoId,
                                        name = ytVideo.title,
                                        url = r.videoUrl,
                                        logoUrl = ytVideo.thumbnailUrl,
                                        audioUrl = r.audioUrl,
                                        qualities = r.qualities,
                                    )
                                )
                            }
                        }
                    }
                },
                onLoadStandings = {
                    selectedEvent?.let { ev ->
                        val league = viewModel.sportsLeagues.value.find { it.abbreviation == ev.leagueAbbreviation }
                        if (league != null) viewModel.loadStandings(league)
                    }
                },
                onTeamClick = { teamName, teamLogo ->
                    viewModel.selectTeam(teamName, teamLogo)
                }
            )
        } else if (selectedTeam != null) {
            TeamDetailScreen(
                teamName = selectedTeam!!.teamName,
                teamLogo = selectedTeam!!.teamLogo,
                allEvents = if (selectedLeague?.id != null && selectedLeague?.id != "now") events else allLiveEvents + allUpcomingEvents,
                onBack = { viewModel.clearTeamSelection() },
                onPlayChannel = onPlayChannel,
                onEventSelected = { viewModel.selectSportEvent(it) },
            )
        } else if (selectedLeague?.id == "now") {
            var nowTab by remember { mutableStateOf("live") }
            val isNowLoading = allLiveLoading || daddyLiveLoading
            val dlLive = daddyLiveEvents.filter { it.isLive }
            val dlUpcoming = daddyLiveEvents.filter { !it.isLive }
            val matchedChannels by viewModel.matchedChannels.collectAsState()
            var selectedDlEvent by remember { mutableStateOf<com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent?>(null) }
            var showChannelPopup by remember { mutableStateOf(false) }
            
            // New sports API clients
            var ppvStEvents by remember { mutableStateOf<List<com.robbdeeze.nuviotv.data.sports.PpvStEvent>>(emptyList()) }
            var streamedPkEvents by remember { mutableStateOf<List<com.robbdeeze.nuviotv.data.sports.StreamedPkEvent>>(emptyList()) }
            var streamsports99Events by remember { mutableStateOf<List<com.robbdeeze.nuviotv.data.sports.StreamSports99Event>>(emptyList()) }
            val context = androidx.compose.ui.platform.LocalContext.current
            
            fun openExternalUrl(url: String) {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
            
            LaunchedEffect(Unit) {
                launch {
                    ppvStEvents = PpvStClient.fetchEvents()
                }
                launch {
                    streamedPkEvents = StreamedPkClient.fetchEvents()
                }
                launch {
                    streamsports99Events = StreamSports99Client.fetchEvents()
                }
            }

            BackHandler(enabled = showChannelPopup) { showChannelPopup = false; selectedDlEvent = null }

            if (showChannelPopup && selectedDlEvent != null) {
                Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A), contentColor = Color.White), modifier = Modifier.width(500.dp).heightIn(max = 600.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📺 Channels for", color = Color(0xFF888888), fontSize = 12.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(selectedDlEvent!!.eventName.take(40), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                var closeFocused by remember { mutableStateOf(false) }
                                IconButton(onClick = { showChannelPopup = false }, modifier = Modifier.size(28.dp).onFocusChanged { closeFocused = it.isFocused }) {
                                    Icon(Icons.Default.Clear, null, tint = if (closeFocused) Color.White else Color(0xFF666666), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            if (matchedChannels.isNotEmpty()) {
                                Text("✅ Matched ${matchedChannels.size} IPTV channels", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                    items(matchedChannels.take(20)) { mc ->
                                        var mcFocused by remember { mutableStateOf(false) }
                                        Card(onClick = { onPlayChannel(mc.channel) }, colors = CardDefaults.cardColors(containerColor = if (mcFocused) Color(0xFF2E2E2E) else Color(0xFF0D1117)),
                                            border = BorderStroke(if (mcFocused) 2.dp else 0.dp, if (mcFocused) Color.White else Color.Transparent),
                                            modifier = Modifier.fillMaxWidth().onFocusChanged { mcFocused = it.isFocused }
                                        ) {
                                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(mc.channel.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                Text(mc.sourceName, color = Color(0xFF4A90D9), fontSize = 10.sp)
                                                Spacer(Modifier.width(6.dp))
                                                Text(mc.matchType.label, color = Color(0xFF888888), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No IPTV channel matches found", color = Color(0xFFE8553A), fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("Try searching these channels elsewhere:", color = Color(0xFF888888), fontSize = 11.sp)
                                Spacer(Modifier.height(4.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                    items(selectedDlEvent!!.channels) { ch ->
                                        Row(Modifier.padding(vertical = 4.dp)) {
                                            Text("• ${ch.name}", color = Color(0xFFB0B0B0), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isNowLoading && allLiveEvents.isEmpty() && daddyLiveEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning for live & upcoming events...", color = Color(0xFFB0B0B0))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                        listOf("live" to "Live", "upcoming" to "Upcoming").forEach { (key, label) ->
                            var tabFocused by remember { mutableStateOf(false) }
                            Surface(onClick = { nowTab = key }, shape = RoundedCornerShape(20.dp),
                                color = if (nowTab == key) Color(0xFFE8553A) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                border = BorderStroke(if (tabFocused) 2.dp else 0.dp, if (tabFocused) Color.White else Color.Transparent),
                                modifier = Modifier.height(34.dp).onFocusChanged { tabFocused = it.isFocused }
                            ) { Box(Modifier.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) { Text(label, color = if (nowTab == key) Color.White else Color(0xFFc1c7d2), fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("${dlLive.size} live · ${dlUpcoming.size} upcoming", color = Color(0xFF666666), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (nowTab == "live") {
                        val liveCombined = dlLive.take(30)
                        val hasDlLive = liveCombined.isNotEmpty()
                        val hasEspnLive = allLiveEvents.isNotEmpty()
                        if (!hasDlLive && !hasEspnLive) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No live events right now", color = Color.LightGray)
                            }
                        } else if (hasDlLive) {
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(liveCombined, key = { it.id }) { dlEvent ->
                                    var dlFocused by remember { mutableStateOf(false) }
                                    Card(
                                        onClick = {
                                            selectedDlEvent = dlEvent
                                            val chNames = dlEvent.channels.map { it.name }
                                            scope.launch { viewModel.matchSportEventChannels(chNames) }
                                            showChannelPopup = true
                                        },
                                        colors = CardDefaults.cardColors(containerColor = if (dlFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (dlFocused) 2.dp else 0.dp, if (dlFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { dlFocused = it.isFocused }
                                    ) {
                                        Column(Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("LIVE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                                    modifier = Modifier.background(Color(0xFF00FF00).copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(dlEvent.category.take(16), color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Spacer(Modifier.weight(1f))
                                                Text(dlEvent.localTime, color = Color(0xFF888888), fontSize = 10.sp)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(dlEvent.eventName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(4.dp))
                                            val chDisplay = dlEvent.channels.take(3).map { it.name }.joinToString(", ")
                                            Text("📺 $chDisplay", color = Color(0xFF4A90D9), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (dlEvent.channels.size > 3) {
                                                Text("+${dlEvent.channels.size - 3} more", color = Color(0xFF666666), fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(allLiveEvents.take(30), key = { it.id }) { event ->
                                    var cardFocused by remember { mutableStateOf(false) }
                                    val score = "${event.awayScore ?: "-"} - ${event.homeScore ?: "-"}"
                                    Card(
                                        onClick = { viewModel.selectSportEvent(event) },
                                        colors = CardDefaults.cardColors(containerColor = if (cardFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (cardFocused) 2.dp else 0.dp, if (cardFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { cardFocused = it.isFocused }
                                    ) {
                                        Column(Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("LIVE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                                    modifier = Modifier.background(Color(0xFF00FF00).copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(event.leagueAbbreviation, color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Spacer(Modifier.weight(1f))
                                                Text(score, color = if (score != "- -") Color(0xFF4ADE80) else Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
                                                    if (!event.awayTeam.logoUrl.isNullOrBlank()) {
                                                        AsyncImage(model = event.awayTeam.logoUrl, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit)
                                                    } else {
                                                        Text(event.awayTeam.displayName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 9.sp)
                                                    }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(event.awayTeam.displayName, color = Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
                                                    if (!event.homeTeam.logoUrl.isNullOrBlank()) {
                                                        AsyncImage(model = event.homeTeam.logoUrl, contentDescription = null, modifier = Modifier.size(20.dp), contentScale = ContentScale.Fit)
                                                    } else {
                                                        Text(event.homeTeam.displayName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 9.sp)
                                                    }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Text(event.homeTeam.displayName, color = Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // PPV.st live events
                        if (ppvStEvents.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("PPV.st Live", color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(ppvStEvents, key = { it.id }) { ev ->
                                    var f by remember { mutableStateOf(false) }
                                    Card(onClick = { openExternalUrl(ev.streamUrl) }, colors = CardDefaults.cardColors(containerColor = if (f) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (f) 2.dp else 0.dp, if (f) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { f = it.isFocused }) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(ev.category, color = Color(0xFF888888), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Streamed.pk live events
                        if (streamedPkEvents.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Streamed.pk Live", color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(streamedPkEvents, key = { it.id }) { ev ->
                                    var f by remember { mutableStateOf(false) }
                                    Card(onClick = { openExternalUrl(ev.streamUrl) }, colors = CardDefaults.cardColors(containerColor = if (f) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (f) 2.dp else 0.dp, if (f) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { f = it.isFocused }) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(ev.category, color = Color(0xFF888888), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        
                        // StreamSports99.ru live events
                        if (streamsports99Events.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("StreamSports99 Live", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(streamsports99Events, key = { it.id }) { ev ->
                                    var f by remember { mutableStateOf(false) }
                                    Card(onClick = { openExternalUrl(ev.streamUrl) }, colors = CardDefaults.cardColors(containerColor = if (f) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (f) 2.dp else 0.dp, if (f) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { f = it.isFocused }) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(ev.category, color = Color(0xFF888888), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val upcomingDl = dlUpcoming.take(60)
                        val hasDlUpcoming = upcomingDl.isNotEmpty()
                        val hasEspnUpcoming = allUpcomingEvents.isNotEmpty()
                        if (!hasDlUpcoming && !hasEspnUpcoming) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No upcoming events found", color = Color.LightGray)
                            }
                        } else if (hasDlUpcoming) {
                            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(upcomingDl, key = { it.id }) { dlEvent ->
                                    var dlFocused by remember { mutableStateOf(false) }
                                    Card(
                                        onClick = {
                                            selectedDlEvent = dlEvent
                                            val chNames = dlEvent.channels.map { it.name }
                                            scope.launch { viewModel.matchSportEventChannels(chNames) }
                                            showChannelPopup = true
                                        },
                                        colors = CardDefaults.cardColors(containerColor = if (dlFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (dlFocused) 2.dp else 0.dp, if (dlFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { dlFocused = it.isFocused }
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(dlEvent.category.take(16), color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(dlEvent.eventName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(dlEvent.localDate, color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                Spacer(Modifier.width(6.dp))
                                                Text(dlEvent.localTime, color = Color(0xFF888888), fontSize = 10.sp)
                                            }
                                            val chDisplay = dlEvent.channels.take(3).map { it.name }.joinToString(", ")
                                            Text("📺 $chDisplay", color = Color(0xFF4A90D9), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
                                items(allUpcomingEvents.take(30), key = { it.id }) { event ->
                                    var espnFocused by remember { mutableStateOf(false) }
                                    Card(
                                        onClick = { viewModel.selectSportEvent(event) },
                                        colors = CardDefaults.cardColors(containerColor = if (espnFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                        border = BorderStroke(if (espnFocused) 2.dp else 0.dp, if (espnFocused) Color.White else Color.Transparent),
                                        modifier = Modifier.fillMaxWidth().onFocusChanged { espnFocused = it.isFocused }
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(event.leagueAbbreviation, color = Color(0xFFE8553A), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(event.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sync2Cal Upcoming Schedule
            Sync2CalUpcomingSection(
                sync2CalEventsByLeague = sync2CalEvents,
                sync2CalTvChannels = sync2CalTvChannels,
                isLoading = sync2CalLoading,
                onRefresh = { viewModel.loadSync2CalEvents() },
            )

            // External Streams Section
            ExternalStreamsSection(
                matches = viewModel.externalStreamsMatches.value,
                isLoading = viewModel.externalStreamsLoading.value,
                onRefresh = { viewModel.loadExternalStreamsMatches() },
                onStreamClick = { match ->
                    val source = match.sources.firstOrNull()
                    val url = source?.let { ExternalStreamsClient.resolveStreamUrl(it) } ?: ""
                    if (url.isNotEmpty()) {
                        onPlayChannel(IptvChannel(id = url, name = match.title, url = url, logoUrl = match.poster))
                    }
                }
            )
        }
        else if (currentLeagueVal != null) {
            val standingsEntries by viewModel.standingsEntries.collectAsState()
            val standingsLoading by viewModel.standingsLoading.collectAsState()
            val standingsError by viewModel.standingsError.collectAsState()
            LaunchedEffect(currentLeagueVal) {
                if (events.isEmpty()) {
                    viewModel.loadSportsScoreboard(currentLeagueVal)
                }
                if (standingsEntries.isEmpty() && !standingsLoading) {
                    viewModel.loadStandings(currentLeagueVal)
                }
            }
            if (currentLeagueVal.id == "now") {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    // Live events from all leagues
                    if (allLiveEvents.isNotEmpty()) {
                        Text("🔴 Live Now", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        allLiveEvents.take(10).forEach { event ->
                            var nowFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { viewModel.selectSportEvent(event) },
                                colors = CardDefaults.cardColors(containerColor = if (nowFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (nowFocused) 2.dp else 0.dp, if (nowFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { nowFocused = it.isFocused }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF00FF00).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("LIVE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(event.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = Color(0xFFB0B0B0), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(event.leagueAbbreviation, color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    // Sync2cal upcoming across all leagues
                    val nowLeagueIds = listOf("nba", "ufc", "bkfc", "boxing", "mlb", "nhl", "pfl", "powerslap", "nfl", "mls", "epl", "laliga", "seriea", "bundesliga", "ligue1", "ucl", "f1", "tennis", "golf", "cfb", "cbb", "wnba", "soccer")
                    val allSyncNow = nowLeagueIds.flatMap { lid -> sync2CalEvents[lid].orEmpty() }.sortedBy { it.startTime }.take(20)
                    if (allSyncNow.isNotEmpty()) {
                        Text("📅 Upcoming", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        allSyncNow.forEach { ev ->
                            var nowSyncFocused by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (nowSyncFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (nowSyncFocused) 2.dp else 0.dp, if (nowSyncFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { nowSyncFocused = it.isFocused }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val dateFmt = formatEventDate(ev.startTime)
                                        if (dateFmt.isNotBlank()) {
                                            Text(dateFmt, color = Color(0xFF888888), fontSize = 11.sp)
                                        }
                                    }
                                    if (ev.location != null) {
                                        Text(ev.location, color = Color(0xFF666666), fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    // DaddyLive events - only live events from API
                    val liveDaddyEvents = daddyLiveEvents.filter { it.isLive }
                    if (liveDaddyEvents.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📺 Sports Now/Later", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE8553A)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                Text("DADDYLIVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        liveDaddyEvents.take(10).forEach { dl ->
                            var dlFocused by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (dlFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                border = BorderStroke(if (dlFocused) 2.dp else 0.dp, if (dlFocused) Color.White else Color.Transparent),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { dlFocused = it.isFocused }
                            ) {
                                Text(dl.eventName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                Text(
                    "${currentLeagueVal.name} Standings",
                    color = Color(0xFFc1c7d2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )
                if (standingsLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (standingsError != null) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text(standingsError!!, color = Color(0xFFB0B0B0), fontSize = 14.sp)
                    }
                } else if (standingsEntries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        Text("No standings available", color = Color(0xFFB0B0B0))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(standingsEntries, key = { it.team.id }) { entry ->
                            val wins = entry.stats?.find { it.name == "wins" }?.displayValue ?: "-"
                            val losses = entry.stats?.find { it.name == "losses" }?.displayValue ?: "-"
                            var stFocused by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (stFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    if (stFocused) 2.dp else 0.dp,
                                    if (stFocused) Color.White else Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { stFocused = it.isFocused }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = entry.team.logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.team.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text("$wins - $losses", color = Color(0xFF888888), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "${currentLeagueVal.name} Events",
                    color = Color(0xFFc1c7d2),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (events.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No events available", color = Color(0xFFB0B0B0))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(events, key = { it.id }) { event ->
                            var evFocused by remember { mutableStateOf(false) }
                            val evScale by animateFloatAsState(targetValue = if (evFocused) 1.02f else 1f, tween(150), label = "evScale")
                            Card(
                                onClick = { viewModel.selectSportEvent(event) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (evFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)
                                ),
                                border = BorderStroke(
                                    if (evFocused) 2.dp else 0.dp,
                                    if (evFocused) Color.White else Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().onFocusChanged { evFocused = it.isFocused }
                                    .graphicsLayer { scaleX = evScale; scaleY = evScale }
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            event.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${event.awayTeam.displayName} vs ${event.homeTeam.displayName}",
                                            color = Color(0xFFB0B0B0),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val formattedDate = formatEventDate(event.date)
                                        if (formattedDate.isNotBlank()) {
                                            Text(
                                                formattedDate,
                                                color = Color(0xFF888888),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    val score = "${event.awayScore ?: "-"} - ${event.homeScore ?: "-"}"
                                    Text(
                                        score,
                                        color = if (score != "- -") Color(0xFF4ADE80) else Color(0xFF888888),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        else {
            if (sportHomeLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFE8553A), strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Hunting for the latest nutz...", color = Color(0xFF888888), fontSize = 13.sp)
                    }
                }
            } else if (sportHomeError != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(sportHomeError!!, color = Color(0xFFE8553A), fontSize = 14.sp)
                }
            } else {
                LaunchedEffect(Unit) {
                    if (allLiveEvents.isEmpty() && !allLiveLoading) {
                        viewModel.loadAllLiveEvents()
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    // Live events row
                    if (allLiveEvents.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🔴 Live Now", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 0.dp))
                            Text("${allLiveEvents.size} ACTIVE", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            items(allLiveEvents.take(10)) { event ->
                                var liveFocused by remember { mutableStateOf(false) }
                                Card(
                                    onClick = { viewModel.selectSportEvent(event) },
                                    colors = CardDefaults.cardColors(containerColor = if (liveFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                    border = BorderStroke(if (liveFocused) 2.dp else 0.dp, if (liveFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.width(280.dp).onFocusChanged { liveFocused = it.isFocused }
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF00FF00).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                Text("LIVE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(event.leagueAbbreviation, color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(event.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(6.dp))
                                        Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = Color(0xFFB0B0B0), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Upcoming events from sync2cal
                    val nowLeagues = listOf("nba", "ufc", "bkfc", "boxing", "mlb", "nhl", "pfl", "powerslap", "nfl", "mls", "epl", "laliga", "seriea", "bundesliga", "ligue1", "ucl", "f1", "tennis", "golf", "cfb", "cbb", "wnba")
                    val nowMillis = System.currentTimeMillis()
                    val monthMillis = 30L * 24 * 60 * 60 * 1000
                    val syncNowPairs = nowLeagues
                        .flatMap { lid -> sync2CalEvents[lid].orEmpty().map { it to lid } }
                        .mapNotNull { (ev, lid) ->
                            val t = parseIsoMillis(ev.startTime)
                            if (t == null || t < nowMillis || t > nowMillis + monthMillis) null else Triple(ev, lid, t)
                        }
                        .sortedBy { it.third }
                        .map { it.first to it.second }
                        .take(12)
                    if (syncNowPairs.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📅 Upcoming Schedule", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("${syncNowPairs.size} EVENTS", color = Color(0xFF888888), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                            items(syncNowPairs, key = { it.first.id }) { (ev, lid) ->
                                var ucFocused by remember { mutableStateOf(false) }
                                val league = leagues.firstOrNull { it.id == lid }
                                val (awayTeam, homeTeam) = splitEventTeams(ev.title)
                                Card(
                                    onClick = {
                                        viewModel.selectSportEvent(
                                            SportEvent(
                                                id = "sync_${ev.id}", name = ev.title, date = ev.startTime, status = "Scheduled",
                                                homeTeam = SportTeam(id = "", name = homeTeam, displayName = homeTeam),
                                                awayTeam = SportTeam(id = "", name = awayTeam, displayName = awayTeam),
                                                leagueAbbreviation = league?.abbreviation ?: "SPORTS",
                                            )
                                        )
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (ucFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                    border = BorderStroke(if (ucFocused) 2.dp else 0.dp, if (ucFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.width(260.dp).onFocusChanged { ucFocused = it.isFocused }
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(ev.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        val dateFmt = formatEventDate(ev.startTime)
                                        if (dateFmt.isNotBlank()) {
                                            Text(dateFmt, color = Color(0xFF888888), fontSize = 11.sp)
                                        }
                                        if (ev.location != null) {
                                            Text(ev.location, color = Color(0xFF666666), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (league != null) {
                                            Text(league.abbreviation, color = Color(0xFF4A90D9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (homeTeam.isNotBlank() && homeTeam != "Home") {
                                            Text("$awayTeam vs $homeTeam", color = Color(0xFFB0B0B0), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Video grid (Featured Highlights merged with Latest Clips into one list)
                    val allFeaturedVideos = (sportHomeHeroVideos + sportHomeFilterVideos.values.flatten())
                        .distinctBy { it.videoId }
                        .take(16)
                    Text("🔥 Featured Highlights", color = Color(0xFFc1c7d2), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 0.dp))
                    Spacer(Modifier.height(8.dp))
                    if (allFeaturedVideos.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No clips found", color = Color(0xFF666666), fontSize = 14.sp)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 64.dp, vertical = 8.dp)
                        ) {
                            items(allFeaturedVideos, key = { it.videoId }) { video ->
                                var gFocused by remember { mutableStateOf(false) }
                                val gScale by animateFloatAsState(targetValue = if (gFocused) 1.05f else 1f, tween(150), label = "gScale")
                                Card(
                                    onClick = {
                                        playingVideoId = video.videoId
                                        scope.launch {
                                            val result = YouTubeStreamResolver.resolveStreamResult(video.videoId)
                                            result?.let { r ->
                                                onPlayChannel(IptvChannel(id = video.videoId, name = video.title, url = r.videoUrl, logoUrl = video.thumbnailUrl, audioUrl = r.audioUrl, qualities = r.qualities))
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(containerColor = if (gFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                    border = BorderStroke(if (gFocused) 2.dp else 0.dp, if (gFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().height(140.dp).onFocusChanged { gFocused = it.isFocused }
                                        .graphicsLayer { scaleX = gScale; scaleY = gScale }
                                ) {
                                    Box(Modifier.fillMaxSize()) {
                                        AsyncImage(model = video.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117)), contentScale = ContentScale.Crop)
                                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))), RoundedCornerShape(12.dp)))
                                        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                                            Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(video.channelName, color = Color(0xFF888888), fontSize = 10.sp, modifier = Modifier.weight(1f))
                                                Text("▶", color = Color(0xFFE8553A), fontSize = 12.sp)
                                            }
                                        }
                                    }
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
private fun TeamLogoOrPlaceholder(logoUrl: String?, size: Dp = 48.dp) {
    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF272a2f))
        )
    } else {
        Box(Modifier.size(size).clip(CircleShape).background(Color(0xFF272a2f)), contentAlignment = Alignment.Center) {
            Text("?", color = Color(0xFF666666), fontSize = (size.value / 3).sp)
        }
    }
}

@Composable
private fun TeamClickableChip(teamName: String, teamLogo: String?, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
        border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color(0xFFa0caff) else Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
                if (!teamLogo.isNullOrBlank()) {
                    AsyncImage(model = teamLogo, contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                } else {
                    Text(teamName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(teamName, color = if (isFocused) Color.White else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SportEventDetailPanel(
    event: SportEvent,
    matchedChannels: List<MatchedChannel>,
    channelsLoading: Boolean,
    videos: List<SportEventVideo>,
    videosLoading: Boolean,
    regionFilter: String,
    activeTab: EventTab,
    standingsEntries: List<EspnStandingEntry> = emptyList(),
    standingsLoading: Boolean = false,
    standingsError: String? = null,
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onSetRegion: (String) -> Unit,
    onSetTab: (EventTab) -> Unit,
    onSearchVideos: (Boolean) -> Unit,
    onPlayHighlight: (SportEvent) -> Unit,
    onLoadStandings: (() -> Unit)? = null,
    onTeamClick: ((teamName: String, teamLogo: String?) -> Unit)? = null,
) {
    val isLive = event.isLive
    val scope = rememberCoroutineScope()
    val tabs = buildList {
        if (isLive) add(EventTab.LIVE)
        add(EventTab.HIGHLIGHTS)
        if (!isLive) add(EventTab.PRE_MATCH)
        add(EventTab.STANDINGS)
    }

    LaunchedEffect(event) {
        onSetTab(tabs.first())
        if (!isLive) onSearchVideos(false)
        if (isLive) onSetTab(EventTab.LIVE)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
        // Header with event teams (clickable) + status
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamClickableChip(teamName = event.awayTeam.displayName, teamLogo = event.awayTeam.logoUrl, onClick = { onTeamClick?.invoke(event.awayTeam.displayName, event.awayTeam.logoUrl) })
                Spacer(Modifier.width(12.dp))
                Text("VS", color = Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.width(12.dp))
                TeamClickableChip(teamName = event.homeTeam.displayName, teamLogo = event.homeTeam.logoUrl, onClick = { onTeamClick?.invoke(event.homeTeam.displayName, event.homeTeam.logoUrl) })
            }
            Text(event.status.uppercase(), color = Color(0xFFa0caff), fontSize = 12.sp, letterSpacing = 1.sp)
        }

        // Tab chips (Stitch style)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            tabs.forEach { tab ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = tab == activeTab
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) Color.White else Color(0xFF272a2f).copy(alpha = 0.4f))
                        .then(if (isFocused && !isSelected) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier)
                        .clickable { onSetTab(tab); if (tab == EventTab.HIGHLIGHTS || tab == EventTab.PRE_MATCH) onSearchVideos(tab == EventTab.PRE_MATCH) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(tab.label, color = if (isSelected) Color.Black else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tab content
        when (activeTab) {
            EventTab.LIVE -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Channel List (4/12)
                    Column(modifier = Modifier.weight(4f).fillMaxHeight().padding(end = 16.dp)) {
                        Text("Live Broadcasters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL" to "All", "US" to "US", "UK" to "UK", "CA" to "CA").forEach { (id, label) ->
                                var isFocused by remember { mutableStateOf(false) }
                                val isSelected = regionFilter == id
                                Box(
                                    modifier = Modifier
                                        .onFocusChanged { isFocused = it.isFocused }
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFa0caff) else Color(0xFF272a2f))
                                        .then(if (isFocused && !isSelected) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable { onSetRegion(id) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(label, color = if (isSelected) Color(0xFF003259) else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val filtered = if (regionFilter == "ALL") matchedChannels else matchedChannels.filter { it.region == regionFilter || (it.region == "Other" && regionFilter == "US") }
                        ChannelListContent(matchedChannels = filtered, isLoading = channelsLoading, event = event, onPlayChannel = onPlayChannel, onPlayHighlight = onPlayHighlight)
                        // Fallback "Watch Highlights Instead"
                        if (!channelsLoading && filtered.isEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF8e1300).copy(alpha = 0.3f), Color.Transparent)), RoundedCornerShape(16.dp)).padding(20.dp)) {
                                Column {
                                    Text("Broadcaster Offline?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Jump straight into the best moments from this season.", color = Color(0xFFc1c7d2), fontSize = 13.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Watch Highlights Instead", color = Color(0xFFffb4a5), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFffb4a5), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    // Right: Highlights (8/12)
                    Column(modifier = Modifier.weight(8f).fillMaxHeight().padding(start = 16.dp)) {
                        Text("Recent Highlights", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        if (videos.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Tap HIGHLIGHTS to load videos", color = Color(0xFFB0B0B0))
                            }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                                items(videos, key = { it.videoId }) { video ->
                                    VideoCardSmall(video = video, onClick = {
                                        scope.launch {
                                            YouTubeStreamResolver.resolveStreamResult(video.videoId)?.let { r ->
                                                onPlayChannel(IptvChannel(id = video.videoId, name = video.title, url = r.videoUrl, logoUrl = video.thumbnailUrl, audioUrl = r.audioUrl, qualities = r.qualities))
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
            EventTab.STANDINGS -> {
                LaunchedEffect(event) { onLoadStandings?.invoke() }
                Column(Modifier.fillMaxSize()) {
                    if (standingsLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                    } else if (standingsError != null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(standingsError!!, color = Color(0xFFFFB4AB), fontSize = 14.sp)
                                if (standingsError!!.contains("Retry")) {
                                    Spacer(Modifier.height(12.dp))
                                    var retryFocused by remember { mutableStateOf(false) }
                                    Card(onClick = { onLoadStandings?.invoke() }, colors = CardDefaults.cardColors(containerColor = if (retryFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)), modifier = Modifier.onFocusChanged { retryFocused = it.isFocused }) {
                                        Text("Retry", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (standingsEntries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No standings available", color = Color(0xFFB0B0B0)) }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                            items(standingsEntries, key = { it.team.id }) { entry ->
                                val record = entry.stats?.find { it.name == "rank" || it.name == "wins" }?.displayValue ?: ""
                                val wins = entry.stats?.find { it.name == "wins" }?.displayValue ?: "-"
                                val losses = entry.stats?.find { it.name == "losses" }?.displayValue ?: "-"
                                var stFocused by remember { mutableStateOf(false) }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (stFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(if (stFocused) 2.dp else 0.dp, if (stFocused) Color.White else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { stFocused = it.isFocused }
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(model = entry.team.logo, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.team.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("$wins - $losses", color = Color(0xFF888888), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            EventTab.HIGHLIGHTS, EventTab.PRE_MATCH -> {
                if (videosLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White) }
                } else if (videos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No videos found", color = Color(0xFFB0B0B0))
                            Spacer(Modifier.height(8.dp))
                            var retryFocused by remember { mutableStateOf(false) }
                            Card(onClick = { onSearchVideos(activeTab == EventTab.PRE_MATCH) }, colors = CardDefaults.cardColors(containerColor = if (retryFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)), modifier = Modifier.onFocusChanged { retryFocused = it.isFocused }) {
                                Text("Retry", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                        items(videos, key = { it.videoId }) { video ->
                            VideoCardSmall(video = video, onClick = {
                                scope.launch {
                                    YouTubeStreamResolver.resolveStreamResult(video.videoId)?.let { r ->
                                        onPlayChannel(IptvChannel(id = video.videoId, name = video.title, url = r.videoUrl, logoUrl = video.thumbnailUrl, audioUrl = r.audioUrl, qualities = r.qualities))
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCardSmall(video: SportEventVideo, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val vsScale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1f, tween(150), label = "vsScale")
    val ts = RoundedCornerShape(8.dp)
    val scope = rememberCoroutineScope()
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewAudioUrl by remember { mutableStateOf<String?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewEnded by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isFocused) {
        if (isFocused && previewUrl == null && !previewEnded) {
            previewJob?.cancel()
            previewJob = scope.launch {
                val result = try { YouTubeStreamResolver.resolveStreamResult(video.videoId) } catch (_: Exception) { null }
                if (result != null) {
                    previewUrl = result.videoUrl
                    previewAudioUrl = result.audioUrl
                    isPreviewPlaying = true
                    delay(3_000L)
                    isPreviewPlaying = false
                    previewEnded = true
                }
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

    Column(
        modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { isFocused = it.isFocused }.clickable(onClick = onClick)
            .graphicsLayer { scaleX = vsScale; scaleY = vsScale }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(ts).background(Color(0xFF121212))
                .then(if (isFocused) Modifier.border(2.dp, Color.White, ts) else Modifier)
        ) {
            AsyncImage(model = video.thumbnailUrl, video.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (isPreviewPlaying && previewUrl != null) {
                TrailerPlayer(
                    trailerUrl = previewUrl,
                    trailerAudioUrl = previewAudioUrl,
                    isPlaying = true,
                    onEnded = { isPreviewPlaying = false; previewEnded = true },
                    muted = true,
                    cropToFill = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (video.durationSeconds > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(formatDuration(video.durationSeconds), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(32.dp).align(Alignment.Center).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).padding(6.dp))
        }
        Text(video.title, color = Color.White, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
    }
}

@Composable
private fun ChannelListContent(
    matchedChannels: List<MatchedChannel>,
    isLoading: Boolean,
    event: SportEvent,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayHighlight: (SportEvent) -> Unit,
) {
    val scope = rememberCoroutineScope()

    if (isLoading) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    } else if (matchedChannels.isEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No IPTV channels found for this game", color = Color(0xFFB0B0B0), modifier = Modifier.padding(bottom = 12.dp))
            var isFocused by remember { mutableStateOf(false) }
            Card(
                onClick = { onPlayHighlight(event) },
                colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                border = BorderStroke(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x33FFFFFF)),
                modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
            ) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Watch Highlights Instead", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(matchedChannels) { matched ->
                var isFocused by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                        .background(if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                        .clickable { onPlayChannel(matched.channel) }
                        .padding(12.dp)
                ) {
                    AsyncImage(model = matched.channel.logoUrl, null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(Color.DarkGray))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(matched.channel.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row {
                            Text(matched.sourceName, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                            if (matched.region.isNotEmpty() && matched.region != "Other") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("• ${matched.region}", color = Color(0xFF4A90D9), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(matched.matchType.label, color = Color.Black, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun ChannelSelectionPanel(
    event: SportEvent,
    matchedChannels: List<MatchedChannel>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onPlayHighlight: (SportEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with event info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column {
                Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.status, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Match info card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamLogoOrPlaceholder(event.awayTeam.logoUrl)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.awayTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(event.awayScore ?: "-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text("VS", color = Color.LightGray, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TeamLogoOrPlaceholder(event.homeTeam.logoUrl)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(event.homeTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(event.homeScore ?: "-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Channel list
        Text("Available Channels", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.5f), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        } else if (matchedChannels.isEmpty()) {
            // No channels found — offer highlight fallback
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No IPTV channels found for this game", color = Color(0xFFB0B0B0), modifier = Modifier.padding(bottom = 12.dp))
                var isFocused by remember { mutableStateOf(false) }
                Card(
                    onClick = { onPlayHighlight(event) },
                    colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                    border = BorderStroke(if (isFocused) 2.dp else 1.dp, if (isFocused) Color.White else Color(0x33FFFFFF)),
                    modifier = Modifier.onFocusChanged { isFocused = it.isFocused }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Highlights Instead", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matchedChannels) { matched ->
                    var isFocused by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp))
                            .background(if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                            .clickable { onPlayChannel(matched.channel) }
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = matched.channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(matched.channel.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(matched.sourceName, color = Color(0xFFB0B0B0), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = matched.matchType.label,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VidNutzSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val uiState by viewModel.vidNutzUiState.collectAsState()
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showVidNutzSearch by remember { mutableStateOf(false) }
    var vidSearchText by remember { mutableStateOf("") }
    val listState = rememberLazyGridState()
    LaunchedEffect(uiState.searchQuery) { vidSearchText = uiState.searchQuery }

    // Restore scroll position when returning from a video
    LaunchedEffect(uiState.scrollPosition) {
        if (uiState.scrollPosition >= 0) {
            try { listState.scrollToItem(uiState.scrollPosition) } catch (_: Exception) {}
        }
    }

    BackHandler { viewModel.setSubScreen(HubSubScreen.Hub) }
    FloatingGlassHeader(title = "", onBack = { viewModel.setSubScreen(HubSubScreen.Hub) })

    LaunchedEffect(Unit) {
        if (uiState.videos.isEmpty() && uiState.searchQuery.isBlank()) {
            viewModel.loadVidNutzVideos(VidNutzCategory.TRENDING)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {

        // Search row (above pills)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            var searchFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        showVidNutzSearch = false
                        viewModel.loadVidNutzVideos(uiState.selectedCategory)
                    } else {
                        showVidNutzSearch = !showVidNutzSearch
                    }
                },
                modifier = Modifier
                    .onFocusChanged { searchFocused = it.isFocused }
                    .border(
                        width = if (searchFocused) 2.dp else 0.dp,
                        color = if (searchFocused) Color.White else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (uiState.searchQuery.isNotEmpty()) Color(0xFF4A90D9) else Color.LightGray
                )
            }
            AnimatedVisibility(visible = showVidNutzSearch || uiState.searchQuery.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(280.dp)
                        .padding(start = 8.dp)
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = vidSearchText,
                        onValueChange = { q ->
                            vidSearchText = q
                            searchJob?.cancel()
                            if (q.isNotBlank()) {
                                searchJob = scope.launch {
                                    delay(400)
                                    viewModel.searchVidNutz(q)
                                }
                            } else {
                                viewModel.loadVidNutzVideos(uiState.selectedCategory)
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (vidSearchText.isNotBlank()) {
                                viewModel.searchVidNutz(vidSearchText)
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.searchQuery.isNotEmpty() || vidSearchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchJob?.cancel()
                            vidSearchText = ""
                            viewModel.loadVidNutzVideos(uiState.selectedCategory)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(VidNutzCategory.entries.toList()) { category ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = category == uiState.selectedCategory
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isSelected -> Color.White
                                isFocused -> Color.White.copy(alpha = 0.15f)
                                else -> Color(0xFF1A1A1A)
                            }
                        )
                        .then(
                            if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50))
                            else Modifier
                        )
                        .clickable { viewModel.loadVidNutzVideos(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.displayName,
                        color = if (isSelected || isFocused) Color.Black else Color(0xFFB0B0B0),
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val displayVideos = uiState.searchResults ?: uiState.videos

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (displayVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No videos found", color = Color(0xFFB0B0B0))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayVideos) { index, video ->
                    VidNutzVideoCard(
                        video = video,
                        scrollIndex = index,
                        onClick = {
                            viewModel.playVideo(
                                videoId = video.videoId,
                                title = video.title,
                                thumbnail = video.thumbnailUrl,
                                onPlayChannel = onPlayChannel,
                                scrollPosition = index,
                                isInSearchMode = uiState.searchResults != null,
                                searchQuery = uiState.searchQuery,
                                selectedCategory = uiState.selectedCategory,
                            )
                        }
                    )
                }
                if (uiState.hasMore) {
                    item {
                        var lmFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { viewModel.loadMoreVidNutz() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (lmFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            border = BorderStroke(if (lmFocused) 2.dp else 1.dp, if (lmFocused) Color.White else Color(0xFF4A90D9).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(52.dp).onFocusChanged { lmFocused = it.isFocused }
                        ) {
                            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(color = Color(0xFF4A90D9), strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (uiState.isLoadingMore) "Loading..." else "Load More", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VidNutzVideoCard(
    video: VidNutzVideo,
    scrollIndex: Int = 0,
    onClick: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(150),
        label = "cardScale"
    )
    val playAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(200),
        label = "playAlpha"
    )

    // Thumbnail preview state — resolves stream on focus, plays 3 sec confined in thumb
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var previewAudioUrl by remember { mutableStateOf<String?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewEnded by remember { mutableStateOf(false) }
    var previewJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isFocused) {
        if (isFocused && previewUrl == null && !previewEnded) {
            previewJob?.cancel()
            previewJob = coroutineScope.launch {
                val result = try {
                    YouTubeStreamResolver.resolveStreamResult(video.videoId)
                } catch (_: Exception) { null }
                if (result != null) {
                    previewUrl = result.videoUrl
                    previewAudioUrl = result.audioUrl
                    isPreviewPlaying = true
                    // Auto-stop after 3 seconds
                    delay(3_000L)
                    isPreviewPlaying = false
                    previewEnded = true
                }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 16f else 0f
            }
            .clip(RoundedCornerShape(16.dp))
            .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Trailer preview confined to thumbnail (plays behind the static thumb)
            if (isPreviewPlaying && previewUrl != null) {
                TrailerPlayer(
                    trailerUrl = previewUrl,
                    trailerAudioUrl = previewAudioUrl,
                    isPlaying = true,
                    onEnded = {
                        isPreviewPlaying = false
                        previewEnded = true
                    },
                    muted = true,
                    cropToFill = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Play overlay (hidden by default, shows on focus)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(playAlpha)
                    .background(Color.Black.copy(alpha = 0.2f))
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .alpha(playAlpha)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp).align(Alignment.Center)
                )
            }
            // Duration badge
            if (video.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        FocusMarqueeText(
            text = video.title,
            focused = isFocused,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            modifier = Modifier.padding(top = 8.dp, start = 2.dp)
        )
        if (video.viewCount > 0 || video.uploadDate.isNotEmpty()) {
            Text(
                text = buildString {
                    if (video.viewCount > 0) append(formatViews(video.viewCount))
                    if (video.uploadDate.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(video.uploadDate)
                    }
                },
                color = Color(0xFF888888),
                fontSize = 11.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 1.dp, start = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) {
        "${if (hours < 10) "0$hours" else "$hours"}:${if (minutes < 10) "0$minutes" else "$minutes"}:${if (secs < 10) "0$secs" else "$secs"}"
    } else {
        "${if (minutes < 10) "0$minutes" else "$minutes"}:${if (secs < 10) "0$secs" else "$secs"}"
    }
}

private fun formatViews(count: Long): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M views"
    count >= 1_000 -> "${count / 1_000}K views"
    else -> "$count views"
}

@Composable
fun MusicNutzSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val uiState by viewModel.musicNutzUiState.collectAsState()
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showMusicSearch by remember { mutableStateOf(false) }
    BackHandler {
        if (uiState.selectedAlbum != null) {
            viewModel.dismissAlbumDetail()
        } else {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.tracks.isEmpty() && uiState.searchQuery.isBlank()) {
            viewModel.loadMusicNutzTracks(MusicNutzCategory.TRENDING)
        }
    }

    // Album detail view
    if (uiState.selectedAlbum != null) {
        AlbumDetailView(
            album = uiState.selectedAlbum!!,
            tracks = uiState.albumTracks,
            isLoading = uiState.isLoadingAlbumTracks,
            onBack = { viewModel.dismissAlbumDetail() },
            onPlayTrack = { track ->
                scope.launch {
                    val youtubeId = viewModel.resolveMusicTrackYoutubeId(track)
                    youtubeId?.let { yId ->
                        val result = YouTubeStreamResolver.resolveStreamResult(yId)
                        result?.let { r ->
                            onPlayChannel(
                                IptvChannel(
                                    id = yId,
                                    name = "${track.artistName} - ${track.title}",
                                    url = r.videoUrl,
                                    logoUrl = track.albumCover,
                                    audioUrl = r.audioUrl,
                                    qualities = r.qualities,
                                )
                            )
                        }
                    }
                }
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search toggle
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            var searchFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = { showMusicSearch = !showMusicSearch },
                modifier = Modifier
                    .onFocusChanged { searchFocused = it.isFocused }
                    .border(
                        width = if (searchFocused) 2.dp else 0.dp,
                        color = if (searchFocused) Color.White else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (uiState.searchQuery.isNotEmpty()) Color(0xFF4A90D9) else Color.LightGray
                )
            }
        }
        AnimatedVisibility(visible = showMusicSearch || uiState.searchQuery.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { q ->
                        searchJob?.cancel()
                        if (q.isNotBlank()) {
                            searchJob = scope.launch {
                                delay(400)
                                if (uiState.mode == MusicNutzMode.TRACKS) {
                                    viewModel.searchMusicNutzTracks(q)
                                } else {
                                    viewModel.searchMusicNutzAlbums(q)
                                }
                            }
                        } else {
                        if (uiState.mode == MusicNutzMode.TRACKS) {
                            viewModel.loadMusicNutzTracks(uiState.selectedCategory)
                        } else {
                            viewModel.loadMusicNutzAlbums(uiState.selectedCategory)
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (uiState.searchQuery.isNotBlank()) {
                        if (uiState.mode == MusicNutzMode.TRACKS) {
                            viewModel.searchMusicNutzTracks(uiState.searchQuery)
                        } else {
                            viewModel.searchMusicNutzAlbums(uiState.searchQuery)
                        }
                    }
                }),
                modifier = Modifier.weight(1f)
            )
            if (uiState.searchQuery.isNotEmpty()) {
                IconButton(onClick = {
                    searchJob?.cancel()
                    if (uiState.mode == MusicNutzMode.TRACKS) {
                        viewModel.loadMusicNutzTracks(uiState.selectedCategory)
                    } else {
                        viewModel.loadMusicNutzAlbums(uiState.selectedCategory)
                    }
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                }
            }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Genre Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(MusicNutzCategory.entries.toList()) { category ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = category == uiState.selectedCategory
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isSelected -> Color.White
                                isFocused -> Color.White.copy(alpha = 0.15f)
                                else -> Color(0xFF1A1A1A)
                            }
                        )
                        .then(
                            if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50))
                            else Modifier
                        )
                        .clickable {
                            if (uiState.mode == MusicNutzMode.TRACKS) {
                                viewModel.loadMusicNutzTracks(category)
                            } else {
                                viewModel.loadMusicNutzAlbums(category)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category.displayName,
                        color = if (isSelected || isFocused) Color.Black else Color(0xFFB0B0B0),
                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Toggle (Tracks / Albums / Playlists / Downloads / Saved)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item { MusicModeChip("Tracks", uiState.mode == MusicNutzMode.TRACKS, { viewModel.setMusicNutzMode(MusicNutzMode.TRACKS) }) }
            item { MusicModeChip("Albums", uiState.mode == MusicNutzMode.ALBUMS, { viewModel.setMusicNutzMode(MusicNutzMode.ALBUMS) }) }
            item { MusicModeChip("Playlists", uiState.mode == MusicNutzMode.PLAYLISTS, { viewModel.setMusicNutzMode(MusicNutzMode.PLAYLISTS) }) }
            item { MusicModeChip("Saved", uiState.mode == MusicNutzMode.SAVED, { viewModel.setMusicNutzMode(MusicNutzMode.SAVED) }) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (uiState.mode) {
            MusicNutzMode.TRACKS -> TrackContent(uiState, viewModel, onPlayChannel)
            MusicNutzMode.ALBUMS -> AlbumContent(uiState, viewModel, onPlayChannel)
            MusicNutzMode.PLAYLISTS -> PlaylistContent(uiState, viewModel, onPlayChannel)
            MusicNutzMode.DOWNLOADS -> {}
            MusicNutzMode.SAVED -> SavedContent(uiState, viewModel, onPlayChannel)
        }
    }

    // Add to Playlist dialog overlay
    if (uiState.showAddToPlaylistDialog && uiState.pendingTrackForPlaylist != null) {
        val track = uiState.pendingTrackForPlaylist!!
        BackHandler { viewModel.showAddToPlaylistDialog(false) }
        Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { viewModel.showAddToPlaylistDialog(false) }), contentAlignment = Alignment.Center) {
            Box(Modifier.width(450.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("${track.artistName} - ${track.title}", color = Color(0xFF888888), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
                    if (uiState.playlists.isEmpty()) {
                        Text("No playlists yet. Create one first!", color = Color(0xFFFFAA00), fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                            items(uiState.playlists, key = { it.id }) { pl ->
                                var plFocused by remember { mutableStateOf(false) }
                                Row(Modifier.fillMaxWidth().background(if (plFocused) Color(0xFF2E2E2E) else Color(0xFF111111), RoundedCornerShape(8.dp)).onFocusChanged { plFocused = it.isFocused }.border(if (plFocused) 2.dp else 0.dp, if (plFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp)).clickable { viewModel.addTrackToPlaylist(pl.id, track) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                    Text("${pl.tracks.size} tracks", color = Color(0xFF888888), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    var closeFocused by remember { mutableStateOf(false) }
                    Button(onClick = { viewModel.showAddToPlaylistDialog(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (closeFocused) Color(0xFF2E2E2E) else Color(0xFF111111), contentColor = Color.White),
                        modifier = Modifier.height(44.dp).onFocusChanged { closeFocused = it.isFocused }) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun MusicModeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isSelected -> Color.White
                                isFocused -> Color.White.copy(alpha = 0.15f)
                                else -> Color(0xFF272a2f).copy(alpha = 0.4f)
                            }
                        )
            .then(
                if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected || isFocused) Color.Black else Color(0xFFB0B0B0),
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun TrackContent(
    uiState: MusicNutzUiState,
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val scope = rememberCoroutineScope()
    val display = uiState.searchResults ?: uiState.tracks

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (display.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tracks found", color = Color(0xFFB0B0B0))
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(display, key = { "t_${it.id}" }) { track ->
                TrackCard(
                    track = track,
                    onClick = {
                        viewModel.playMusicTrack(track, onPlayChannel)
                    },
                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(true, track) },
                    onDownload = { viewModel.downloadTrack(track) },
                    isDownloading = track.id in uiState.downloadingTrackIds,
                    isSaved = track.id in uiState.downloadingTrackIds, // placeholder
                )
            }
        }
    }
}

@Composable
private fun AlbumContent(
    uiState: MusicNutzUiState,
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val scope = rememberCoroutineScope()
    val display = uiState.albumResults ?: uiState.albums

    if (uiState.isLoadingAlbums) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    } else if (display.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No albums found", color = Color(0xFFB0B0B0))
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(display, key = { "a_${it.id}" }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { viewModel.selectAlbum(album) },
                    onToggleSave = { viewModel.toggleSavedAlbum(album.id) },
                    isSaved = album.id in uiState.savedAlbumIds,
                )
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: MusicTrack,
    onClick: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    isDownloading: Boolean = false,
    isSaved: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(150),
        label = "cardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
        ) {
            AsyncImage(
                model = track.albumCover,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
                modifier = Modifier
                    .size(if (isFocused) 44.dp else 36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isFocused) 0.5f else 0.3f))
                    .padding(8.dp)
            )
            if (track.durationSeconds > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = musicDuration(track.durationSeconds),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Action buttons on focus (top-right)
            if (isFocused) {
                Column(Modifier.align(Alignment.TopEnd).padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onAddToPlaylist != null) {
                        var plFocused by remember { mutableStateOf(false) }
                        IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(28.dp).background(Color(0x99000000), CircleShape).onFocusChanged { plFocused = it.isFocused }) {
                            Icon(Icons.Default.Add, "Add to Playlist", tint = if (plFocused) Color(0xFF00CEC9) else Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp)
        )
        Text(
            text = track.artistName,
            color = Color(0xFF888888),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp)
        )
        if (track.albumName.isNotBlank()) {
            Text(
                text = track.albumName,
                color = Color(0xFF888888),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun AlbumCard(
    album: MusicAlbum,
    onClick: () -> Unit,
    onToggleSave: (() -> Unit)? = null,
    isSaved: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val thumbShape = RoundedCornerShape(12.dp)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(150),
        label = "cardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
        ) {
            AsyncImage(
                model = album.coverUrl,
                contentDescription = album.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "View",
                tint = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
                modifier = Modifier
                    .size(if (isFocused) 44.dp else 36.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isFocused) 0.5f else 0.3f))
                    .padding(8.dp)
            )
            // Save/bookmark icon
            if (onToggleSave != null) {
                var saveFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color(0x99000000), CircleShape)
                        .onFocusChanged { saveFocused = it.isFocused }
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isSaved) "Unsave" else "Save",
                        tint = if (isSaved) Color(0xFFFF6B6B) else if (saveFocused) Color.White else Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = album.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp)
        )
        Text(
            text = album.artistName,
            color = Color(0xFF888888),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp)
        )
        if (album.releaseDate.isNotBlank()) {
            Text(
                text = album.releaseDate.take(4),
                color = Color(0xFF888888),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun AlbumDetailView(
    album: MusicAlbum,
    tracks: List<MusicTrack>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (MusicTrack) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = album.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "__album_header__") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    var artFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .focusable()
                            .onFocusChanged { artFocused = it.isFocused }
                            .clip(RoundedCornerShape(16.dp))
                            .then(if (artFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(16.dp)) else Modifier)
                    ) {
                        AsyncImage(
                            model = album.coverUrl,
                            contentDescription = album.title,
                            modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = album.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artistName,
                        color = Color(0xFFB0B0B0),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (album.releaseDate.isNotBlank()) {
                        Text(
                            text = album.releaseDate.take(4),
                            color = Color(0xFF888888),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        Text(
                            text = "${tracks.size} tracks",
                            color = Color(0xFF888888),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.5f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            items(tracks, key = { "at_${it.id}" }) { track ->
                AlbumTrackRow(track = track, onPlay = { onPlayTrack(track) })
            }
        }
    }
}

@Composable
private fun AlbumTrackRow(track: MusicTrack, onPlay: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) Color(0xFF2E2E2E) else Color.Transparent)
            .then(
                if (isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                else Modifier
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onPlay)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = if (isFocused) Color.White else Color(0xFFB0B0B0),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                color = Color(0xFF888888),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = musicDuration(track.durationSeconds),
            color = Color(0xFF888888),
            fontSize = 11.sp
        )
    }
}

private fun musicDuration(sec: Int): String {
    return "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"
}

@Composable
private fun PlaylistContent(
    uiState: MusicNutzUiState,
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val scope = rememberCoroutineScope()
    val createFocusReq = remember { FocusRequester() }

    if (uiState.showCreatePlaylistDialog) {
        LaunchedEffect(Unit) { createFocusReq.requestFocus() }
        BackHandler { viewModel.showCreatePlaylistDialog(false) }
        Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { viewModel.showCreatePlaylistDialog(false) }), contentAlignment = Alignment.Center) {
            Box(Modifier.width(450.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create Playlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
                    var nameFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.newPlaylistName,
                        onValueChange = { viewModel.setNewPlaylistName(it) },
                        modifier = Modifier.fillMaxWidth().focusRequester(createFocusReq).onFocusChanged { nameFocused = it.isFocused },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = if (nameFocused) Color(0xFF00CEC9) else Color.White, unfocusedBorderColor = Color(0xFF666666), focusedLabelColor = Color.White),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        var createFocused by remember { mutableStateOf(false) }
                        Button(onClick = { viewModel.createPlaylist(uiState.newPlaylistName) }, enabled = uiState.newPlaylistName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = if (createFocused) Color(0xFF00CEC9) else Color(0xFF00CEC9), contentColor = Color.Black, disabledContainerColor = Color(0xFF333333)),
                            modifier = Modifier.height(44.dp).onFocusChanged { createFocused = it.isFocused }) { Text("Create", fontWeight = FontWeight.Bold) }
                        var cancelFocused by remember { mutableStateOf(false) }
                        Button(onClick = { viewModel.showCreatePlaylistDialog(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (cancelFocused) Color(0xFF2E2E2E) else Color(0xFF111111), contentColor = Color.White),
                            modifier = Modifier.height(44.dp).onFocusChanged { cancelFocused = it.isFocused }) { Text("Cancel") }
                    }
                }
            }
        }
        return
    }

    if (uiState.selectedPlaylist != null) {
        val pl = uiState.selectedPlaylist!!
        BackHandler { viewModel.selectPlaylist(null) }
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.weight(1f))
                Text("${pl.tracks.size} tracks", color = Color(0xFF888888), fontSize = 14.sp)
            }
            if (pl.tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No tracks in this playlist", color = Color(0xFF666666)) }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
                    items(pl.tracks, key = { "plt_${it.id}" }) { track ->
                        var trackFocused by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth().background(if (trackFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).onFocusChanged { trackFocused = it.isFocused }.border(if (trackFocused) 2.dp else 0.dp, if (trackFocused) Color.White else Color.Transparent, RoundedCornerShape(8.dp)).clickable {
                            viewModel.playMusicTrack(track, onPlayChannel)
                        }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artistName, color = Color(0xFF888888), fontSize = 12.sp)
                            }
                            var delFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { viewModel.removeTrackFromPlaylist(pl.id, track.id) }, modifier = Modifier.size(32.dp).onFocusChanged { delFocused = it.isFocused }.border(if (delFocused) 2.dp else 0.dp, if (delFocused) Color.White else Color.Transparent, CircleShape)) {
                                Icon(Icons.Default.Delete, "Remove", tint = if (delFocused) Color(0xFFFFB4AB) else Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("My Playlists", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            var createBtnFocused by remember { mutableStateOf(false) }
            Button(onClick = { viewModel.showCreatePlaylistDialog(true) },
                colors = ButtonDefaults.buttonColors(containerColor = if (createBtnFocused) Color(0xFF00CEC9) else Color(0xFF00CEC9), contentColor = Color.Black),
                modifier = Modifier.height(40.dp).onFocusChanged { createBtnFocused = it.isFocused }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("New Playlist", fontWeight = FontWeight.Bold)
            }
        }
        if (uiState.playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No playlists yet. Create one!", color = Color(0xFF666666)) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.playlists, key = { it.id }) { pl ->
                    var plFocused by remember { mutableStateOf(false) }
                    val plScale by animateFloatAsState(if (plFocused) 1.08f else 1f, tween(150), label = "plScale")
                    Card(onClick = { viewModel.selectPlaylist(pl) },
                        colors = CardDefaults.cardColors(containerColor = if (plFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(if (plFocused) 2.dp else 0.dp, if (plFocused) Color.White else Color.Transparent),
                        modifier = Modifier.fillMaxWidth().height(120.dp).onFocusChanged { plFocused = it.isFocused }.graphicsLayer { scaleX = plScale; scaleY = plScale }
                    ) {
                        Box(Modifier.fillMaxSize().padding(16.dp)) {
                            Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${pl.tracks.size} tracks", color = Color(0xFF888888), fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart))
                            var delPlFocused by remember { mutableStateOf(false) }
                            IconButton(onClick = { viewModel.deletePlaylist(pl.id) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp).onFocusChanged { delPlFocused = it.isFocused }) {
                                Icon(Icons.Default.Delete, "Delete", tint = if (delPlFocused) Color(0xFFFFB4AB) else Color(0xFF666666), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsContent(
    uiState: MusicNutzUiState,
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (uiState.downloadedTracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No downloads", color = Color(0xFF888888), fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("Download tracks from the Tracks tab", color = Color(0xFF555555), fontSize = 14.sp)
            }
        }
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
            items(uiState.downloadedTracks, key = { "dl_${it.trackId}" }) { dl ->
                var dlFocused by remember { mutableStateOf(false) }
                val dlScale by animateFloatAsState(if (dlFocused) 1.08f else 1f, tween(150), label = "dlScale")
                Card(onClick = {
                    scope.launch {
                        val url = YouTubeStreamResolver.resolveStreamUrl("") // placeholder — in production use local file
                        url?.let { u -> onPlayChannel(IptvChannel(id = dl.localPath, name = "${dl.artistName} - ${dl.title}", url = u, logoUrl = dl.albumCover)) }
                    }
                }, colors = CardDefaults.cardColors(containerColor = if (dlFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(if (dlFocused) 2.dp else 0.dp, if (dlFocused) Color.White else Color.Transparent),
                    modifier = Modifier.fillMaxWidth().height(180.dp).onFocusChanged { dlFocused = it.isFocused }.graphicsLayer { scaleX = dlScale; scaleY = dlScale }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(model = dl.albumCover, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(dl.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dl.artistName, color = Color(0xFFB0B0B0), fontSize = 12.sp)
                            Text("Downloaded", color = Color(0xFF00CEC9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        var delDlFocused by remember { mutableStateOf(false) }
                        IconButton(onClick = { viewModel.deleteDownloadedTrack(dl.trackId) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp).onFocusChanged { delDlFocused = it.isFocused }) {
                            Icon(Icons.Default.Delete, "Delete", tint = if (delDlFocused) Color(0xFFFFB4AB) else Color(0xFF666666), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedContent(
    uiState: MusicNutzUiState,
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedAlbums = uiState.albums.filter { it.id in uiState.savedAlbumIds }

    if (savedAlbums.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No saved albums", color = Color(0xFF888888), fontSize = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text("Bookmark albums from the Albums tab", color = Color(0xFF555555), fontSize = 14.sp)
            }
        }
    } else {
        LazyVerticalGrid(columns = GridCells.Fixed(4), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
            items(savedAlbums, key = { "sv_${it.id}" }) { album ->
                var svFocused by remember { mutableStateOf(false) }
                val svScale by animateFloatAsState(if (svFocused) 1.08f else 1f, tween(150), label = "svScale")
                Card(onClick = { viewModel.selectAlbum(album) },
                    colors = CardDefaults.cardColors(containerColor = if (svFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp), border = BorderStroke(if (svFocused) 2.dp else 0.dp, if (svFocused) Color.White else Color.Transparent),
                    modifier = Modifier.fillMaxWidth().height(200.dp).onFocusChanged { svFocused = it.isFocused }.graphicsLayer { scaleX = svScale; scaleY = svScale }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AsyncImage(model = album.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                            Text(album.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(album.artistName, color = Color(0xFFB0B0B0), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.Favorite, "Saved", tint = Color(0xFFFF6B6B), modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalStreamsSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val matches by viewModel.externalStreamsMatches.collectAsState()
    val loading by viewModel.externalStreamsLoading.collectAsState()
    val error by viewModel.externalStreamsError.collectAsState()
    val selectedCategory by viewModel.externalStreamsSelectedCategory.collectAsState()
    val categories = listOf("football", "basketball", "baseball", "hockey", "tennis", "boxing", "ufc", "nfl", "nba", "mlb", "nhl", "soccer")

    LaunchedEffect(Unit) {
        viewModel.loadExternalStreamsMatches()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("External Streams", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    var f by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { viewModel.setExternalStreamsCategory(cat) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedCategory == cat) Color(0xFF4A90D9) else if (f) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                        border = BorderStroke(if (f) 2.dp else 0.dp, if (f) Color.White else Color.Transparent),
                        modifier = Modifier.height(32.dp).onFocusChanged { f = it.isFocused }
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text(cat.replaceFirstChar { it.uppercase() }, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (loading) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF4A90D9), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Loading...", color = Color(0xFF888888), fontSize = 12.sp)
                }
            }
        }
        if (error != null) {
            item {
                Text(error!!, color = Color(0xFFE8553A), fontSize = 12.sp)
            }
        }
        if (!loading && matches.isEmpty() && error == null) {
            item {
                Text("No matches found for $selectedCategory", color = Color(0xFF888888), fontSize = 14.sp)
            }
        }
        items(matches) { match ->
            var f by remember { mutableStateOf(false) }
            Card(
                onClick = {
                    val source = match.sources.firstOrNull()
                    val channel = IptvChannel(
                        id = source?.id ?: match.id,
                        name = match.title,
                        url = source?.let { ExternalStreamsClient.resolveStreamUrl(it) } ?: "",
                        logoUrl = match.poster
                    )
                    onPlayChannel(channel)
                },
                colors = CardDefaults.cardColors(containerColor = if (f) Color(0xFF2E2E2E) else Color(0xFF1A1A1A), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(if (f) 2.dp else 0.dp, if (f) Color.White else Color.Transparent),
                modifier = Modifier.fillMaxWidth().onFocusChanged { f = it.isFocused }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(model = match.poster, contentDescription = null, modifier = Modifier.width(80.dp).height(45.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(match.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(match.category, color = Color(0xFFB0B0B0), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MultiWindowSubScreen(allChannels: List<IptvChannel> = emptyList(), favoriteChannels: List<IptvChannel> = emptyList(), historyChannels: List<IptvChannel> = emptyList(), iptvSources: List<IptvSource> = emptyList(), onPlayChannel: (IptvChannel) -> Unit = {}, onLoadIptvChannels: ((IptvSource) -> Unit)? = null, onNavigateToIptv: () -> Unit = {}) {
    var ss by remember { mutableStateOf(false) }; var pc by remember { mutableStateOf<IptvChannel?>(null) }; var sco by remember { mutableStateOf(false) }; var csi by remember { mutableStateOf("") }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        MultiWindowPushStore.pendingChannel?.let { ch ->
            pc = ch
            ss = true
            MultiWindowPushStore.pendingChannel = null
        }
    }
    LaunchedEffect(Unit) {
        val slotIdx = IptvPlayerStore.launchedFromSlotIndex
        if (slotIdx >= 0) {
            val newChannel = IptvPlayerStore.currentChannel()
            if (newChannel != null) {
                val existing = MultiWindowStore.streams.find { it.slotIndex == slotIdx }
                if (existing != null && existing.channel.url != newChannel.url) {
                    MultiWindowStore.addToSlot(newChannel, slotIdx, existing.id)
                }
            }
            IptvPlayerStore.launchedFromSlotIndex = -1
        }
    }
    Box(Modifier.fillMaxSize()) {
        MultiWindowGrid(onBrowseChannels = onNavigateToIptv, onOpenSlotPicker = { c -> pc = c; ss = true }, onOpenCellOptions = { s -> csi = s; sco = true }, onPlayChannel = onPlayChannel)
        if (ss && pc != null) { val sfr = remember { FocusRequester() }; BackHandler { ss = false }; Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().onFocusChanged { if (it.isFocused) sfr.requestFocus() }.clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { ss = false }), contentAlignment = Alignment.CenterEnd) { Box(Modifier.fillMaxHeight().width(300.dp).background(Color(0xFF0D1117).copy(alpha = 0.95f)).padding(16.dp)) { Column { var cf by remember { mutableStateOf(false) }; val cs by animateFloatAsState(if (cf) 1.08f else 1f, tween(150), label = "cs"); IconButton(onClick = { ss = false }, modifier = Modifier.align(Alignment.End).focusRequester(sfr).focusable().onFocusChanged { cf = it.isFocused }.graphicsLayer { scaleX = cs; scaleY = cs }.border(if (cf) 2.dp else 0.dp, if (cf) Color.White else Color.Transparent, CircleShape)) { Icon(Icons.Default.Clear, "Close", tint = Color.White) }; Spacer(Modifier.height(8.dp)); MultiWindowSlotPicker(channel = pc!!, onDismiss = { pc = null; ss = false }) } } } }
        if (sco) { BackHandler { sco = false }; Box(Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null, onClick = { sco = false }), contentAlignment = Alignment.CenterEnd) { Box(Modifier.fillMaxHeight().width(320.dp).background(Color(0xFF0D1117).copy(alpha = 0.95f)).padding(16.dp)) { Column { var cf by remember { mutableStateOf(false) }; val cs by animateFloatAsState(if (cf) 1.08f else 1f, tween(150), label = "cs2"); IconButton(onClick = { sco = false }, modifier = Modifier.align(Alignment.End).onFocusChanged { cf = it.isFocused }.graphicsLayer { scaleX = cs; scaleY = cs }.border(if (cf) 2.dp else 0.dp, if (cf) Color.White else Color.Transparent, CircleShape)) { Icon(Icons.Default.Clear, "Close", tint = Color.White) }; Spacer(Modifier.height(8.dp)); MultiWindowCellOptions(streamId = csi, onDismiss = { sco = false }, allChannels = allChannels, historyChannels = historyChannels, favoriteChannels = favoriteChannels, onPlayChannel = onPlayChannel) } } } }
    }
}

@Composable
fun MagNutzSubScreen(
    viewModel: RobbdeezeNutzHubViewModel,
    onPlayChannel: (IptvChannel) -> Unit,
) {
    val uiState by viewModel.magNutzUiState.collectAsState()
    val torrents by viewModel.magNutzTorrents.collectAsState()
    val addDialogFocusReq = remember { FocusRequester() }
    val deleteConfirmFocusReq = remember { FocusRequester() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pending = RobbdeezeNutzHubViewModel.pendingMagnetUri
        if (pending != null) {
            RobbdeezeNutzHubViewModel.pendingMagnetUri = null
            viewModel.showMagNutzAddDialog(true)
            viewModel.setMagNutzMagnetInput(pending)
        }
    }

    val filteredTorrents = remember(torrents, uiState.filter) {
        if (uiState.filter == null) torrents
        else torrents.filter { it.status == uiState.filter }
    }

    LaunchedEffect(Unit) {
        viewModel.initMagNutz()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.disposeMagNutz() }
    }

    BackHandler {
        if (uiState.selectedTorrent != null) {
            viewModel.selectTorrent(null)
        } else if (uiState.showAddDialog) {
            viewModel.showMagNutzAddDialog(false)
        } else {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF131313))) {
        Column(modifier = Modifier.fillMaxSize()) {
            FloatingGlassHeader(
                title = "MagNutz",
                onBack = { viewModel.setSubScreen(HubSubScreen.Hub) },
                searchPlaceholder = "",
                trailingContent = {
                    var addFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = { viewModel.showMagNutzAddDialog(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (addFocused) Color(0xFF00A572) else Color(0xFF00A572),
                            contentColor = Color(0xFF00311F)
                        ),
                        modifier = Modifier
                            .height(44.dp)
                            .onFocusChanged { addFocused = it.isFocused }
                            .graphicsLayer {
                                scaleX = if (addFocused) 1.06f else 1f
                                scaleY = if (addFocused) 1.06f else 1f
                            }
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ADD MAGNET", fontWeight = FontWeight.Bold)
                    }
                }
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp, vertical = 8.dp)
            ) {
                val chips = listOf(null to "All", TorrentStatus.DOWNLOADING to "Downloading", TorrentStatus.SEEDING to "Seeding", TorrentStatus.COMPLETED to "Completed", TorrentStatus.FAILED to "Failed", TorrentStatus.PAUSED to "Paused")
                items(chips) { (status, label) ->
                    var chipFocused by remember { mutableStateOf(false) }
                    val isSelected = uiState.filter == status
                    Card(
                        onClick = { viewModel.setMagNutzFilter(status) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF00A572) else if (chipFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                            contentColor = if (isSelected) Color(0xFF00311F) else Color(0xFFc1c7d2)
                        ),
                        border = BorderStroke(
                            width = if (chipFocused) 2.dp else 0.dp,
                            color = if (chipFocused) Color.White else Color.Transparent
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .onFocusChanged { chipFocused = it.isFocused }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredTorrents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No torrents", color = Color(0xFF888888), fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Press ADD MAGNET to get started", color = Color(0xFF555555), fontSize = 14.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
                ) {
                    items(filteredTorrents, key = { it.id }) { torrent ->
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, tween(150), label = "tScale")
                        val statusColor = when (torrent.status) {
                            TorrentStatus.DOWNLOADING -> Color(0xFF00A572)
                            TorrentStatus.SEEDING -> Color(0xFF4D8EFF)
                            TorrentStatus.COMPLETED -> Color(0xFF888888)
                            TorrentStatus.FAILED -> Color(0xFFFFB4AB)
                            TorrentStatus.PAUSED -> Color(0xFFFFAA00)
                            TorrentStatus.QUEUED -> Color(0xFF888888)
                        }
                        Card(
                            onClick = { viewModel.selectTorrent(torrent) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) Color.White else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isFocused = it.isFocused }
                                .graphicsLayer { scaleX = scale; scaleY = scale }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    torrent.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .background(Color(0xFF333333), RoundedCornerShape(4.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = torrent.progress.coerceIn(0f, 1f))
                                            .background(statusColor, RoundedCornerShape(4.dp))
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${(torrent.progress * 100).toInt()}%",
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    val speedLabel = if (torrent.downloadSpeed > 0) formatSpeed(torrent.downloadSpeed) else ""
                                    if (speedLabel.isNotEmpty()) {
                                        Text(speedLabel, color = Color(0xFF888888), fontSize = 12.sp)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        torrent.status.name,
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedTorrent != null) {
            val torrent = uiState.selectedTorrent!!
            val detailFocusReq = remember { FocusRequester() }
            LaunchedEffect(Unit) { detailFocusReq.requestFocus() }
            BackHandler { viewModel.selectTorrent(null) }
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                    onClick = { viewModel.selectTorrent(null) }
                ), contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(520.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(torrent.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 12.dp))

                        val statItems = listOf(
                            "Peers" to "${torrent.peers}",
                            "Seeds" to "${torrent.seeds}",
                            "Down" to formatSpeed(torrent.downloadSpeed),
                            "Up" to formatSpeed(torrent.uploadSpeed),
                            "Ratio" to formatRatio(torrent),
                            "Size" to formatBytes(torrent.torrentSize),
                            "Progress" to "${(torrent.progress * 100).toInt()}%",
                            "Trackers" to "${torrent.activeTrackers}",
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(160.dp)
                        ) {
                            items(statItems) { (label, value) ->
                                Box(
                                    Modifier.background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)).padding(8.dp)
                                ) {
                                    Column {
                                        Text(label, color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Box(
                            Modifier.fillMaxWidth().background(Color(0xFF0D0D0D), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                torrent.magnetUri.take(80) + if (torrent.magnetUri.length > 80) "..." else "",
                                color = Color(0xFF888888), fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (torrent.status == TorrentStatus.PAUSED) {
                                ActionBtn("Resume", Color(0xFF00A572), onClick = { viewModel.resumeTorrent(torrent.id) }, focusReq = detailFocusReq)
                            } else {
                                ActionBtn("Pause", Color(0xFFFFAA00), onClick = { viewModel.pauseTorrent(torrent.id) }, focusReq = detailFocusReq)
                            }
                            if (torrent.status == TorrentStatus.COMPLETED || torrent.status == TorrentStatus.SEEDING) {
                                ActionBtn("Play", Color(0xFF4D8EFF), onClick = {
                                    val streamUrl = "http://127.0.0.1:8091/stream/${torrent.infoHash}"
                                    val ch = IptvChannel(id = "magnutz_${torrent.id}", name = torrent.name, url = streamUrl, logoUrl = null, categoryName = "Torrent")
                                    onPlayChannel(ch)
                                    viewModel.selectTorrent(null)
                                }, focusReq = detailFocusReq)
                            }
                            ActionBtn("Remove", Color(0xFFFFB4AB), onClick = { showDeleteConfirm = true })
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var saveFocused by remember { mutableStateOf(false) }
                            Surface(
                                onClick = {
                                    // Placeholder for save location picker
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (saveFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                                border = BorderStroke(if (saveFocused) 2.dp else 1.dp, if (saveFocused) Color.White else Color(0xFF444444).copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(44.dp).onFocusChanged { saveFocused = it.isFocused }
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Save Location", color = Color(0xFF00A572), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        var closeFocused by remember { mutableStateOf(false) }
                        Surface(
                            onClick = { viewModel.selectTorrent(null) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (closeFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                            border = BorderStroke(if (closeFocused) 2.dp else 1.dp, if (closeFocused) Color.White else Color(0xFF444444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(44.dp).onFocusChanged { closeFocused = it.isFocused }
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Close", color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteConfirm && uiState.selectedTorrent != null) {
            val delTorrent = uiState.selectedTorrent!!
            LaunchedEffect(Unit) { deleteConfirmFocusReq.requestFocus() }
            BackHandler { showDeleteConfirm = false }
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                    onClick = { showDeleteConfirm = false }
                ), contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(420.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Remove Torrent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Delete \"${delTorrent.name.take(60)}\" from MagNutz?", color = Color(0xFFB0B0B0), fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var delFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showDeleteConfirm = false; viewModel.removeTorrent(delTorrent.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (delFocused) Color(0xFFFF4444) else Color(0xFFCC2222),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.height(48.dp).onFocusChanged { delFocused = it.isFocused }.focusRequester(deleteConfirmFocusReq)
                            ) { Text("Remove", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            var delCancelFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showDeleteConfirm = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (delCancelFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.height(48.dp).onFocusChanged { delCancelFocused = it.isFocused }
                            ) { Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        }
                    }
                }
            }
        }

        if (uiState.showAddDialog) {
            LaunchedEffect(Unit) { addDialogFocusReq.requestFocus() }
            BackHandler { viewModel.showMagNutzAddDialog(false) }
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)).focusable().clickable(
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null,
                    onClick = { viewModel.showMagNutzAddDialog(false) }
                ), contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.width(600.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Add Magnet Link", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(bottom = 16.dp))

                        var inputFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = uiState.magnetInput,
                            onValueChange = { viewModel.setMagNutzMagnetInput(it) },
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(addDialogFocusReq)
                                .onFocusChanged { inputFocused = it.isFocused },
                            label = { Text("Paste magnet:?xt=urn:btih:...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = if (inputFocused) Color(0xFF00A572) else Color.White,
                                unfocusedBorderColor = Color(0xFF666666),
                                focusedLabelColor = Color.White,
                            ),
                        )

                        if (uiState.errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(uiState.errorMessage!!, color = Color(0xFFFFB4AB), fontSize = 13.sp)
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            var addBtnFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { viewModel.addMagnet(uiState.magnetInput) },
                                enabled = uiState.magnetInput.isNotBlank() && !uiState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (addBtnFocused) Color(0xFF00A572) else Color(0xFF00A572),
                                    contentColor = Color(0xFF00311F),
                                    disabledContainerColor = Color(0xFF333333),
                                ),
                                modifier = Modifier.height(48.dp).onFocusChanged { addBtnFocused = it.isFocused }
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Add", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            var cancelFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = { viewModel.showMagNutzAddDialog(false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cancelFocused) Color(0xFF2E2E2E) else Color(0xFF111111),
                                    contentColor = Color.White,
                                ),
                                modifier = Modifier.height(48.dp).onFocusChanged { cancelFocused = it.isFocused }
                            ) { Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                        }
                    }
                }
            }
        }
    }
}

private fun parseIsoMillis(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    try {
        return java.time.OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
    } catch (_: Exception) {}
    try {
        return java.time.LocalDateTime.parse(dateStr.take(19)).atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (_: Exception) {}
    try {
        return java.time.LocalDate.parse(dateStr.take(10)).atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    } catch (_: Exception) {}
    return null
}

private fun splitEventTeams(title: String): Pair<String, String> {
    val parts = title.split(Regex("\\s+[vV][sS]\\s+|\\s*@\\s*"))
    val first = parts.firstOrNull()?.trim().orEmpty()
    val second = parts.getOrNull(1)?.trim().orEmpty()
    if (first.isBlank() || second.isBlank()) return Pair("Away", "Home")
    return Pair(first, second)
}

private fun formatEventDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)
        val date = sdf.parse(dateStr.take(16))
        val out = java.text.SimpleDateFormat("MMM dd, h:mm a", java.util.Locale.US)
        date?.let { return out.format(it) }
    } catch (_: Exception) {}
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val date = sdf.parse(dateStr.take(10))
        val out = java.text.SimpleDateFormat("MMM dd", java.util.Locale.US)
        date?.let { return out.format(it) }
    } catch (_: Exception) {}
    return dateStr
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec >= 1_000_000_000 -> "${"%.1f".format(bytesPerSec / 1_000_000_000f)} GB/s"
        bytesPerSec >= 1_000_000 -> "${"%.1f".format(bytesPerSec / 1_000_000f)} MB/s"
        bytesPerSec >= 1_000 -> "${"%.1f".format(bytesPerSec / 1_000f)} KB/s"
        else -> "${bytesPerSec} B/s"
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000_000L -> "${"%.1f".format(bytes / 1_000_000_000_000f)} TB"
        bytes >= 1_000_000_000L -> "${"%.1f".format(bytes / 1_000_000_000f)} GB"
        bytes >= 1_000_000L -> "${"%.1f".format(bytes / 1_000_000f)} MB"
        bytes >= 1_000L -> "${"%.1f".format(bytes / 1_000f)} KB"
        else -> "${bytes} B"
    }
}

private fun formatRatio(torrent: TorrentItem): String {
    val ratio = if (torrent.torrentSize > 0) torrent.loadedSize.toFloat() / torrent.torrentSize else 0f
    return "%.2f".format(ratio)
}

@Composable
private fun ActionBtn(
    label: String,
    color: Color,
    onClick: () -> Unit,
    focusReq: FocusRequester? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) color.copy(alpha = 0.3f) else color.copy(alpha = 0.15f),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) Color.White else color.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .height(52.dp)
            .widthIn(min = 120.dp)
            .then(if (focusReq != null) Modifier.focusRequester(focusReq) else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (isFocused) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
