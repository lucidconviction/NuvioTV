package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalEvent
import com.robbdeeze.nuviotv.data.remote.api.Sync2CalTvChannel

@Composable
fun Sync2CalUpcomingSection(
    sync2CalEventsByLeague: Map<String, List<Sync2CalEvent>>,
    sync2CalTvChannels: Map<Long, List<Sync2CalTvChannel>>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    val allEvents = sync2CalEventsByLeague.values.flatten().sortedBy { it.startTime }.take(20)
    if (allEvents.isEmpty() && !isLoading) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Upcoming Schedule", color = Color(0xFFa0caff), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFa0caff).copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${allEvents.size} EVENTS", color = Color(0xFFa0caff), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            var refreshFocused by remember { mutableStateOf(false) }
            Card(
                onClick = onRefresh,
                colors = CardDefaults.cardColors(containerColor = if (refreshFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
                border = BorderStroke(if (refreshFocused) 1.dp else 0.dp, if (refreshFocused) Color.White else Color.Transparent),
                modifier = Modifier.onFocusChanged { refreshFocused = it.isFocused }
            ) {
                Text("SYNC2CAL", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            items(allEvents.take(12), key = { it.id }) { event ->
                Sync2CalEventCard(
                    event = event,
                    tvChannels = sync2CalTvChannels[event.id] ?: emptyList(),
                )
            }
        }
    }
}

@Composable
private fun Sync2CalEventCard(
    event: Sync2CalEvent,
    tvChannels: List<Sync2CalTvChannel>,
) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
        border = BorderStroke(if (isFocused) 2.dp else 0.dp, if (isFocused) Color.White else Color.Transparent),
        modifier = Modifier.width(200.dp).onFocusChanged { isFocused = it.isFocused }
    ) {
        Column(Modifier.padding(14.dp)) {
            val timeParts = event.startTime.split("T")
            val dateStr = timeParts.getOrNull(0)?.let { formatSync2CalDate(it) } ?: "TBD"
            val timeStr = timeParts.getOrNull(1)?.let { formatSync2CalTime(it) } ?: ""

            Text(dateStr, color = Color(0xFFa0caff), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (timeStr.isNotBlank()) {
                Text(timeStr, color = Color(0xFF888888), fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(event.title, color = if (isFocused) Color.White else Color(0xFFc1c7d2), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (tvChannels.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("TV: ${tvChannels.take(3).joinToString(", ") { it.name }}", color = Color(0xFF4A90D9), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tvChannels.size > 3) {
                    Text("+${tvChannels.size - 3} more", color = Color(0xFF666666), fontSize = 9.sp)
                }
            }
        }
    }
}

private fun formatSync2CalDate(isoDate: String): String {
    return try {
        val parts = isoDate.split("-")
        val y = parts.getOrNull(0) ?: return isoDate
        val m = parts.getOrNull(1) ?: return isoDate
        val d = parts.getOrNull(2)?.substringBefore("T")?.substringBefore(" ") ?: return isoDate
        val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthIdx = m.toIntOrNull() ?: return "$m/$d"
        val monthName = monthNames.getOrElse(monthIdx) { m }
        "$monthName $d"
    } catch (_: Exception) { isoDate }
}

private fun formatSync2CalTime(isoTime: String): String {
    return try {
        val clean = isoTime.substringBefore("Z").substringBefore("+").substringBefore("-")
        val parts = clean.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return isoTime
        val m = parts.getOrNull(1) ?: "00"
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        "$h12:$m $amPm"
    } catch (_: Exception) { isoTime }
}
