package com.iptv.playxy.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import com.iptv.playxy.R
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun PlayerSurface(
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    keepScreenOn: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.player_compose_view, null, false) as PlayerView).apply {
                this.player = playerManager.getPlayer()
                this.resizeMode = resizeMode
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setKeepContentOnPlayerReset(true)
                this.keepScreenOn = keepScreenOn
            }.also { view ->
                playerManager.attachPlayerView(view)
            }
        },
        update = { view ->
            playerManager.attachPlayerView(view)
            view.resizeMode = resizeMode
            view.keepScreenOn = keepScreenOn
        }
    )
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
