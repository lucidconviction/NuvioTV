package com.robbdeeze.nuviotv.ui.screens.sports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent
import com.robbdeeze.nuviotv.data.remote.dto.EspnStandingEntry
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.SportEvent
import com.robbdeeze.nuviotv.domain.model.SportLeague
import com.robbdeeze.nuviotv.domain.sports.HighlightVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val tvMargin = 48.dp
private val Bg = Color(0xFF000000)
private val CardBg = Color(0xFF1A1A1A)
private val CardBgFocused = Color(0xFF2E2E2E)
private val Accent = Color(0xFFE8553A)
private val AccentGreen = Color(0xFF22C55E)
private val LiveGreen = Color(0xFF00FF00)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFc1c7d2)
private val TextMuted = Color(0xFF666666)

@Composable
fun TvSportsLayout(
    leagues: List<SportLeague>,
    selectedLeague: SportLeague?,
    onSelectLeague: (SportLeague?) -> Unit,
    allLiveEvents: List<SportEvent>,
    allLiveLoading: Boolean,
    leagueEvents: List<SportEvent>,
    standings: List<EspnStandingEntry>,
    standingsLoading: Boolean,
    highlightVideos: List<HighlightVideo>,
    highlightVideosLoading: Boolean = false,
    daddyLiveEvents: List<DaddyLiveEvent>,
    onRefresh: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onShowChannels: (List<IptvChannel>, String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var searchResults by remember { mutableStateOf<List<SportEvent>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val displayEvents = if (selectedLeague == null) allLiveEvents else leagueEvents
    val liveEvents = displayEvents.filter { it.isLive }
    val upcomingEvents = displayEvents.filter { !it.isLive }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = tvMargin)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("SportNutz", color = Accent, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.width(24.dp))
                Box(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
                    var searchFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            searchJob?.cancel()
                            if (q.isNotBlank()) {
                                searchJob = scope.launch {
                                    delay(400)
                                    val lower = q.lowercase()
                                    isSearching = true
                                    searchResults = displayEvents.filter { e ->
                                        e.name.lowercase().contains(lower) ||
                                        e.homeTeam.displayName.lowercase().contains(lower) ||
                                        e.awayTeam.displayName.lowercase().contains(lower) ||
                                        e.leagueAbbreviation.lowercase().contains(lower)
                                    }
                                    isSearching = false
                                }
                            } else {
                                searchResults = emptyList()
                            }
                        },
                        placeholder = { Text("Search teams, leagues, events...", color = TextMuted.copy(alpha = 0.5f), fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextMuted, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = ""; searchResults = emptyList() }) {
                                    Icon(Icons.Filled.Close, "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Accent, unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
                            cursorColor = Accent,
                            focusedContainerColor = CardBg, unfocusedContainerColor = CardBg,
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .onFocusChanged { searchFocused = it.isFocused },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // League chips
            var chipsFocused by remember { mutableStateOf(false) }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
                    .onFocusChanged { chipsFocused = it.isFocused },
            ) {
                items(leagues) { league ->
                    var chipFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedLeague?.id == league.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                when { isSelected -> Color.White; chipFocused -> Color.White.copy(alpha = 0.15f); else -> CardBg.copy(alpha = 0.5f) }
                            )
                            .clickable { onSelectLeague(if (isSelected) null else league) }
                            .onFocusChanged { chipFocused = it.isFocused }
                            .focusable()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(league.name, color = if (isSelected) Color.Black else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // Search results
            if (searchResults.isNotEmpty()) {
                Text("Search Results (${searchResults.size})", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(((searchResults.size / 3 + 1) * 140).dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false,
                ) {
                    gridItems(searchResults.take(9)) { event ->
                        var srFocused by remember { mutableStateOf(false) }
                        val srScale by animateFloatAsState(targetValue = if (srFocused) 1.05f else 1f, tween(150), label = "sr")
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (srFocused) CardBgFocused else CardBg)
                                .border(if (srFocused) 2.dp else 0.dp, if (srFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { onShowChannels(emptyList(), event.name) }
                                .onFocusChanged { srFocused = it.isFocused }
                                .focusable()
                                .graphicsLayer { scaleX = srScale; scaleY = srScale }
                                .padding(14.dp),
                        ) {
                            Text(event.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            // Live Events Grid
            if (liveEvents.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Live Now", color = Accent, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("${liveEvents.size} ACTIVE", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(((minOf(liveEvents.size, 6) / 3 + 1) * 200).dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    userScrollEnabled = false,
                ) {
                    gridItems(liveEvents.take(6)) { event ->
                        var cardFocused by remember { mutableStateOf(false) }
                        val cardScale by animateFloatAsState(targetValue = if (cardFocused) 1.05f else 1f, tween(150), label = "lc")
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (cardFocused) CardBgFocused else CardBg)
                                .border(if (cardFocused) 2.dp else 0.dp, if (cardFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { onShowChannels(emptyList(), event.name) }
                                .onFocusChanged { cardFocused = it.isFocused }
                                .focusable()
                                .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
                                .padding(14.dp),
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("LIVE", color = LiveGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                        modifier = Modifier.background(LiveGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(event.leagueAbbreviation, color = Accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(event.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
                                            if (!event.awayTeam.logoUrl.isNullOrBlank()) AsyncImage(model = event.awayTeam.logoUrl, null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                                            else Text(event.awayTeam.displayName.take(2).uppercase(), color = TextMuted, fontSize = 8.sp)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(event.awayTeam.displayName, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(event.awayScore ?: "-", color = if ((event.awayScore?.toIntOrNull() ?: 0) > (event.homeScore?.toIntOrNull() ?: 0)) AccentGreen else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
                                            if (!event.homeTeam.logoUrl.isNullOrBlank()) AsyncImage(model = event.homeTeam.logoUrl, null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                                            else Text(event.homeTeam.displayName.take(2).uppercase(), color = TextMuted, fontSize = 8.sp)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(event.homeTeam.displayName, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(event.homeScore ?: "-", color = if ((event.homeScore?.toIntOrNull() ?: 0) > (event.awayScore?.toIntOrNull() ?: 0)) AccentGreen else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // Standings + Highlights row
            if (standings.isNotEmpty() || highlightVideos.isNotEmpty() || highlightVideosLoading) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    if (standings.isNotEmpty()) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg).padding(16.dp)) {
                                Text("Standings", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(Modifier.height(12.dp))
                                standings.take(5).forEachIndexed { index, entry ->
                                    val team = entry.team
                                    val stats = entry.stats ?: emptyList()
                                    val wins = stats.find { it.name == "wins" }?.displayValue ?: "0"
                                    val losses = stats.find { it.name == "losses" }?.displayValue ?: "0"
                                    val record = "$wins-$losses"
                                    var stFocused by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (index == 0) CardBgFocused else Color.Transparent)
                                            .onFocusChanged { stFocused = it.isFocused }
                                            .focusable()
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text((index + 1).toString().padStart(2, '0'), color = if (index == 0) Accent else TextMuted, fontSize = 11.sp,
                                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(24.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(Modifier.size(22.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
                                                if (!team?.logo.isNullOrBlank()) AsyncImage(model = team!!.logo, null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                                                else Text(team?.displayName?.take(2)?.uppercase() ?: "?", color = TextMuted, fontSize = 8.sp)
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(team?.displayName ?: "", color = TextPrimary, fontSize = 12.sp, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text(record, color = if (index == 0) Accent else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    if (index < minOf(standings.size, 5) - 1) HorizontalDivider(color = TextMuted.copy(alpha = 0.15f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                    if (highlightVideosLoading && highlightVideos.isEmpty()) {
                        Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.CenterStart) {
                            CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                        }
                    } else if (highlightVideos.isNotEmpty()) {
                        Box(modifier = Modifier.weight(2f)) {
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Highlights", color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    itemsIndexed(highlightVideos.take(8)) { _, h ->
                                        HighlightCard(
                                            thumbnailUrl = h.thumbnail,
                                            title = h.title,
                                            channelName = h.channelName,
                                            durationSeconds = h.durationSeconds,
                                            onClick = {},
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // Fight Card section for combat sports
            if (selectedLeague?.id in setOf("mma", "ufc", "bkfc", "powerslap", "boxing", "pfl") && upcomingEvents.isNotEmpty()) {
                Text("Fight Cards", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    itemsIndexed(upcomingEvents.take(8)) { _, event ->
                        var fcFocused by remember { mutableStateOf(false) }
                        val fcScale by animateFloatAsState(targetValue = if (fcFocused) 1.05f else 1f, tween(150), label = "fc")
                        Box(
                            modifier = Modifier.width(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (fcFocused) CardBgFocused else CardBg)
                                .border(if (fcFocused) 2.dp else 0.dp, if (fcFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .onFocusChanged { fcFocused = it.isFocused }
                                .focusable()
                                .graphicsLayer { scaleX = fcScale; scaleY = fcScale }
                                .padding(14.dp),
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(event.leagueAbbreviation, color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(event.name.substringBefore(" vs", event.name).substringBefore(" vs.", event.name), color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(event.awayTeam.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text("VS", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text(event.homeTeam.displayName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(10.dp))
                                Text("FIGHT NIGHT", color = Accent.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // Upcoming
            if (upcomingEvents.isNotEmpty()) {
                Text("Upcoming", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    itemsIndexed(upcomingEvents.take(8)) { i, event ->
                        var ucFocused by remember { mutableStateOf(false) }
                        val ucScale by animateFloatAsState(targetValue = if (ucFocused) 1.05f else 1f, tween(150), label = "uc")
                        Box(
                            modifier = Modifier.width(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (ucFocused) CardBgFocused else CardBg)
                                .border(if (ucFocused) 2.dp else 0.dp, if (ucFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { onShowChannels(emptyList(), event.name) }
                                .onFocusChanged { ucFocused = it.isFocused }
                                .focusable()
                                .graphicsLayer { scaleX = ucScale; scaleY = ucScale }
                                .padding(14.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(event.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                Text("${event.awayTeam.displayName} vs ${event.homeTeam.displayName}", color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            // DaddyLive
            if (daddyLiveEvents.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sports Now/Later", color = Accent, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Accent).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("DADDYLIVE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    daddyLiveEvents.take(10).forEach { dlEvent ->
                        TvDaddyLiveCard(event = dlEvent, onClick = { onShowChannels(emptyList(), dlEvent.eventName) })
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
