package com.iptv.playxy.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player

/**
 * Estado observable simplificado del reproductor para UI.
 */
class PlayerUiState {
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var hasError by mutableStateOf(false)
    var firstFrameRendered by mutableStateOf(false)
}

@Composable
fun rememberPlayerUiState(playerManager: PlayerManager): PlayerUiState {
    val uiState = remember { PlayerUiState() }

    LaunchedEffect(playerManager) {
        uiState.firstFrameRendered = playerManager.hasRenderedFirstFrame()
        uiState.isBuffering = !uiState.firstFrameRendered
    }

    DisposableEffect(playerManager) {
        val frameListener: (Boolean) -> Unit = { rendered ->
            uiState.firstFrameRendered = rendered
            uiState.isBuffering = !rendered
        }
        playerManager.addFrameListener(frameListener)
        onDispose { playerManager.removeFrameListener(frameListener) }
    }

    val player = playerManager.getPlayer()
    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_BUFFERING && !uiState.firstFrameRendered) {
                    uiState.isBuffering = true
                }
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    uiState.isBuffering = false
                    uiState.hasError = false
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                uiState.isPlaying = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                uiState.hasError = true
                uiState.isPlaying = false
                uiState.isBuffering = false
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    return uiState
}
