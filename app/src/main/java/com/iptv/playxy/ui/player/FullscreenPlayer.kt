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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.iptv.playxy.ui.LocalPipController

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
    var trackDialogTab by remember { mutableStateOf<TrackSelectionTab?>(null) }
    var showFitDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var orientationMode by rememberSaveable { mutableStateOf(FullscreenOrientationMode.Auto) }
    val playerContainer = LocalPlayerContainerHost.current
    val pipController = LocalPipController.current

    BackHandler { onBack() }

    LaunchedEffect(streamUrl, playerType) {
        if (playbackState.streamUrl != streamUrl) {
            playerManager.playMedia(streamUrl, playerType)
        }
    }

    ImmersiveMode(enabled = true, orientationMode = orientationMode)

    playerContainer(
        PlayerContainerConfig(
            state = playbackState,
            modifier = modifier.fillMaxSize(),
            controlsLocked = showTrackDialog || showFitDialog || showEngineDialog,
            overlay = { state, _, setControlsVisible ->
                FullscreenOverlay(
                    state = state,
                    title = title,
                    playerType = playerType,
                    hasProgress = playerType != PlayerType.TV,
                    hasPrevious = hasPrevious,
                    hasNext = hasNext,
                    orientationMode = orientationMode,
                    onBack = {
                        onBack()
                        setControlsVisible(true)
                    },
                    onShowAudioTracks = {
                        setControlsVisible(true)
                        trackDialogTab = TrackSelectionTab.Audio
                        showTrackDialog = true
                    },
                    onShowSubtitleTracks = {
                        setControlsVisible(true)
                        trackDialogTab = TrackSelectionTab.Subtitles
                        showTrackDialog = true
                    },
                    onShowFit = {
                        setControlsVisible(true)
                        showFitDialog = true
                    },
                    onShowEngineSettings = {
                        setControlsVisible(true)
                        showEngineDialog = true
                    },
                    onOrientationModeChange = { newMode ->
                        setControlsVisible(true)
                        orientationMode = newMode
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
                    enableNext = hasNext,
                    onPip = { pipController.requestPip(onClose = onBack) }
                )
            }
        )
    )

    if (showTrackDialog && playbackState.tracks.hasDialogOptions) {
        TrackSelectionDialog(
            tracks = playbackState.tracks,
            onDismiss = {
                showTrackDialog = false
                trackDialogTab = null
            },
            onAudioSelected = { option -> playerManager.selectAudioTrack(option.id) },
            onSubtitleSelected = { option ->
                if (option == null) playerManager.disableSubtitles() else playerManager.selectSubtitleTrack(option.id)
            },
            initialTab = trackDialogTab
        )
    }

    if (showFitDialog) {
        VideoFitDialog(
            selectedScale = playerManager.getVideoScaleType(),
            onDismiss = { showFitDialog = false },
            onScaleSelected = { scaleType -> playerManager.setVideoScaleType(scaleType) },
            immersive = true
        )
    }

    if (showEngineDialog) {
        PlayerEngineSettingsDialog(
            initialConfig = playerManager.getEngineConfig(),
            onDismiss = { showEngineDialog = false },
            onApply = { config -> playerManager.setEngineConfig(config) },
            immersive = true
        )
    }
}

@Composable
internal fun FullscreenOverlay(
    state: PlaybackUiState,
    title: String,
    playerType: PlayerType,
    hasProgress: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    orientationMode: FullscreenOrientationMode,
    onBack: () -> Unit,
    onShowAudioTracks: () -> Unit,
    onShowSubtitleTracks: () -> Unit,
    onShowFit: () -> Unit,
    onShowEngineSettings: () -> Unit,
    onOrientationModeChange: (FullscreenOrientationMode) -> Unit,
    onTogglePlay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onRetry: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    enablePrevious: Boolean,
    enableNext: Boolean,
    onPip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        var orientationMenuExpanded by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                Row {
                    val showAudio = state.tracks.audio.size > 1
                    val showSubtitles = state.tracks.text.size > 1
                    if (showAudio) {
                        IconButton(onClick = onShowAudioTracks) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Audio", tint = Color.White)
                        }
                    }
                    if (showSubtitles) {
                        IconButton(onClick = onShowSubtitleTracks) {
                            Icon(
                                imageVector = Icons.Default.ClosedCaption,
                                contentDescription = "Subtítulos",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onShowFit) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Ajuste de pantalla",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onShowEngineSettings) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Motor de reproducci\u00f3n",
                            tint = Color.White
                        )
                    }
                    Box {
                        val orientationIcon = when (orientationMode) {
                            FullscreenOrientationMode.Horizontal -> Icons.Default.StayCurrentLandscape
                            FullscreenOrientationMode.Vertical -> Icons.Default.StayCurrentPortrait
                            FullscreenOrientationMode.Auto -> Icons.Default.ScreenRotation
                        }
                        IconButton(onClick = { orientationMenuExpanded = true }) {
                            Icon(
                                imageVector = orientationIcon,
                                contentDescription = "Orientación",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = orientationMenuExpanded,
                            onDismissRequest = { orientationMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Automático") },
                                leadingIcon = {
                                    if (orientationMode == FullscreenOrientationMode.Auto) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    orientationMenuExpanded = false
                                    onOrientationModeChange(FullscreenOrientationMode.Auto)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Horizontal") },
                                leadingIcon = {
                                    if (orientationMode == FullscreenOrientationMode.Horizontal) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    orientationMenuExpanded = false
                                    onOrientationModeChange(FullscreenOrientationMode.Horizontal)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Vertical") },
                                leadingIcon = {
                                    if (orientationMode == FullscreenOrientationMode.Vertical) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    orientationMenuExpanded = false
                                    onOrientationModeChange(FullscreenOrientationMode.Vertical)
                                }
                            )
                        }
                    }
                    IconButton(onClick = onPip) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (state.hasError) {
            Surface(
                color = Color.Red.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
            ) {
                Text(
                    text = "Error: ${state.errorMessage ?: "Sin código"}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hasProgress) {
                    PlaybackProgress(
                        state = state,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onPrevious != null) {
                        IconButton(
                            onClick = onPrevious,
                            enabled = enablePrevious,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = if (enablePrevious) Color.White else Color.Gray
                            )
                        }
                    }
                    if (playerType != PlayerType.TV) {
                        IconButton(
                            onClick = onSeekBack,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Replay10, contentDescription = "Retroceder", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = { if (state.hasError) onRetry() else onTogglePlay() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.hasError) Icons.Default.Refresh else if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.hasError) "Reintentar" else if (state.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White
                        )
                    }
                    if (playerType != PlayerType.TV) {
                        IconButton(
                            onClick = onSeekForward,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Forward10, contentDescription = "Avanzar", tint = Color.White)
                        }
                    }
                    if ((playerType == PlayerType.TV || playerType == PlayerType.SERIES) && onNext != null) {
                        IconButton(
                            onClick = onNext,
                            enabled = enableNext,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .padding(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = if (enableNext) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImmersiveMode(
    enabled: Boolean,
    orientationMode: FullscreenOrientationMode = FullscreenOrientationMode.Auto
) {
    val activity = LocalContext.current as? Activity

    SideEffect {
        val desiredOrientation =
            if (enabled) {
                when (orientationMode) {
                    FullscreenOrientationMode.Auto -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    FullscreenOrientationMode.Horizontal -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    FullscreenOrientationMode.Vertical -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

        if (activity?.requestedOrientation != desiredOrientation) {
            activity?.requestedOrientation = desiredOrientation
        }
    }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}

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
            val disposeWindow = activity?.window
            if (disposeWindow != null) {
                val controller = WindowCompat.getInsetsController(disposeWindow, disposeWindow.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
                disposeWindow.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                @Suppress("DEPRECATION")
                disposeWindow.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
