package com.iptv.playxy.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    var showTrackDialog by remember { mutableStateOf(false) }
    val playerContainer = LocalPlayerContainerHost.current

    BackHandler { onBack() }

    LaunchedEffect(streamUrl, playerType) {
        if (playbackState.streamUrl != streamUrl) {
            playerManager.playMedia(streamUrl, playerType)
        }
    }

    ImmersiveMode()

    playerContainer(
        PlayerContainerConfig(
            state = playbackState,
            modifier = modifier.fillMaxSize(),
            controlsLocked = showTrackDialog,
            overlay = { state, _, setControlsVisible ->
                FullscreenOverlay(
                    state = state,
                    title = title,
                    playerType = playerType,
                    hasTrackOptions = state.tracks.hasDialogOptions,
                    hasProgress = playerType != PlayerType.TV,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    onBack = {
                        onBack()
                        setControlsVisible(true)
                    },
                    onShowTracks = {
                        setControlsVisible(true)
                        showTrackDialog = true
                    },
                    onTogglePlay = {
                        if (state.isPlaying) playerManager.pause() else playerManager.play()
                    },
                    onSeekBack = { playerManager.seekBackward() },
                    onSeekForward = { playerManager.seekForward() },
                    onRetry = { playerManager.playMedia(streamUrl, playerType, forcePrepare = true) },
                    onSeek = { position -> playerManager.seekTo(position) },
                    onPrevious = onPreviousItem,
                    onNext = onNextItem,
                    enablePrevious = hasPrevious,
                    enableNext = hasNext
                )
            }
        )
    )

    if (showTrackDialog && playbackState.tracks.hasDialogOptions) {
        TrackSelectionDialog(
            tracks = playbackState.tracks,
            onDismiss = { showTrackDialog = false },
            onAudioSelected = { option -> playerManager.selectAudioTrack(option.id) },
            onSubtitleSelected = { option ->
                if (option == null) playerManager.disableSubtitles() else playerManager.selectSubtitleTrack(option.id)
            }
        )
    }
}

@Composable
private fun FullscreenOverlay(
    state: PlaybackUiState,
    title: String,
    playerType: PlayerType,
    hasTrackOptions: Boolean,
    hasProgress: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onShowTracks: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onRetry: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    enablePrevious: Boolean,
    enableNext: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (hasTrackOptions) {
                    IconButton(onClick = onShowTracks) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Pistas", tint = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.hasError) {
                Text(
                    text = state.errorMessage ?: "Contenido no disponible",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                FilledTonalButton(onClick = onRetry) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text(text = "Reintentar", modifier = Modifier.padding(start = 8.dp))
                }
            }

            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onPrevious != null) {
                    IconButton(onClick = onPrevious, enabled = enablePrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = if (enablePrevious) Color.White else Color.Gray
                        )
                    }
                }
                if (playerType != PlayerType.TV) {
                    IconButton(onClick = onSeekBack) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Retroceder", tint = Color.White)
                    }
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White
                    )
                }
                if (playerType != PlayerType.TV) {
                    IconButton(onClick = onSeekForward) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Avanzar", tint = Color.White)
                    }
                }
                if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onNext != null) {
                    IconButton(onClick = onNext, enabled = enableNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = if (enableNext) Color.White else Color.Gray
                        )
                    }
                }
            }
        }

        if (hasProgress) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                PlaybackProgress(
                    state = state,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ImmersiveMode() {
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
        }

        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val disposeWindow = activity?.window
            if (disposeWindow != null) {
                val controller = WindowCompat.getInsetsController(disposeWindow, disposeWindow.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                disposeWindow.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                disposeWindow.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}
