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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.playxy.ui.LocalPipController
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
    val pipController = LocalPipController.current
    val hideUiForPip by pipController.hidePlayerUi.collectAsStateWithLifecycle()

    LaunchedEffect(hideUiForPip) {
        if (hideUiForPip) {
            showControls.value = false
        }
    }

    LaunchedEffect(showControls.value, uiState.isPlaying, controlsLocked) {
        if (!controlsLocked && showControls.value && uiState.isPlaying && !uiState.hasError) {
            delay(4500)
            showControls.value = false
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .noRippleClickable(enabled = !controlsLocked && !hideUiForPip) {
                showControls.value = !showControls.value
            }
    ) {
        if (!uiState.hasError) {
            val keepAwake = uiState.isPlaying
            PlayerSurface(
                playerManager = playerManager,
                modifier = Modifier.fillMaxSize(),
                keepScreenOn = keepAwake
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }

        AnimatedVisibility(
            visible = (!hideUiForPip) && (showControls.value || !uiState.isPlaying || uiState.hasError),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            overlay(uiState, showControls.value) { visible ->
                showControls.value = visible
            }
        }

        if (!hideUiForPip && uiState.isBuffering && !uiState.firstFrameRendered) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}
