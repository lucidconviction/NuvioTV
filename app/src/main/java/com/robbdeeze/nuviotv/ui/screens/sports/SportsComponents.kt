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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.data.sports.DaddyLiveEvent
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.sports.EspnProcessedEvent
import com.robbdeeze.nuviotv.domain.sports.TeamStanding

private val Bg = Color(0xFF000000)
private val SurfaceBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF1A1A1A)
private val CardBgFocused = Color(0xFF2E2E2E)
private val Accent = Color(0xFFE8553A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFc1c7d2)
private val TextMuted = Color(0xFF666666)
private val LiveGreen = Color(0xFF00FF00)
private val ChannelBlue = Color(0xFF4A90D9)
private val Primary = Color(0xFFDBFCFF)
private val OnSurface = Color(0xFFDAE2FD)
private val OnSurfaceVariant = Color(0xFFB9CACB)
private val AccentGreen = Color(0xFF22C55E)
private val ErrorRed = Color(0xFFFFB4AB)
private val SurfaceContainer = Color(0xFF111111)
private val SurfaceContainerHigh = Color(0xFF1A1A1A)
private val SurfaceContainerLow = Color(0xFF0D1117)

@Composable
fun DpadCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(150),
        label = "dpadScale",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isFocused) CardBgFocused else CardBg)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = shape,
            )
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
fun TvScoreCard(
    event: EspnProcessedEvent,
    isLive: Boolean,
    onClick: () -> Unit,
) {
    DpadCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        event.timeStr ?: event.detail.ifBlank { event.status }.uppercase(),
                        color = if (isLive) Accent else TextSecondary,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                    )
                    if (isLive) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(9999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(ErrorRed))
                            Text("LIVE", color = ErrorRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Text(event.league.uppercase().take(8), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(16.dp))
            TeamScoreRow(label = event.homeTeam, score = event.homeScore, logo = event.homeLogo, isWinning = (event.homeScore?.toIntOrNull() ?: 0) > (event.awayScore?.toIntOrNull() ?: 0))
            Spacer(Modifier.height(12.dp))
            TeamScoreRow(label = event.awayTeam, score = event.awayScore, logo = event.awayLogo, isWinning = (event.awayScore?.toIntOrNull() ?: 0) > (event.homeScore?.toIntOrNull() ?: 0))
        }
    }
}

@Composable
private fun TeamScoreRow(label: String, score: String?, logo: String?, isWinning: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
            if (!logo.isNullOrBlank()) {
                AsyncImage(model = logo, contentDescription = null, modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit)
            } else {
                Text(label.take(2).uppercase(), color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (isWinning) TextPrimary else TextSecondary, fontSize = 16.sp,
            fontWeight = if (isWinning) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(score ?: "-", color = if (isWinning) AccentGreen else TextSecondary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun TvStandingsBento(
    standings: List<TeamStanding>,
    onTeamClick: ((String) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CardBg).padding(16.dp)) {
        Text("Standings", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("#", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            Text("TEAM", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("W-L", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        standings.forEachIndexed { index, standing ->
            val isFirst = index == 0
            StandingsRow(standing = standing, rank = index + 1, isFirst = isFirst, onClick = onTeamClick?.let { { it(standing.teamName) } })
            if (index < standings.size - 1) HorizontalDivider(color = TextMuted.copy(alpha = 0.15f), thickness = 0.5.dp)
        }
    }
}

@Composable
private fun StandingsRow(standing: TeamStanding, rank: Int, isFirst: Boolean, onClick: (() -> Unit)?) {
    var isFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (isFirst) CardBgFocused else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text((rank).toString().padStart(2, '0'), color = if (isFirst) Accent else TextMuted, fontSize = 11.sp,
            fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
                if (!standing.logo.isNullOrBlank()) {
                    AsyncImage(model = standing.logo, contentDescription = null, modifier = Modifier.size(18.dp), contentScale = ContentScale.Fit)
                } else {
                    Text(standing.teamName.take(2).uppercase(), color = TextMuted, fontSize = 8.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(standing.teamName, color = TextPrimary, fontSize = 12.sp, fontWeight = if (isFirst) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(standing.record, color = if (isFirst) Accent else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HighlightCard(
    thumbnailUrl: String,
    title: String,
    channelName: String,
    durationSeconds: Int,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(150),
        label = "hlScale",
    )
    Box(
        modifier = Modifier.width(280.dp).aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) CardBgFocused else CardBg)
            .border(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
            if (thumbnailUrl.isNotBlank()) {
                AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            if (durationSeconds > 0) {
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                    val m = durationSeconds / 60; val s = durationSeconds % 60
                    Text("$m:${s.toString().padStart(2, '0')}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(channelName, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

@Composable
fun TvUpcomingCard(event: EspnProcessedEvent, onClick: () -> Unit) {
    DpadCard(onClick = onClick, modifier = Modifier.width(240.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val timeLabel = (event.timeStr ?: "").ifBlank { "TBD" }
            Text(timeLabel, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeamCircle(logo = event.homeLogo, name = event.homeTeam, size = 44)
                Text("VS", color = TextSecondary.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                TeamCircle(logo = event.awayLogo, name = event.awayTeam, size = 44)
            }
            Spacer(Modifier.height(8.dp))
            Text(event.awayTeam.take(24), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TeamCircle(logo: String?, name: String, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(CardBg), contentAlignment = Alignment.Center) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(model = logo, contentDescription = null, modifier = Modifier.size((size - 4).dp), contentScale = ContentScale.Fit)
        } else {
            Text(name.take(2).uppercase(), color = TextMuted, fontSize = (size / 4).sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TvDaddyLiveCard(event: DaddyLiveEvent, onClick: () -> Unit) {
    val isLive = event.isLive
    DpadCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isLive) Accent.copy(alpha = 0.2f) else CardBg), contentAlignment = Alignment.Center) {
                Text(event.category.take(2).uppercase(), color = if (isLive) Accent else TextMuted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(event.eventName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isLive) {
                        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(ErrorRed).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(Color.White))
                                Text("LIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${event.localDate} · ${event.localTime} · ${event.channels.size} channel(s)", color = TextMuted, fontSize = 12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (isLive) Accent else CardBg).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(if (isLive) "▶ WATCH" else "VIEW", color = if (isLive) Color.Black else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
