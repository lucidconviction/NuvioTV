package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.IptvChannel

@Composable
fun ChannelSwitcherPanel(
    channels: List<IptvChannel>,
    currentChannel: IptvChannel?,
    favoriteIds: Set<String>,
    history: List<IptvChannel>,
    showHistory: Boolean,
    onSwitchChannel: (IptvChannel) -> Unit,
    onToggleFavorite: (IptvChannel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(top = 16.dp, bottom = 32.dp)
                .clickable(enabled = false) {}
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showHistory) "Channel History" else "Channel Switcher",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                var closeFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .onFocusChanged { closeFocused = it.isFocused }
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = if (closeFocused) Color.White else Color(0xFFB0B0B0),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF2A2A2A), modifier = Modifier.padding(vertical = 8.dp))

            // Channel list
            val displayChannels = if (showHistory && history.isNotEmpty()) history else channels

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (displayChannels.size > 8) 400.dp else (displayChannels.size * 56).dp.coerceAtMost(400.dp))
            ) {
                itemsIndexed(displayChannels) { index, channel ->
                    val isCurrent = currentChannel?.url == channel.url
                    val isFav = channel.id in favoriteIds
                    var isFocused by remember { mutableStateOf(index == 0) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                            .focusable()
                            .background(
                                when {
                                    isCurrent -> Color(0xFF2E2E2E)
                                    isFocused -> Color(0xFF252525)
                                    else -> Color.Transparent
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .then(
                                if (isFocused) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .clickable { onSwitchChannel(channel) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Channel logo
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Name + current indicator
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Now Playing",
                                        tint = Color(0xFF00FF00),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = channel.name,
                                    color = if (isCurrent) Color(0xFF00FF00) else Color.White,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Favorite toggle
                        var favFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { onToggleFavorite(channel) },
                            modifier = Modifier
                                .onFocusChanged { favFocused = it.isFocused }
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFav) "Unfavorite" else "Favorite",
                                tint = if (isFav) Color.Red else if (favFocused) Color.White else Color(0xFF888888),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ChannelPlaybackState(
    val channels: List<IptvChannel> = emptyList(),
    val currentChannel: IptvChannel? = null,
    val favoriteIds: Set<String> = emptySet(),
    val history: List<IptvChannel> = emptyList(),
    val showChannelSwitcher: Boolean = false,
    val showChannelHistory: Boolean = false,
)
