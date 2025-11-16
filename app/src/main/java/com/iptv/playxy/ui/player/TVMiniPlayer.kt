package com.iptv.playxy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    var showTrackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(streamUrl) {
        playerManager.playMedia(streamUrl, PlayerType.TV)
    }

    MiniPlayerContainer(
        uiState = playbackState,
        playerManager = playerManager,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        controlsLocked = showTrackDialog
    ) { state, _, setControlsVisible ->
        TVMiniOverlay(
            state = state,
            channelName = channelName,
            hasTrackOptions = state.tracks.hasDialogOptions,
            onClose = {
                onClose()
                setControlsVisible(true)
            },
            onReplay = { playerManager.playMedia(streamUrl, PlayerType.TV, forcePrepare = true) },
            onTogglePlay = {
                if (state.isPlaying) playerManager.pause() else playerManager.play()
            },
            onPrevious = onPreviousChannel,
            onNext = onNextChannel,
            onShowTracks = {
                setControlsVisible(true)
                showTrackDialog = true
            },
            onFullscreen = onFullscreen
        )
    }

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
private fun TVMiniOverlay(
    state: PlaybackUiState,
    channelName: String,
    hasTrackOptions: Boolean,
    onClose: () -> Unit,
    onReplay: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShowTracks: () -> Unit,
    onFullscreen: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.hasError) {
                Text(
                    text = state.errorMessage ?: "Contenido no disponible",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = onReplay) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Text(text = "Reintentar", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasTrackOptions) {
                IconButton(onClick = onShowTracks) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Pistas", tint = Color.White)
                }
            }
            IconButton(onClick = onFullscreen) {
                Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Pantalla completa", tint = Color.White)
            }
        }
    }
}
