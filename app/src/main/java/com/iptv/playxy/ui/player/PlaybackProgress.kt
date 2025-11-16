package com.iptv.playxy.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackProgress(
    state: PlaybackUiState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.durationMs <= 0L) return
    var sliderPosition by remember { mutableFloatStateOf(state.positionMs.toFloat()) }
    val userScrubbing = remember { mutableStateOf(false) }

    LaunchedEffect(state.positionMs, state.durationMs) {
        if (!userScrubbing.value) {
            sliderPosition = state.positionMs.coerceAtMost(state.durationMs).toFloat()
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatPlaybackTime(state.positionMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = formatPlaybackTime(state.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    userScrubbing.value = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    userScrubbing.value = false
                    onSeek(sliderPosition.toLong())
                },
                valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
