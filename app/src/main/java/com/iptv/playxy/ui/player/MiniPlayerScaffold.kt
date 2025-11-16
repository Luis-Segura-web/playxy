package com.iptv.playxy.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

internal typealias MiniPlayerOverlay = @Composable BoxScope.(PlaybackUiState, Boolean, (Boolean) -> Unit) -> Unit

@Composable
internal fun MiniPlayerContainer(
    uiState: PlaybackUiState,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    controlsLocked: Boolean = false,
    overlay: MiniPlayerOverlay
) {
    val showControls = remember { mutableStateOf(true) }

    LaunchedEffect(showControls.value, uiState.isPlaying, controlsLocked) {
        if (!controlsLocked && showControls.value && uiState.isPlaying && !uiState.hasError) {
            delay(4500)
            showControls.value = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .noRippleClickable(enabled = !controlsLocked) {
                showControls.value = !showControls.value
            }
    ) {
        PlayerSurface(playerManager = playerManager, modifier = Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = showControls.value || !uiState.isPlaying || uiState.hasError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            overlay(uiState, showControls.value) { visible ->
                showControls.value = visible
            }
        }

        if (uiState.isBuffering && !uiState.firstFrameRendered) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}
