
package com.iptv.playxy.ui.player

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mini player for Movies (Portrait mode)
 * Controls: Seek Bar, Pause/Play, Close, Fullscreen
 */
@Composable
fun MovieMiniPlayer(
    streamUrl: String,
    movieTitle: String,
    playerManager: PlayerManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {}
) {
    val uiState = rememberPlayerUiState(playerManager)
    var showControls by remember { mutableStateOf(true) }
    var showTrackSelector by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val logTag = "MovieMiniPlayer"
    val scope = rememberCoroutineScope()

    // Auto-hide controls after 5 seconds (pero NO si el diálogo está abierto)
    LaunchedEffect(showControls, uiState.isPlaying, showTrackSelector, uiState.hasError) {
        if (showControls && uiState.isPlaying && !uiState.hasError && !showTrackSelector) {
            delay(5000)
            showControls = false
        }
    }

    // Initialize player BEFORE creating the view
    LaunchedEffect(Unit) {
        playerManager.initializePlayer()
    }

    LaunchedEffect(streamUrl, playerViewReady) {
        if (playerViewReady) {
            Log.d(logTag, "playMedia diferido (Movie) URL=$streamUrl")
            playerManager.playMedia(streamUrl, PlayerType.MOVIE)
        }
    }

    // Update position and duration periodically
    LaunchedEffect(playerManager) {
        while (isActive) {
            currentPosition = playerManager.getCurrentPosition()
            duration = playerManager.getDuration()
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!showTrackSelector) showControls = !showControls
            }
    ) {
        key(streamUrl) {
            PlayerVideoSurface(
                streamKey = streamUrl,
                modifier = Modifier.fillMaxSize(),
                playerManager = playerManager,
                onPlayerReady = { playerViewReady = true }
            )
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls || !uiState.isPlaying || uiState.hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Semi-transparent background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Top bar with movie title and CLOSE button (TOP RIGHT)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = movieTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    // CLOSE button - TOP RIGHT
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                // Center controls con mensaje de error arriba
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.hasError) {
                        Text(
                            text = "Contenido no disponible",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playerManager.seekBackward(10000) },
                            modifier = Modifier.size(56.dp)
                        ) { Icon(Icons.Default.Replay10, contentDescription = "Retroceder 10s", tint = Color.White, modifier = Modifier.size(40.dp)) }

                        if (uiState.hasError) {
                            OutlinedButton(onClick = {
                                uiState.hasError = false
                                scope.launch {
                                    delay(100)
                                    playerManager.playMedia(streamUrl, PlayerType.MOVIE)
                                }
                            }, modifier = Modifier.height(56.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reintentar", tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Reintentar")
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (uiState.isPlaying) {
                                        playerManager.pause()
                                    } else {
                                        playerManager.play()
                                    }
                                    showControls = true
                                },
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir",
                                    tint = Color.White,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { playerManager.seekForward(10000) },
                            modifier = Modifier.size(56.dp)
                        ) { Icon(Icons.Default.Forward10, contentDescription = "Avanzar 10s", tint = Color.White, modifier = Modifier.size(40.dp)) }
                    }
                }

                // Bottom right buttons (Audio/Subtitles + Fullscreen)
                val hasTracksAvailable = hasAudioOrSubtitleTracks(playerManager.getPlayer())

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Audio/Subtitles button - only if tracks available
                    if (hasTracksAvailable) {
                        IconButton(onClick = {
                            showTrackSelector = true
                            showControls = true // Mantener controles visibles
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Audio y Subtítulos",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // FULLSCREEN button
                    IconButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Pantalla completa",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Track Selector Dialog
                if (showTrackSelector) {
                    TrackSelectorDialog(
                        player = playerManager.getPlayer(),
                        onDismiss = { showTrackSelector = false }
                    )
                }

                // Bottom seek bar and time
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(bottom = 40.dp) // Space for fullscreen button
                ) {
                    // Time indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }

                    // Seek bar - Con área táctil aumentada
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp), // Aumenta el área táctil verticalmente
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = currentPosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                            onValueChange = { playerManager.seekTo(it.toLong()) },
                            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


            }
        }

        if (uiState.isBuffering && !uiState.firstFrameRendered) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        if (showTrackSelector) {
            TrackSelectorDialog(player = playerManager.getPlayer(), onDismiss = { showTrackSelector = false })
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val locale = java.util.Locale.getDefault()
    return if (hours > 0) {
        String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(locale, "%02d:%02d", minutes, seconds)
    }
}
