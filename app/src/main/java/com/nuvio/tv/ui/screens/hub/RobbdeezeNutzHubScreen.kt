package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.robbdeeze.nuviotv.ui.components.FocusMarqueeText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.data.sports.YouTubeStreamResolver
import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import com.robbdeeze.nuviotv.domain.model.*
import com.robbdeeze.nuviotv.ui.screens.player.IptvPlayerStore
import com.robbdeeze.nuviotv.ui.screens.player.SportsNowStore
import kotlinx.coroutines.Job
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
                else -> HubSubScreen.Iptv
            }
            viewModel.setSubScreen(target)
        }
    }
    val scope = rememberCoroutineScope()

    // Tab Reset flow
    LaunchedEffect(Unit) {
        viewModel.resetEvent.collect {
            viewModel.setSubScreen(HubSubScreen.Hub)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (subScreen != HubSubScreen.Hub) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { viewModel.setSubScreen(HubSubScreen.Hub) },
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .border(
                                width = if (isBackFocused) 2.dp else 0.dp,
                                color = if (isBackFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when (subScreen) {
                        HubSubScreen.Hub -> "RobbdeezeNutz Hubz"
                        HubSubScreen.Iptv -> "IPTVNutz"
                        HubSubScreen.Sports -> "SportNutz"
                        HubSubScreen.VidNutz -> "VidNutz"
                        HubSubScreen.MusicNutz -> "MusicNutz"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
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
                }
            }
        }
    }
}

@Composable
fun HubScreenContent(
    onSelectScreen: (HubSubScreen) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize().padding(start = 32.dp, top = 64.dp, end = 32.dp, bottom = 32.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        item {
            HubCard(
                title = "IPTVNutz",
                description = "Custom M3U and Xtream playlist live streams manager",
                badge = "TV",
                badgeColor = Color(0xFF4A90D9),
                onClick = { onSelectScreen(HubSubScreen.Iptv) }
            )
        }
        item {
            HubCard(
                title = "SportNutz",
                description = "Leagues scoreboards, standings, and match highlights finder",
                badge = "SP",
                badgeColor = Color(0xFFE8553A),
                onClick = { onSelectScreen(HubSubScreen.Sports) }
            )
        }
        item {
            HubCard(
                title = "VidNutz",
                description = "Browse, search, and watch videos from across the web",
                badge = "VN",
                badgeColor = Color(0xFF6C5CE7),
                onClick = { onSelectScreen(HubSubScreen.VidNutz) }
            )
        }
        item {
            HubCard(
                title = "MusicNutz",
                description = "Deezer music collections with seamless YouTube audio backing",
                badge = "MU",
                badgeColor = Color(0xFF00CEC9),
                onClick = { onSelectScreen(HubSubScreen.MusicNutz) }
            )
        }
    }
}

@Composable
fun HubCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = tween(180),
        label = "cardScale"
    )

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) Color.White else Color.Transparent
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 12f else 4f
            }
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isFocused) {
                        Brush.verticalGradient(
                            colors = listOf(
                                badgeColor.copy(alpha = 0.15f),
                                Color(0xFF1A1A1A)
                            )
                        )
                    } else Brush.verticalGradient(
                        colors = listOf(
                            badgeColor.copy(alpha = 0.05f),
                            Color(0xFF1A1A1A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        color = badgeColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        color = Color(0xFFB0B0B0),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    var activeSource by remember { mutableStateOf<IptvSource?>(null) }
    var newSourceName by remember { mutableStateOf("") }
    var newSourceUrl by remember { mutableStateOf("") }
    var newSourceType by remember { mutableStateOf("m3u") }
    var showSearch by remember { mutableStateOf(false) }
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

    if (activeSource == null) {
        // Sources Dashboard
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Favorites Section
            if (favorites.isNotEmpty()) {
                item {
                    Text("Favorites", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(favorites) { channel ->
                            var isFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { onPlayChannel(channel) },
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(100.dp)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) Color.White else Color(0x33FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF1E1E1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(channel.name, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                            viewModel.addIptvSource("IPTV-org Global", "https://iptv-org.github.io/iptv/index.m3u", "m3u")
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
                                onClick = { activeSource = IptvSource("All", "__all__", "m3u") },
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
                        Card(
                            onClick = { activeSource = source },
                            colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                            modifier = Modifier
                                .width(170.dp).height(140.dp)
                                .onFocusChanged { isFocused = it.isFocused }
                                .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = if (isFocused) 12f else 0f }
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                Column(Modifier.fillMaxSize().padding(12.dp)) {
                                    Text(source.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.weight(1f))
                                    Text(source.url, color = Color(0xFF888888), fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(6.dp))
                                    Text(source.type.uppercase(), color = Color(0xFF4A90D9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                // Delete button
                                var deleteFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { viewModel.removeIptvSource(source.url) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp).onFocusChanged { deleteFocused = it.isFocused }
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = if (deleteFocused) Color.Red else Color(0xFF666666), modifier = Modifier.size(16.dp))
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
                val addFocus = remember { FocusRequester() }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111), contentColor = Color.White),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add New IPTV Source", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Name field
                        var nameFocused by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = newSourceName, onValueChange = { newSourceName = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                                .onFocusChanged { nameFocused = it.isFocused }
                                .focusProperties { down = urlFocus },
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
                                .focusProperties { down = m3uFocus; up = nameFocus },
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
                                    .focusProperties { down = addFocus; up = m3uFocus }
                            ) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("Xtream Codes", color = if (newSourceType == "xtream") Color.White else Color(0xFFB0B0B0), fontWeight = FontWeight.Bold)
                                }
                            }
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
                                if (newSourceName.isNotBlank() && newSourceUrl.isNotBlank()) {
                                    viewModel.addIptvSource(newSourceName, newSourceUrl, newSourceType)
                                    newSourceName = ""
                                    newSourceUrl = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (addFocused) Color(0xFF4A90D9) else Color.White, contentColor = Color.Black),
                            modifier = Modifier.align(Alignment.End).focusRequester(addFocus)
                                .onFocusChanged { addFocused = it.isFocused }
                                .focusProperties { up = xtreamFocus }
                        ) { Text("Add Source") }
                    }
                }
            }
        }
    } else {
        // Channel Browser
        var selectedCategory by remember { mutableStateOf<String?>(null) }
        val categoryFiltered = remember(channels, selectedCategory) {
            if (selectedCategory != null) channels.filter { it.categoryName == selectedCategory }
            else channels
        }
        val displayChannels = remember(categoryFiltered, searchQuery) {
            if (searchQuery.isBlank()) categoryFiltered
            else categoryFiltered.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

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
                Button(
                    onClick = { activeSource = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White)
                ) {
                    Text("Sources")
                }
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

            Spacer(modifier = Modifier.height(8.dp))

            // Category tab bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedCategory == null
                    Column {
                        Box(
                            modifier = Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Color(0xFF2A2A2A)
                                        isFocused -> Color(0xFF333333)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(if (isFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { selectedCategory = null }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "All",
                                color = if (isSelected) Color.White else Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // Selected indicator line
                        if (isSelected) Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF4A90D9)).padding(horizontal = 8.dp))
                    }
                }
                items(categories) { cat ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedCategory == cat
                    Column {
                        Box(
                            modifier = Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Color(0xFF2A2A2A)
                                        isFocused -> Color(0xFF333333)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(if (isFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else Color(0xFF888888),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                        // Selected indicator line
                        if (isSelected) Box(Modifier.fillMaxWidth().height(3.dp).background(Color(0xFF4A90D9)).padding(horizontal = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (displayChannels.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (searchQuery.isNotBlank()) "No channels match \"$searchQuery\""
                        else "No channels found",
                        color = Color(0xFFB0B0B0)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayChannels, key = { it.id }) { channel ->
                        val isFav = favorites.any { it.id == channel.id }
                        ChannelGridCard(
                            channel = channel,
                            isFavorite = isFav,
                            onClick = {
                                val idx = channels.indexOf(channel).coerceAtLeast(0)
                                IptvPlayerStore.setChannels(channels, idx)
                                onPlayChannel(channel)
                            },
                            onToggleFavorite = { viewModel.toggleIptvFavorite(channel, !isFav) }
                        )
                    }
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
    val allLiveLoading by viewModel.allLiveLoading.collectAsState()
    val selectedEvent by viewModel.selectedSportEvent.collectAsState()
    val matchedChannels by viewModel.matchedChannels.collectAsState()
    val channelsLoading by viewModel.sportsChannelLoading.collectAsState()
    val sportVideos by viewModel.sportEventVideos.collectAsState()
    val sportVideosLoading by viewModel.sportVideosLoading.collectAsState()
    val regionFilter by viewModel.sportRegionFilter.collectAsState()
    val activeTab by viewModel.activeEventTab.collectAsState()
    var selectedLeague by remember { mutableStateOf<SportLeague?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(leagues) { league ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = selectedLeague?.id == league.id
                Card(
                    onClick = { selectedLeague = league },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A),
                        contentColor = if (isSelected) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(vertical = 4.dp)
                ) {
                    Text(league.name, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedEvent != null) {
            SportEventDetailPanel(
                event = selectedEvent!!,
                matchedChannels = matchedChannels,
                channelsLoading = channelsLoading,
                videos = sportVideos,
                videosLoading = sportVideosLoading,
                regionFilter = regionFilter,
                activeTab = activeTab,
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
                            val resolvedUrl = YouTubeStreamResolver.resolveStreamUrl(ytVideo.videoId)
                            resolvedUrl?.let { playUrl ->
                                onPlayChannel(
                                    IptvChannel(
                                        id = ytVideo.videoId,
                                        name = ytVideo.title,
                                        url = playUrl,
                                        logoUrl = ytVideo.thumbnailUrl
                                    )
                                )
                            }
                        }
                    }
                }
            )
        } else if (selectedLeague?.id == "now") {
            if (allLiveLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning all leagues for live events...", color = Color(0xFFB0B0B0))
                    }
                }
            } else if (allLiveEvents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No live events right now", color = Color.LightGray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allLiveEvents, key = { it.id }) { event ->
                        var isFocused by remember { mutableStateOf(false) }
                        Card(
                            onClick = { viewModel.selectSportEvent(event) },
                            colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                            border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
                            modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("LIVE", color = Color(0xFF00FF00), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                        modifier = Modifier.background(Color(0xFF00FF00).copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(event.leagueAbbreviation, color = Color(0xFF4A90D9), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text(event.status, color = Color.Red, fontSize = 11.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        TeamLogoOrPlaceholder(event.awayTeam.logoUrl)
                                        Spacer(Modifier.height(4.dp))
                                        Text(event.awayTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(event.awayScore ?: "-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("VS", color = Color(0xFF666666), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        TeamLogoOrPlaceholder(event.homeTeam.logoUrl)
                                        Spacer(Modifier.height(4.dp))
                                        Text(event.homeTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(event.homeScore ?: "-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedLeague == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a League to View Scores", color = Color.LightGray)
            }
        } else if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            // Scoreboard
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(events) { event ->
                    var isFocused by remember { mutableStateOf(false) }
                    Card(
                        onClick = { viewModel.selectSportEvent(event) },
                        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (event.isLive) {
                                    Text(
                                        text = "LIVE",
                                        color = Color(0xFF00FF00),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF00FF00).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(event.status, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(event.date.substringBefore("T"), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogoOrPlaceholder(event.awayTeam.logoUrl)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(event.awayTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(event.awayScore ?: "-", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamLogoOrPlaceholder(event.homeTeam.logoUrl)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(event.homeTeam.displayName, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(event.homeScore ?: "-", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            }
                            if (event.isLive) {
                                Spacer(modifier = Modifier.height(8.dp))
                                var btnFocused by remember { mutableStateOf(false) }
                                Card(
                                    onClick = { viewModel.selectSportEvent(event) },
                                    colors = CardDefaults.cardColors(containerColor = if (btnFocused) Color(0xFF4A90D9) else Color(0xFF0D1B2A)),
                                    border = BorderStroke(if (btnFocused) 2.dp else 1.dp, Color(0xFF4A90D9).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { btnFocused = it.isFocused }
                                ) {
                                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Find Channel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(Modifier.weight(1f))
                                        Text("Watch Live", color = Color(0xFF4A90D9), fontSize = 11.sp)
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
private fun TeamLogoOrPlaceholder(logoUrl: String?) {
    if (logoUrl != null) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
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
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onSetRegion: (String) -> Unit,
    onSetTab: (EventTab) -> Unit,
    onSearchVideos: (Boolean) -> Unit,
    onPlayHighlight: (SportEvent) -> Unit,
) {
    val isLive = event.isLive
    val scope = rememberCoroutineScope()
    val tabs = buildList {
        if (isLive) add(EventTab.LIVE)
        add(EventTab.HIGHLIGHTS)
        if (!isLive) add(EventTab.PRE_MATCH)
    }

    LaunchedEffect(event) {
        onSetTab(tabs.first())
        if (!isLive) onSearchVideos(false)
        if (isLive) onSetTab(EventTab.LIVE)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back and event info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            var isBackFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .border(
                        width = if (isBackFocused) 2.dp else 0.dp,
                        color = if (isBackFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.status, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Tab chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabs) { tab ->
                var isFocused by remember { mutableStateOf(false) }
                val isSelected = tab == activeTab
                Box(
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when { isSelected -> Color.White; isFocused -> Color.White.copy(alpha = 0.15f); else -> Color(0xFF1A1A1A) }
                        )
                        .then(if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier)
                        .clickable { onSetTab(tab); if (tab == EventTab.HIGHLIGHTS || tab == EventTab.PRE_MATCH) onSearchVideos(tab == EventTab.PRE_MATCH) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(tab.label, color = if (isSelected || isFocused) Color.Black else Color(0xFFB0B0B0), fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab content
        when (activeTab) {
            EventTab.LIVE -> {
                // Region filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("ALL" to "All", "US" to "US", "UK" to "UK", "CA" to "CA")) { (id, label) ->
                        var isFocused by remember { mutableStateOf(false) }
                        val isSelected = regionFilter == id
                        Box(
                            modifier = Modifier
                                .onFocusChanged { isFocused = it.isFocused }
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(50))
                                .background(when { isSelected -> Color(0xFF4A90D9); isFocused -> Color.White.copy(alpha = 0.15f); else -> Color(0xFF1A1A1A) })
                                .then(if (!isSelected && isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(50)) else Modifier)
                                .clickable { onSetRegion(id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = if (isSelected) Color.White else Color(0xFFB0B0B0), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val filtered = if (regionFilter == "ALL") matchedChannels
                    else matchedChannels.filter { it.region == regionFilter || (it.region == "Other" && regionFilter == "US") }
                ChannelListContent(
                    matchedChannels = filtered,
                    isLoading = channelsLoading,
                    event = event,
                    onPlayChannel = onPlayChannel,
                    onPlayHighlight = onPlayHighlight,
                )
            }
            EventTab.HIGHLIGHTS, EventTab.PRE_MATCH -> {
                if (videosLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (videos.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No videos found", color = Color(0xFFB0B0B0))
                            Spacer(modifier = Modifier.height(8.dp))
                            var retryFocused by remember { mutableStateOf(false) }
                            Card(
                                onClick = { onSearchVideos(activeTab == EventTab.PRE_MATCH) },
                                colors = CardDefaults.cardColors(containerColor = if (retryFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                                modifier = Modifier.onFocusChanged { retryFocused = it.isFocused }
                            ) { Text("Retry", modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp), color = Color.White) }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(videos, key = { it.videoId }) { video ->
                            VideoCardSmall(
                                video = video,
                                onClick = {
                                    scope.launch {
                                        val streamUrl = YouTubeStreamResolver.resolveStreamUrl(video.videoId)
                                        streamUrl?.let { url ->
                                            onPlayChannel(IptvChannel(id = video.videoId, name = video.title, url = url, logoUrl = video.thumbnailUrl))
                                        }
                                    }
                                }
                            )
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
    val ts = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier.fillMaxWidth().focusable().onFocusChanged { isFocused = it.isFocused }.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(ts).background(Color(0xFF121212))
                .then(if (isFocused) Modifier.border(2.dp, Color.White, ts) else Modifier)
        ) {
            AsyncImage(model = video.thumbnailUrl, video.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            var isBackFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .border(
                        width = if (isBackFocused) 2.dp else 0.dp,
                        color = if (isBackFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
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

    LaunchedEffect(Unit) {
        if (uiState.videos.isEmpty() && uiState.searchQuery.isBlank()) {
            viewModel.loadVidNutzVideos(VidNutzCategory.TRENDING)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search toggle + collapsible search input
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            var searchFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        searchJob?.cancel()
                        viewModel.loadVidNutzVideos(uiState.selectedCategory)
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
            if (uiState.searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { q ->
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
                        if (uiState.searchQuery.isNotBlank()) viewModel.searchVidNutz(uiState.searchQuery)
                    }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    searchJob?.cancel()
                    viewModel.loadVidNutzVideos(uiState.selectedCategory)
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Category Chips
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

        Spacer(modifier = Modifier.height(16.dp))

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
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayVideos, key = { it.videoId }) { video ->
                    VidNutzVideoCard(
                        video = video,
                        onClick = {
                            scope.launch {
                                val streamUrl = YouTubeStreamResolver.resolveStreamUrl(video.videoId)
                                streamUrl?.let { url ->
                                    onPlayChannel(
                                        IptvChannel(
                                            id = video.videoId,
                                            name = video.title,
                                            url = url,
                                            logoUrl = video.thumbnailUrl
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
                item(key = "__load_more__") {
                    val showLoading = uiState.isLoadingMore
                    val showButton = !uiState.isLoadingMore && uiState.hasMore && displayVideos.isNotEmpty()
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        if (showLoading) {
                            CircularProgressIndicator(
                                color = Color.White.copy(alpha = 0.5f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (showButton) {
                            Button(
                                onClick = { viewModel.loadMoreVidNutz() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Load More", fontWeight = FontWeight.Bold)
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
    onClick: () -> Unit,
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
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isFocused) 16f else 0f
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
                .then(if (isFocused) Modifier.border(2.dp, Color.White, thumbShape) else Modifier)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
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
            // Play overlay
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White.copy(alpha = if (isFocused) 1f else 0.7f),
                modifier = Modifier
                    .size(if (isFocused) 56.dp else 48.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isFocused) 0.5f else 0.3f))
                    .padding(12.dp)
            )
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
                        val streamUrl = YouTubeStreamResolver.resolveStreamUrl(yId)
                        streamUrl?.let { url ->
                            onPlayChannel(
                                IptvChannel(
                                    id = yId,
                                    name = "${track.artistName} - ${track.title}",
                                    url = url,
                                    logoUrl = track.albumCover
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

        // Mode Toggle (Tracks / Albums)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MusicModeChip(
                label = "Tracks",
                isSelected = uiState.mode == MusicNutzMode.TRACKS,
                onClick = { viewModel.setMusicNutzMode(MusicNutzMode.TRACKS) }
            )
            MusicModeChip(
                label = "Albums",
                isSelected = uiState.mode == MusicNutzMode.ALBUMS,
                onClick = { viewModel.setMusicNutzMode(MusicNutzMode.ALBUMS) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        if (uiState.mode == MusicNutzMode.TRACKS) {
            TrackContent(uiState, viewModel, onPlayChannel)
        } else {
            AlbumContent(uiState, viewModel, onPlayChannel)
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
                    else -> Color(0xFF1A1A1A)
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
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(display, key = { "t_${it.id}" }) { track ->
                TrackCard(track = track, onClick = {
                    scope.launch {
                        val youtubeId = viewModel.resolveMusicTrackYoutubeId(track)
                        youtubeId?.let { yId ->
                            val streamUrl = YouTubeStreamResolver.resolveStreamUrl(yId)
                            streamUrl?.let { url ->
                                onPlayChannel(
                                    IptvChannel(
                                        id = yId,
                                        name = "${track.artistName} - ${track.title}",
                                        url = url,
                                        logoUrl = track.albumCover
                                    )
                                )
                            }
                        }
                    }
                })
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
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(display, key = { "a_${it.id}" }) { album ->
                AlbumCard(album = album, onClick = { viewModel.selectAlbum(album) })
            }
        }
    }
}

@Composable
private fun TrackCard(track: MusicTrack, onClick: () -> Unit) {
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
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
                .then(if (isFocused) Modifier.border(2.dp, Color.White, thumbShape) else Modifier)
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
private fun AlbumCard(album: MusicAlbum, onClick: () -> Unit) {
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
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(thumbShape)
                .background(Color(0xFF121212))
                .then(if (isFocused) Modifier.border(2.dp, Color.White, thumbShape) else Modifier)
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
            var isBackFocused by remember { mutableStateOf(false) }
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .onFocusChanged { isBackFocused = it.isFocused }
                    .border(
                        width = if (isBackFocused) 2.dp else 0.dp,
                        color = if (isBackFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF121212)),
                        contentScale = ContentScale.Crop
                    )
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
