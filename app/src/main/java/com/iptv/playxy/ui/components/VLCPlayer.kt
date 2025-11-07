package com.iptv.playxy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * VLC Player Component (Placeholder)
 * 
 * This component will be used to play IPTV streams using VLC SDK
 * To be implemented in a future phase
 * 
 * @param streamUrl The URL of the stream to play
 * @param modifier Modifier for the composable
 */
@Composable
fun VLCPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
            Text(
                text = "VLC Player",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "En desarrollo",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
