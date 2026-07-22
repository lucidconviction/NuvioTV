package com.robbdeeze.nuviotv.ui.screens.multi

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robbdeeze.nuviotv.domain.model.IptvChannel

@Composable
fun MultiWindowSlotPicker(
    channel: IptvChannel,
    onDismiss: () -> Unit,
    swapMode: Boolean = false,
    swapSourceIndex: Int = -1,
    onSwapTarget: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val store = MultiWindowStore
    val currentStreams = store.streams.toList()
    val totalSlots = MultiWindowStore.MAX_PLAYERS

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            if (swapMode) "Swap: pick target slot" else "Pick Slot: ${channel.name}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val cols = 3
        val rows = (totalSlots + cols - 1) / cols

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until cols) {
                    val slotIndex = row * cols + col
                    if (slotIndex < totalSlots) {
                        if (swapMode) {
                            SwapSlotCell(
                                slotIndex = slotIndex,
                                occupiedBy = currentStreams.find { it.slotIndex == slotIndex },
                                isSource = slotIndex == swapSourceIndex,
                                onClick = {
                                    onSwapTarget?.invoke(slotIndex)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SlotCell(
                                slotIndex = slotIndex,
                                occupiedBy = currentStreams.find { it.slotIndex == slotIndex },
                                onClick = {
                                    store.addToSlot(channel, slotIndex)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SlotCell(
    slotIndex: Int,
    occupiedBy: WindowStream?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> Color(0xFF2E2E2E)
                    occupiedBy != null -> Color(0xFF1A2A1A)
                    else -> Color(0xFF1A1A1A)
                }
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = when {
                    isFocused -> Color.White
                    occupiedBy != null -> Color(0xFF4A90D9).copy(alpha = 0.5f)
                    else -> Color(0xFF333333)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Slot ${slotIndex + 1}",
                color = if (occupiedBy != null) Color(0xFF4A90D9) else Color(0xFF666666),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (occupiedBy != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    occupiedBy.channel.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Empty",
                    color = Color(0xFF444444),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun SwapSlotCell(
    slotIndex: Int,
    occupiedBy: WindowStream?,
    isSource: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "swapPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor = when {
        isSource -> Color(0xFF00FF00)
        isFocused -> Color.White
        occupiedBy != null -> Color(0xFF4A90D9).copy(alpha = pulseAlpha)
        else -> Color(0xFF444444)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSource -> Color(0xFF1A3A1A)
                    isFocused -> Color(0xFF2E2E2E)
                    occupiedBy != null -> Color(0xFF1A1A1A)
                    else -> Color(0xFF111111)
                }
            )
            .border(
                width = if (isFocused || isSource) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (isSource) "Source" else "Slot ${slotIndex + 1}",
                color = if (isSource) Color(0xFF00FF00) else if (occupiedBy != null) Color(0xFF4A90D9) else Color(0xFF666666),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (occupiedBy != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    occupiedBy.channel.name,
                    color = Color.White,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isSource) "You" else "Empty",
                    color = Color(0xFF444444),
                    fontSize = 10.sp
                )
            }
        }
    }
}
