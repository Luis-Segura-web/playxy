package com.iptv.playxy.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

@Stable
data class PlayerContainerConfig(
    val state: PlaybackUiState,
    val modifier: Modifier = Modifier,
    val controlsLocked: Boolean = false,
    val overlay: MiniPlayerOverlay
)

typealias PlayerContainerHost = @Composable (PlayerContainerConfig) -> Unit

val LocalPlayerContainerHost = staticCompositionLocalOf<PlayerContainerHost> {
    error("PlayerContainerHost not provided")
}

@Composable
fun rememberPlayerContainerHost(playerManager: PlayerManager): PlayerContainerHost {
    val movablePlayer = remember(playerManager) {
        movableContentOf<PlayerContainerConfig> { config ->
            MiniPlayerContainer(
                uiState = config.state,
                playerManager = playerManager,
                modifier = config.modifier,
                controlsLocked = config.controlsLocked,
                overlay = config.overlay
            )
        }
    }
    return { config -> movablePlayer(config) }
}
