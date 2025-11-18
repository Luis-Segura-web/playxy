package com.iptv.playxy.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun PlayerSurface(
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    keepScreenOn: Boolean = true
) {
    val playerViewState = remember { mutableStateOf<PlayerView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                this.player = playerManager.getPlayer()
                this.resizeMode = resizeMode
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setKeepContentOnPlayerReset(true)
                this.keepScreenOn = keepScreenOn
            }.also { view ->
                playerViewState.value = view
                playerManager.attachPlayerView(view)
            }
        },
        update = { view ->
            playerViewState.value = view
            playerManager.attachPlayerView(view)
            view.resizeMode = resizeMode
            view.keepScreenOn = keepScreenOn
        }
    )

    DisposableEffect(playerManager, playerViewState.value) {
        val currentView = playerViewState.value
        onDispose {
            currentView?.let { playerManager.detachPlayerView(it) }
        }
    }
}

fun Modifier.noRippleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    clickable(
        enabled = enabled,
        indication = null,
        interactionSource = interactionSource,
        onClick = onClick
    )
}
