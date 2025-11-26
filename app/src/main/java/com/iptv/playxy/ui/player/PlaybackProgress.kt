package com.iptv.playxy.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProgress(
    state: PlaybackUiState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = 4.dp,
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

    val durationFloat = duration.toFloat()
    Column(modifier = modifier) {
        val bufferFraction = (state.bufferedPositionMs.toFloat() / durationFloat).coerceIn(0f, 1f)
        val progressFraction = (sliderPosition / durationFloat).coerceIn(0f, 1f)
        val baseTrackColor = Color.White.copy(alpha = 0.2f)
        val bufferTrackColor = Color.White.copy(alpha = 0.45f)
        val playedColor = MaterialTheme.colorScheme.primary
        val trackHeight = 4.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(trackHeight)
            ) {
                val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                drawRoundRect(
                    color = baseTrackColor,
                    size = size,
                    cornerRadius = cornerRadius
                )

                val playedWidth = size.width * progressFraction
                if (playedWidth > 0f) {
                    drawRoundRect(
                        color = playedColor,
                        size = Size(width = playedWidth, height = size.height),
                        cornerRadius = cornerRadius
                    )
                }

                val bufferedWidth = (bufferFraction - progressFraction).coerceAtLeast(0f) * size.width
                if (bufferedWidth > 0f) {
                    drawRect(
                        color = bufferTrackColor,
                        topLeft = Offset(x = playedWidth, y = 0f),
                        size = Size(width = bufferedWidth, height = size.height)
                    )
                }
            }

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
                    .height(40.dp),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .offset(y = 0.dp)
                            .clip(CircleShape)
                            .background(playedColor)
                    )
                }
            )
        }

        val padTop = if (bottomSpacing < 0.dp) 0.dp else bottomSpacing
        // Mantener los controles pegados al slider (sin offset extra cuando es positivo).
        val offsetY = if (bottomSpacing < 0.dp) bottomSpacing else 0.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = padTop)
                .offset(y = offsetY),
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
