package com.iptv.playxy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@Composable
fun PlayerVideoSurface(
    streamKey: String,
    modifier: Modifier = Modifier,
    playerManager: PlayerManager,
    onPlayerReady: () -> Unit
) {
    // No usamos key(streamKey) para no recrear el PlayerView en cada cambio de URL,
    // el ExoPlayer ya cambia de media internamente.
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setKeepContentOnPlayerReset(true)
                keepScreenOn = true
                // Asociar el player existente si ya está inicializado.
                playerManager.getPlayer()?.let { player = it }
            }
        },
        update = { view ->
            val currentPlayer = playerManager.getPlayer()
            if (currentPlayer != null && view.player != currentPlayer) {
                view.player = currentPlayer
            }
            view.keepScreenOn = true

            // Notificar que la surface está lista cuando tenemos un player asociado.
            if (view.player != null && view.videoSurfaceView != null) {
                onPlayerReady()
            }
        },
        modifier = modifier
    )
}

@Composable
fun OverlayGradient(top: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gradient = if (top) Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
    ) else Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
    )
    Box(
        modifier = modifier.background(gradient)
    ) { content() }
}

@Composable
fun PlayerIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    size: Dp = 40.dp,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.size(size)
        )
    }
}
