package com.iptv.playxy.ui.tv.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.PlayerState

@Composable
fun MiniPlayerView(
    channel: LiveStream?,
    streamUrl: String,
    state: PlayerState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (channel == null) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        // Video player
        VLCPlayer(
            url = streamUrl,
            modifier = Modifier.fillMaxSize(),
            onBuffering = { /* Buffering state is already managed by state parameter */ },
            onPlaying = { /* Playing state is already managed by state parameter */ },
            onError = { /* Error state is already managed by state parameter */ }
        )

        // Overlay controls
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Close button (top-right)
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Playback controls (center)
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = when (state) {
                            is PlayerState.Playing -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (state is PlayerState.Playing) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(64.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Fullscreen button (bottom-right)
            IconButton(
                onClick = onFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Pantalla completa",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Buffering indicator
            if (state is PlayerState.Buffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                )
            }

            // Error message
            if (state is PlayerState.Error) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
