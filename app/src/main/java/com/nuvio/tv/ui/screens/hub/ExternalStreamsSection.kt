package com.robbdeeze.nuviotv.ui.screens.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.onFocusChanged
import com.robbdeeze.nuviotv.data.remote.api.ExternalStreamMatch
import com.robbdeeze.nuviotv.data.remote.api.ExternalStreamsClient

@Composable
fun ExternalStreamsSection(
    matches: List<ExternalStreamMatch>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onStreamClick: ((ExternalStreamMatch) -> Unit)? = null
) {
    if (matches.isEmpty() && !isLoading) return

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "External Streams",
                color = Color(0xFFc1c7d2),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onRefresh,
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF4A90D9),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF4A90D9)
                )
            } else {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4A90D9).copy(alpha = 0.2f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${matches.size}", color = Color(0xFF4A90D9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Stream grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(matches, key = { it.id }) { match ->
                var isFocused by remember { mutableStateOf(false) }
                GlassStreamCard(
                    match = match,
                    isFocused = isFocused,
                    onFocusedChange = { isFocused = it },
                    onClick = { onStreamClick?.invoke(match) }
                )
            }
        }
    }
}

@Composable
private fun GlassStreamCard(
    match: ExternalStreamMatch,
    isFocused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isFocused) 0.12f else 0.06f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = if (isFocused) 0.5f else 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .onFocusChanged { onFocusedChange(it.isFocused) }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Category tag
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF4A90D9).copy(alpha = 0.2f),
                        androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    match.category.replaceFirstChar { it.uppercase() },
                    color = Color(0xFF4A90D9),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Title
            Text(
                match.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                textAlign = TextAlign.Center
            )

            // Quality badge + icon row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (match.popular) "🔥 Popular" else "HD",
                    color = Color(0xFF888888),
                    fontSize = 10.sp
                )
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF4A90D9),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}