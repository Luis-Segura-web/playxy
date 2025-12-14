package com.iptv.playxy.ui.player

import android.app.Activity
import android.view.View
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.videolan.libvlc.MediaPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoFitDialog(
    selectedScale: MediaPlayer.ScaleType,
    onDismiss: () -> Unit,
    onScaleSelected: (MediaPlayer.ScaleType) -> Unit,
    immersive: Boolean = false
) {
    KeepSystemBarsHidden(enabled = immersive)
    val options = rememberVideoFitOptions()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Ajuste de pantalla", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            LazyColumn {
                items(options) { option ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onScaleSelected(option.scaleType)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = option.title, style = MaterialTheme.typography.bodyLarge)
                                option.subtitle?.let { subtitle ->
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (option.scaleType == selectedScale) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private data class VideoFitOption(
    val scaleType: MediaPlayer.ScaleType,
    val title: String,
    val subtitle: String? = null
)

@Composable
private fun rememberVideoFitOptions(): List<VideoFitOption> =
    listOf(
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_BEST_FIT,
            title = "Mejor ajuste",
            subtitle = "Recomendado"
        ),
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN,
            title = "Ajustar a pantalla",
            subtitle = "Muestra todo el video"
        ),
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_FILL,
            title = "Llenar",
            subtitle = "Recorta para llenar la pantalla"
        ),
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_16_9,
            title = "16:9",
            subtitle = "Formato panorámico"
        ),
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_4_3,
            title = "4:3",
            subtitle = "Formato clásico"
        ),
        VideoFitOption(
            scaleType = MediaPlayer.ScaleType.SURFACE_ORIGINAL,
            title = "Original",
            subtitle = "Tamaño original del video"
        )
    )

@Composable
private fun KeepSystemBarsHidden(enabled: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose { }

        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        onDispose {
            val disposeWindow = activity?.window
            if (disposeWindow != null) {
                val controller = WindowCompat.getInsetsController(disposeWindow, disposeWindow.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                @Suppress("DEPRECATION")
                disposeWindow.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
        }
    }
}
