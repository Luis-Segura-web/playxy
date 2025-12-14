package com.iptv.playxy.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import android.view.View
import com.iptv.playxy.R
import org.videolan.libvlc.util.VLCVideoLayout

@Composable
fun PlayerSurface(
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    resizeMode: Int = ResizeMode.FIT,
    keepScreenOn: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.player_compose_view, null, false) as VLCVideoLayout).apply {
                this.keepScreenOn = keepScreenOn
                addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            playerManager.attachVideoLayout(this@apply)
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            playerManager.detachVideoLayout(this@apply)
                        }
                    }
                )
            }
        },
        update = { layout ->
            ResizeMode.apply(playerManager, resizeMode)
            layout.keepScreenOn = keepScreenOn
        },
        onRelease = { layout -> playerManager.detachVideoLayout(layout) }
    )
}

object ResizeMode {
    const val FIT = 0
    const val FILL = 1

    fun apply(playerManager: PlayerManager, resizeMode: Int) {
        // LibVLC no expone un equivalente directo al resizeMode de Media3 en este wrapper.
        // Dejamos la opción para compatibilidad con llamadas existentes.
        @Suppress("UNUSED_PARAMETER")
        val ignored = playerManager to resizeMode
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
