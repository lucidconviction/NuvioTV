package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.SportEvent

@Composable
fun TeamDetailScreen(
    teamName: String,
    teamLogo: String?,
    allEvents: List<SportEvent>,
    onBack: () -> Unit,
    onPlayChannel: (IptvChannel) -> Unit,
    onEventSelected: (SportEvent) -> Unit,
) {
    val teamEvents = allEvents.filter { event ->
        event.homeTeam.displayName == teamName || event.awayTeam.displayName == teamName
    }
    val recentResults = teamEvents
        .filter { it.status.uppercase() in setOf("FINAL", "FULL TIME", "FT", "ENDED", "COMPLETE") }
        .sortedByDescending { it.date }
        .take(10)
    val gamesLive = teamEvents.filter { it.isLive }
    val upcoming = teamEvents
        .filter { !it.isLive && it.status.uppercase() !in setOf("FINAL", "FULL TIME", "FT", "ENDED", "COMPLETE") }
        .sortedBy { it.date }
        .take(5)

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            var backFocused by remember { mutableStateOf(false) }
            IconButton(onClick = onBack, modifier = Modifier.size(44.dp).onFocusChanged { backFocused = it.isFocused }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (backFocused) Color.White else Color(0xFF888888))
            }
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF1A1A1A)), contentAlignment = Alignment.Center) {
                if (!teamLogo.isNullOrBlank()) {
                    AsyncImage(model = teamLogo, contentDescription = teamName, modifier = Modifier.size(48.dp), contentScale = ContentScale.Fit)
                } else {
                    Text(teamName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(teamName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${teamEvents.size} games", color = Color(0xFF888888), fontSize = 13.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            if (gamesLive.isNotEmpty()) {
                item {
                    Text("Live Now", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(gamesLive) { event ->
                    TeamGameRow(event = event, onPlayChannel = onPlayChannel, onEventSelected = onEventSelected)
                }
            }

            if (recentResults.isNotEmpty()) {
                item {
                    Text("Recent Results", color = Color(0xFFa0caff), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(recentResults) { event ->
                    TeamGameRow(event = event, onPlayChannel = onPlayChannel, onEventSelected = onEventSelected)
                }
            }

            if (upcoming.isNotEmpty()) {
                item {
                    Text("Upcoming", color = Color(0xFFa0caff), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                items(upcoming) { event ->
                    TeamGameRow(event = event, onPlayChannel = onPlayChannel, onEventSelected = onEventSelected)
                }
            }

            if (recentResults.isEmpty() && upcoming.isEmpty() && gamesLive.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No game data available", color = Color(0xFFB0B0B0), fontSize = 14.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TeamGameRow(
    event: SportEvent,
    onPlayChannel: (IptvChannel) -> Unit,
    onEventSelected: (SportEvent) -> Unit,
) {
    val isLive = event.isLive
    val homeScore = event.homeScore?.toIntOrNull()
    val awayScore = event.awayScore?.toIntOrNull()
    val homeWon = homeScore != null && awayScore != null && homeScore > awayScore
    val awayWon = homeScore != null && awayScore != null && awayScore > homeScore

    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = { onEventSelected(event) },
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
        border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
        modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
                        if (!event.homeTeam.logoUrl.isNullOrBlank()) {
                            AsyncImage(model = event.homeTeam.logoUrl, contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                        } else {
                            Text(event.homeTeam.displayName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 10.sp)
                        }
                    }
                    Text(event.homeTeam.displayName, color = if (homeWon) Color(0xFF4ADE80) else Color(0xFFc1c7d2), fontSize = 14.sp, fontWeight = if (homeWon) FontWeight.ExtraBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(4.dp))
                    Text(event.homeScore ?: "-", color = if (homeWon) Color(0xFF4ADE80) else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF0D1117)), contentAlignment = Alignment.Center) {
                        if (!event.awayTeam.logoUrl.isNullOrBlank()) {
                            AsyncImage(model = event.awayTeam.logoUrl, contentDescription = null, modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                        } else {
                            Text(event.awayTeam.displayName.take(2).uppercase(), color = Color(0xFF888888), fontSize = 10.sp)
                        }
                    }
                    Text(event.awayTeam.displayName, color = if (awayWon) Color(0xFF4ADE80) else Color(0xFFc1c7d2), fontSize = 14.sp, fontWeight = if (awayWon) FontWeight.ExtraBold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(4.dp))
                    Text(event.awayScore ?: "-", color = if (awayWon) Color(0xFF4ADE80) else Color(0xFFc1c7d2), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            if (isLive) {
                Text("LIVE", color = Color(0xFFFFB4AB), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF0D1117)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
