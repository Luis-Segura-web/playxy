package com.iptv.playxy.ui.player

import android.app.Activity
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectionDialog(
    tracks: PlaybackTracks,
    onDismiss: () -> Unit,
    onAudioSelected: (TrackOption) -> Unit,
    onSubtitleSelected: (TrackOption?) -> Unit,
    initialTab: TrackSelectionTab? = null,
    immersive: Boolean = false
) {
    KeepSystemBarsHidden(enabled = immersive)
    val showAudioTab = tracks.audio.size > 1
    val showSubtitleTab = tracks.text.size > 1
    if (!showAudioTab && !showSubtitleTab) return

    val tabs = buildList {
        if (showAudioTab) add(TrackTab.Audio)
        if (showSubtitleTab) add(TrackTab.Subtitles)
    }
    val initialSelectedTabIndex = when (initialTab) {
        TrackSelectionTab.Audio -> tabs.indexOf(TrackTab.Audio)
        TrackSelectionTab.Subtitles -> tabs.indexOf(TrackTab.Subtitles)
        null -> 0
    }.takeIf { it >= 0 } ?: 0
    var selectedTab by remember(initialSelectedTabIndex) { mutableIntStateOf(initialSelectedTabIndex) }

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
                Text(text = "Audio y subtítulos", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            if (tabs.size > 1) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedTab
                        val title = when (tab) {
                            TrackTab.Audio -> "Audio"
                            TrackTab.Subtitles -> "Subtítulos"
                        }
                        Text(
                            text = title,
                            style = if (selected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }

            when (tabs.getOrNull(selectedTab)) {
                TrackTab.Audio -> TrackList(
                    options = tracks.audio,
                    onClick = { option ->
                        onAudioSelected(option)
                        onDismiss()
                    }
                )

                TrackTab.Subtitles -> TrackList(
                    options = tracks.text,
                    onClick = { option ->
                        if (option.isDisableOption) {
                            onSubtitleSelected(null)
                        } else {
                            onSubtitleSelected(option)
                        }
                        onDismiss()
                    }
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun TrackList(
    options: List<TrackOption>,
    onClick: (TrackOption) -> Unit
) {
    LazyColumn {
        items(options) { option ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(option) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                        option.language?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (option.selected) {
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

private enum class TrackTab { Audio, Subtitles }

enum class TrackSelectionTab { Audio, Subtitles }

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
