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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mini player for TV Channels (Portrait mode)
 * Controls: Previous Channel, Pause/Play, Next Channel, Close, Fullscreen
 */
@Composable
fun TVMiniPlayer(
    streamUrl: String,
    channelName: String,
    playerManager: PlayerManager,
    onPreviousChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val uiState = rememberPlayerUiState(playerManager)
    var showControls by remember { mutableStateOf(true) }
    var showTrackSelector by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    var lastPlayedUrl by remember { mutableStateOf<String?>(null) }
    val logTag = "TVMiniPlayer"

    // Auto-hide controls after 5 seconds (pero NO si el diálogo está abierto)
    LaunchedEffect(showControls, uiState.isPlaying, uiState.hasError, showTrackSelector) {
        if (showControls && uiState.isPlaying && !uiState.hasError && !showTrackSelector) {
            delay(5000)
            showControls = false
        }
    }

    // Inicializar player (no reproducir aún hasta que el PlayerView exista)
    LaunchedEffect(Unit) { playerManager.initializePlayer() }

    // Reproducir cuando el PlayerView esté listo y la URL cambie realmente
    LaunchedEffect(streamUrl, playerViewReady) {
        if (playerViewReady && streamUrl.isNotBlank() && streamUrl != lastPlayedUrl) {
            Log.d(logTag, "MiniPlayer: start playback URL=$streamUrl (surface ready)")
            lastPlayedUrl = streamUrl
            playerManager.playMedia(streamUrl, PlayerType.TV)
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
                if (!showTrackSelector) {
                    showControls = !showControls
                }
            }
    ) {
        // Player view - FULL SIZE
        PlayerVideoSurface(
            streamKey = streamUrl,
            modifier = Modifier.fillMaxSize(),
            playerManager = playerManager,
            onPlayerReady = { playerViewReady = true }
        )

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

                // Top bar with channel name and CLOSE button (TOP RIGHT)
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
                        text = channelName,
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
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Channel (ya no se deshabilita en error)
                        IconButton(
                            onClick = onPreviousChannel,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Canal anterior",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Botón Reintentar (solo en error)
                        if (uiState.hasError) {
                            OutlinedButton(onClick = {
                                uiState.hasError = false
                                scope.launch {
                                    delay(100)
                                    playerManager.playMedia(streamUrl, PlayerType.TV)
                                }
                            }, modifier = Modifier.height(56.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reintentar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Reintentar")
                            }
                        } else {
                            // Play/Pause siempre visible (no se reemplaza por retry)
                            IconButton(
                                onClick = {
                                    if (uiState.isPlaying) {
                                        playerManager.pause(); uiState.isPlaying = false
                                    } else {
                                        playerManager.play(); uiState.isPlaying = true
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

                        // Next Channel (ya no se deshabilita en error)
                        IconButton(
                            onClick = onNextChannel,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Canal siguiente",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
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
            }
        }

        // Spinner mientras buffering sin primer frame
        if (uiState.isBuffering && !uiState.firstFrameRendered) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
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
