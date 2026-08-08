package com.robbdeeze.nuviotv.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.QuickChannel
import com.robbdeeze.nuviotv.data.iptv.QuickChannelList

@Composable
fun ChannelHistoryRowSection(
    channels: List<IptvChannel>,
    onChannelClick: (IptvChannel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "IPTV Channel History",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(channels, key = { it.id }) { channel ->
                IptvMiniCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
fun QuickChannelsRowSection(
    onQuickChannelClick: (String) -> Unit,
) {
    val allQuickChannels = QuickChannelList.all
    var selectedRegion by remember { mutableStateOf("All") }
    val regionTabs = listOf("All", "US", "UK", "CA", "Premium", "Bay Area", "PPV Events", "Sports", "News")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Channels",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp)
        ) {
            items(regionTabs) { tab ->
                var tabFocused by remember { mutableStateOf(false) }
                val isActive = selectedRegion == tab
                Card(
                    onClick = { selectedRegion = tab },
                    colors = CardDefaults.colors(
                        containerColor = if (isActive) Color(0xFF4A90D9) else if (tabFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)
                    ),
                    shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
                    border = CardDefaults.border(
                        focusedBorder = if (tabFocused) Border(BorderStroke(2.dp, Color.White)) else Border.None
                    ),
                    modifier = Modifier.height(30.dp).onFocusChanged { tabFocused = it.isFocused }
                ) {
                    Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text(tab, color = if (isActive) Color.White else Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val filtered = allQuickChannels.filter { qc ->
            when (selectedRegion) {
                "All" -> true
                "US" -> "US" in qc.regions
                "UK" -> "UK" in qc.regions
                "CA" -> "CA" in qc.regions
                "Premium" -> "premium" in qc.tags
                "Bay Area" -> "bay-area" in qc.regions
                "PPV Events" -> "ppv" in qc.tags || qc.displayName.contains("ppv", true) || qc.displayName.contains("pay per view", true) || qc.aliases.any { it.contains("ppv", true) || it.contains("box office", true) }
                "Sports" -> "sports" in qc.tags
                "News" -> "news" in qc.tags
                else -> true
            }
        }
        if (filtered.isEmpty()) {
            Text(
                "No quick channels for this region",
                color = Color(0xFF666666),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filtered, key = { it.displayName }) { qc ->
                    QuickChannelCard(
                        quickChannel = qc,
                        onClick = { onQuickChannelClick(qc.displayName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IptvMiniCard(
    channel: IptvChannel,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = if (isFocused) Border(BorderStroke(2.dp, Color.White)) else Border.None
        ),
        modifier = Modifier.width(140.dp).height(90.dp).onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            Box(
                Modifier.fillMaxSize().align(Alignment.BottomCenter).height(50.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )
            Text(
                channel.name,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
            )
        }
    }
}

@Composable
private fun QuickChannelCard(
    quickChannel: QuickChannel,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        colors = CardDefaults.colors(
            containerColor = if (isFocused) Color(0xFF2E2E2E) else Color(0xFF1A1A1A)
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp)),
        border = CardDefaults.border(
            focusedBorder = if (isFocused) Border(BorderStroke(2.dp, Color.White)) else Border.None
        ),
        modifier = Modifier.width(150.dp).height(70.dp).onFocusChanged { isFocused = it.isFocused }
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    quickChannel.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                val tag = quickChannel.tags.firstOrNull()
                if (tag != null) {
                    val label = when (tag) {
                        "region" -> quickChannel.regions.joinToString("/")
                        "sports" -> "SPORTS"
                        else -> tag.uppercase()
                    }
                    Text(
                        label,
                        color = if (tag == "sports") Color(0xFFE8553A) else if (tag == "region") Color(0xFF4ADE80) else Color(0xFF4A90D9),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
