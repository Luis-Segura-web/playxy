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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
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
fun MovieMiniPlayer(
    streamUrl: String,
    movieTitle: String,
    playerManager: PlayerManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onFullscreen: () -> Unit = {}
) {
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    var showTrackDialog by remember { mutableStateOf(false) }

    LaunchedEffect(streamUrl) {
        playerManager.playMedia(streamUrl, PlayerType.MOVIE)
    }

    MiniPlayerContainer(
        uiState = playbackState,
        playerManager = playerManager,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        controlsLocked = showTrackDialog
    ) { state, _, setControlsVisible ->
        MovieMiniPlayerOverlay(
            title = movieTitle,
            state = state,
            onClose = {
                onClose()
                setControlsVisible(true)
            },
            onReplay = { playerManager.playMedia(streamUrl, PlayerType.MOVIE, forcePrepare = true) },
            onSeekBack = { playerManager.seekBackward() },
            onSeekForward = { playerManager.seekForward() },
            onTogglePlay = {
                if (state.isPlaying) playerManager.pause() else playerManager.play()
            },
            onFullscreen = onFullscreen,
            onShowTracks = {
                setControlsVisible(true)
                showTrackDialog = true
            },
            onSeek = { position -> playerManager.seekTo(position) },
            hasTrackOptions = state.tracks.hasDialogOptions
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
private fun MovieMiniPlayerOverlay(
    title: String,
    state: PlaybackUiState,
    onClose: () -> Unit,
    onReplay: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlay: () -> Unit,
    onFullscreen: () -> Unit,
    onShowTracks: () -> Unit,
    onSeek: (Long) -> Unit,
    hasTrackOptions: Boolean
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
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
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
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekBack) {
                    Icon(imageVector = Icons.Default.Replay10, contentDescription = "Retroceder", tint = Color.White)
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onSeekForward) {
                    Icon(imageVector = Icons.Default.Forward10, contentDescription = "Avanzar", tint = Color.White)
                }
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
                .padding(horizontal = 12.dp)
                .padding(top = 1.dp, bottom = 0.dp)
        ) {
            PlaybackProgress(
                state = state,
                onSeek = onSeek,
                modifier = Modifier.fillMaxWidth(),
                bottomSpacing = 0.dp,
                trailingContent = {
                    if (hasTrackOptions) {
                        IconButton(onClick = onShowTracks) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Pistas", tint = Color.White)
                        }
                    }
                    IconButton(onClick = onFullscreen) {
                        Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Pantalla completa", tint = Color.White)
                    }
                }
            )
        }
    }
}
