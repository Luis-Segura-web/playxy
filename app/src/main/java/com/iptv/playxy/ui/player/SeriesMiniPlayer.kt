
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mini player for Series Episodes (Portrait mode)
 * Controls: Previous Episode, Pause/Play, Next Episode, Close, Fullscreen
 */
@Composable
fun SeriesMiniPlayer(
    streamUrl: String,
    episodeTitle: String,
    seasonNumber: Int,
    episodeNumber: Int,
    playerManager: PlayerManager,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {},
    hasPrevious: Boolean = true,
    hasNext: Boolean = true
) {
    val uiState = rememberPlayerUiState(playerManager)
    var showControls by remember { mutableStateOf(true) }
    var showTrackSelector by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    val logTag = "SeriesMiniPlayer"
    val scope = rememberCoroutineScope()

    // Auto-hide controls after 5 seconds (pero NO si el diálogo está abierto)
    LaunchedEffect(showControls, uiState.isPlaying, uiState.hasError, showTrackSelector) {
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
            Log.d(logTag, "playMedia diferido (Series) URL=$streamUrl")
            playerManager.playMedia(streamUrl, PlayerType.SERIES)
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
                showControls = !showControls
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

                // Top bar with episode info and CLOSE button (TOP RIGHT)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "T$seasonNumber E$episodeNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    // CLOSE button - TOP RIGHT
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

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
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousEpisode,
                            enabled = hasPrevious,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Default.SkipPrevious, contentDescription = "Episodio anterior", tint = if (hasPrevious) Color.White else Color.Gray, modifier = Modifier.size(36.dp)) }

                        if (uiState.hasError) {
                            OutlinedButton(onClick = {
                                uiState.hasError = false
                                scope.launch {
                                    delay(100)
                                    playerManager.playMedia(streamUrl, PlayerType.SERIES)
                                }
                            }) {
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
                                modifier = Modifier.size(64.dp)
                            ) { Icon(if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (uiState.isPlaying) "Pausar" else "Reproducir", tint = Color.White, modifier = Modifier.size(48.dp)) }
                        }

                        IconButton(
                            onClick = onNextEpisode,
                            enabled = hasNext,
                            modifier = Modifier.size(48.dp)
                        ) { Icon(Icons.Default.SkipNext, contentDescription = "Episodio siguiente", tint = if (hasNext) Color.White else Color.Gray, modifier = Modifier.size(36.dp)) }
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
                            showControls = true
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
            }
        }
    }
}
