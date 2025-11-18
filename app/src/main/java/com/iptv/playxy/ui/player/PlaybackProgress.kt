package com.iptv.playxy.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgress(
    state: PlaybackUiState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    if (state.durationMs <= 0L) return
    var sliderPosition by remember { mutableFloatStateOf(state.positionMs.toFloat()) }
    val userScrubbing = remember { mutableStateOf(false) }
    val duration = state.durationMs.coerceAtLeast(1L)

    LaunchedEffect(state.positionMs, state.durationMs) {
        if (!userScrubbing.value) {
            sliderPosition = state.positionMs.coerceAtMost(state.durationMs).toFloat()
        }
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackShape = RoundedCornerShape(999.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(trackShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            val bufferFraction = (state.bufferedPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(bufferFraction)
                    .height(4.dp)
                    .clip(trackShape)
                    .background(Color.White.copy(alpha = 0.45f))
            )
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
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${formatPlaybackTime(state.positionMs)} / ${formatPlaybackTime(state.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            if (trailingContent != null) {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailingContent
                )
            }
        }
    }
}
