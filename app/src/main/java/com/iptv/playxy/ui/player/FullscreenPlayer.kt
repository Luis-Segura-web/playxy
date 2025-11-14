package com.iptv.playxy.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Fullscreen player for all content types (Landscape mode)
 * - Video fills entire screen
 * - Controls overlay with auto-hide (5 seconds)
 * - Keep screen on (prevent dimming)
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FullscreenPlayer(
    streamUrl: String,
    title: String,
    playerType: PlayerType,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onPreviousItem: (() -> Unit)? = null,
    onNextItem: (() -> Unit)? = null,
    hasPrevious: Boolean = false,
    hasNext: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isPlaying by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showTrackSelector by remember { mutableStateOf(false) }

    // Listen to player state changes
    DisposableEffect(playerManager) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_IDLE -> { /* Idle */ }
                    Player.STATE_BUFFERING -> { /* Buffering */ }
                    Player.STATE_READY -> hasError = false
                    Player.STATE_ENDED -> hasError = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
                showControls = true
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
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

    // Auto-hide controls after 5 seconds (no ocultar si el selector está abierto)
    LaunchedEffect(showControls, isPlaying, showTrackSelector) {
        if (showControls && isPlaying && !hasError && !showTrackSelector) {
            delay(5000)
            showControls = false
        }
    }

    // Lock to landscape orientation and configure immersive mode
    DisposableEffect(Unit) {
        // Set landscape orientation for fullscreen
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Configure immersive fullscreen mode
        activity?.window?.let { window ->
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            // Configure immersive behavior
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Hide all system bars (status bar + navigation bar)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Keep screen on
            window.decorView.keepScreenOn = true
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Additional fullscreen flags for older Android versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            // Restore system bars
            activity?.window?.let { window ->
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

                // Remove keep screen on
                window.decorView.keepScreenOn = false
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                // Restore normal UI visibility
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    // Player is already initialized and playing, just ensure it's ready
    DisposableEffect(streamUrl) {
        playerManager.initializePlayer()
        // Only play media if URL changed
        val currentMedia = playerManager.getPlayer()?.currentMediaItem?.localConfiguration?.uri.toString()
        if (currentMedia != streamUrl) {
            playerManager.playMedia(streamUrl, playerType)
        }
        isPlaying = playerManager.isPlaying()

        onDispose {
            // Don't release player, just detach
        }
    }

    // Update position and duration periodically
    LaunchedEffect(Unit) {
        while (true) {
            currentPosition = playerManager.getCurrentPosition()
            duration = playerManager.getDuration()
            delay(500)
        }
    }

    // Handle back button
    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (!showTrackSelector) {
                    showControls = !showControls
                }
            }
    ) {
        // Player view - FULL SCREEN
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(true)
                    keepScreenOn = true // Prevent screen dimming
                    // Set player AFTER view is laid out
                    post {
                        player = playerManager.getPlayer()
                    }
                }
            },
            update = { playerView ->
                if (playerView.player != playerManager.getPlayer()) {
                    playerView.player = playerManager.getPlayer()
                }
                playerView.keepScreenOn = true
            },
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay with animation
        AnimatedVisibility(
            visible = (showControls || !isPlaying || hasError),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                val hasTracksAvailable = hasAudioOrSubtitleTracks(playerManager.getPlayer())

                // Top bar with title, back button, and audio/subtitles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 1
                        )
                    }

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
                }

                // Center controls con mensaje de error arriba
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onPreviousItem != null) {
                            IconButton(
                                onClick = { onPreviousItem() },
                                enabled = hasPrevious,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = when(playerType) {
                                        PlayerType.TV -> "Canal anterior"
                                        PlayerType.SERIES -> "Episodio anterior"
                                        else -> "Anterior"
                                    },
                                    tint = if (hasPrevious) Color.White else Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        if (playerType == PlayerType.MOVIE || playerType == PlayerType.SERIES) {
                            IconButton(
                                onClick = { playerManager.seekBackward(10_000) },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Retroceder 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                if (isPlaying) { playerManager.pause(); isPlaying = false } else { playerManager.play(); isPlaying = true }
                                showControls = true
                            },
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        if (playerType == PlayerType.MOVIE || playerType == PlayerType.SERIES) {
                            IconButton(
                                onClick = { playerManager.seekForward(10_000) },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward10,
                                    contentDescription = "Avanzar 10s",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onNextItem != null) {
                            IconButton(
                                onClick = { onNextItem() },
                                enabled = hasNext,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = when(playerType) {
                                        PlayerType.TV -> "Canal siguiente"
                                        PlayerType.SERIES -> "Episodio siguiente"
                                        else -> "Siguiente"
                                    },
                                    tint = if (hasNext) Color.White else Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                if (hasError) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Contenido no disponible",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        OutlinedButton(onClick = {
                            hasError = false
                            playerManager.playMedia(streamUrl, playerType)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reintentar",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }

                // Bottom controls with seek bar (Movies/Series only)
                if (playerType == PlayerType.MOVIE && duration > 0) {
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
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // Time indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = formatTime(duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SEEKBAR DELGADO Y ESTILIZADO
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = { newValue ->
                                playerManager.seekTo(newValue.toLong())
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp) // Más delgado
                        )
                    }
                }


            }
        }

        // Track Selector Dialog SIEMPRE por encima (no depende de showControls)
        if (showTrackSelector) {
            TrackSelectorDialog(
                player = playerManager.getPlayer(),
                onDismiss = { showTrackSelector = false }
            )
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
