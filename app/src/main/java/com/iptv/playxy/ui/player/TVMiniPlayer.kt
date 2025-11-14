@file:OptIn(androidx.media3.common.util.UnstableApi::class)
@file:androidx.media3.common.util.UnstableApi

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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
    val uiState = remember { PlayerUiState() }
    var showControls by remember { mutableStateOf(true) }
    var playerReady by remember { mutableStateOf(false) }
    var playerViewReady by remember { mutableStateOf(false) }
    val logTag = "TVMiniPlayer"

    // Auto-hide controls after 5 seconds (pero NO si el diálogo está abierto)
    LaunchedEffect(showControls, uiState.isPlaying, uiState.hasError) {
        if (showControls && uiState.isPlaying && !uiState.hasError) {
            delay(5000)
            showControls = false
        }
    }

    // Registrar frame listener
    DisposableEffect(playerManager) {
        val frameListener: (Boolean) -> Unit = { rendered ->
            uiState.firstFrameRendered = rendered
            uiState.isBuffering = !rendered
        }
        playerManager.addFrameListener(frameListener)
        onDispose { playerManager.removeFrameListener(frameListener) }
    }

    // Listen to player state changes
    DisposableEffect(playerManager) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                uiState.isBuffering = playbackState == Player.STATE_BUFFERING && !uiState.firstFrameRendered
                if (playbackState == Player.STATE_READY) uiState.hasError = false
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(logTag, "PlayerError ${error.errorCodeName}: ${error.message}")
                uiState.hasError = true
                showControls = true
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                uiState.isPlaying = playing
                if (!playing) {
                    showControls = true
                }
            }
        }
        playerManager.getPlayer()?.addListener(listener)
        onDispose {
            playerManager.getPlayer()?.removeListener(listener)
        }
    }

    // Inicializar player (no reproducir aún hasta que el PlayerView exista)
    LaunchedEffect(Unit) { playerManager.initializePlayer() }

    // Reproducir cuando el PlayerView esté listo y cambie la URL
    LaunchedEffect(streamUrl, playerViewReady) {
        if (playerViewReady) {
            Log.d(logTag, "playMedia diferido hasta tener surface URL=$streamUrl")
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
                showControls = !showControls
            }
    ) {
        // Player view - FULL SIZE
        // Key basado en streamUrl + playerReady para forzar recreación cuando sea necesario
        key(streamUrl, playerReady) {
            AndroidView(
                factory = { ctx ->
                    Log.d(logTag, "Creando PlayerView (factory)")
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setKeepContentOnPlayerReset(false) // para no mantener frame anterior al cambiar canal
                        keepScreenOn = true
                        // Conectar player inmediatamente si está disponible
                        playerManager.getPlayer()?.let {
                            Log.d(logTag, "Conectando player en factory")
                            player = it
                        }
                    }
                },
                update = { view ->
                    if (!playerViewReady) playerViewReady = true
                    val currentPlayer = playerManager.getPlayer()
                    if (currentPlayer != null && view.player != currentPlayer) {
                        Log.d(logTag, "Reconectando player en update")
                        view.player = currentPlayer
                    }
                    view.keepScreenOn = true
                },
                modifier = Modifier.fillMaxSize()
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

                // Spinner mientras buffering sin primer frame
                if (uiState.isBuffering && !uiState.firstFrameRendered) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
                }

                // Track Selector Dialog
                if (showControls) {
                    TrackSelectorDialog(
                        player = playerManager.getPlayer(),
                        onDismiss = { showControls = false }
                    )
                }
            }
        }
    }
}
