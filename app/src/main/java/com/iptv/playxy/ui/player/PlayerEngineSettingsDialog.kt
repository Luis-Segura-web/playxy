package com.iptv.playxy.ui.player

import android.app.Activity
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iptv.playxy.domain.player.AndroidDisplayChroma
import com.iptv.playxy.domain.player.AudioOutput
import com.iptv.playxy.domain.player.DecoderMode
import com.iptv.playxy.domain.player.PlayerEngineConfig
import com.iptv.playxy.domain.player.VideoOutput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerEngineSettingsDialog(
    initialConfig: PlayerEngineConfig,
    onDismiss: () -> Unit,
    onApply: (PlayerEngineConfig) -> Unit,
    immersive: Boolean = false
) {
    KeepSystemBarsHidden(enabled = immersive)
    var config by remember(initialConfig) { mutableStateOf(initialConfig) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Motor de reproducci\u00f3n", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            SettingSection(title = "Decodificador") {
                SettingOption(
                    title = "Software",
                    subtitle = "Forzar decodificaci\u00f3n por CPU",
                    selected = config.decoderMode == DecoderMode.SOFTWARE,
                    onClick = { config = config.copy(decoderMode = DecoderMode.SOFTWARE) }
                )
                SettingOption(
                    title = "Hardware",
                    subtitle = "MediaCodec/Codec2 (puede fallar en algunos dispositivos)",
                    selected = config.decoderMode == DecoderMode.HARDWARE,
                    onClick = { config = config.copy(decoderMode = DecoderMode.HARDWARE) }
                )
            }

            SettingSection(title = "Audio") {
                SettingOption(
                    title = "Predeterminado",
                    selected = config.audioOutput == AudioOutput.DEFAULT,
                    onClick = { config = config.copy(audioOutput = AudioOutput.DEFAULT) }
                )
                SettingOption(
                    title = "OpenSL ES",
                    subtitle = "Salida de audio acelerada (si est\u00e1 disponible)",
                    selected = config.audioOutput == AudioOutput.OPENSL_ES,
                    onClick = { config = config.copy(audioOutput = AudioOutput.OPENSL_ES) }
                )
            }

            SettingSection(title = "Video") {
                SettingOption(
                    title = "Predeterminado",
                    selected = config.videoOutput == VideoOutput.DEFAULT,
                    onClick = { config = config.copy(videoOutput = VideoOutput.DEFAULT) }
                )
                SettingOption(
                    title = "OpenGL (GLES2)",
                    subtitle = "Salida de video por OpenGL",
                    selected = config.videoOutput == VideoOutput.OPENGL_GLES2,
                    onClick = { config = config.copy(videoOutput = VideoOutput.OPENGL_GLES2) }
                )
            }

            SettingSection(title = "Formato de p\u00edxel") {
                SettingOption(
                    title = "Predeterminado",
                    selected = config.androidDisplayChroma == AndroidDisplayChroma.DEFAULT,
                    onClick = { config = config.copy(androidDisplayChroma = AndroidDisplayChroma.DEFAULT) }
                )
                SettingOption(
                    title = "RV32",
                    subtitle = "32-bit (mejor calidad, m\u00e1s consumo)",
                    selected = config.androidDisplayChroma == AndroidDisplayChroma.RV32,
                    onClick = { config = config.copy(androidDisplayChroma = AndroidDisplayChroma.RV32) }
                )
                SettingOption(
                    title = "RGB16",
                    subtitle = "16-bit (menos consumo, puede degradar)",
                    selected = config.androidDisplayChroma == AndroidDisplayChroma.RGB16,
                    onClick = { config = config.copy(androidDisplayChroma = AndroidDisplayChroma.RGB16) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        onApply(config)
                        onDismiss()
                    }
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
    content()
    HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun SettingOption(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun KeepSystemBarsHidden(enabled: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val window = activity?.window ?: return@DisposableEffect onDispose {}
        val decorView = window.decorView
        
        fun applyImmersiveMode() {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        
        @Suppress("DEPRECATION")
        val listener = View.OnSystemUiVisibilityChangeListener { visibility ->
            if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                decorView.postDelayed({ applyImmersiveMode() }, 100)
            }
        }
        
        applyImmersiveMode()
        @Suppress("DEPRECATION")
        decorView.setOnSystemUiVisibilityChangeListener(listener)
        
        onDispose {
            @Suppress("DEPRECATION")
            decorView.setOnSystemUiVisibilityChangeListener(null)
        }
    }
}
