package com.iptv.playxy.ui.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Estado observable simplificado del reproductor para UI.
 */
class PlayerUiState {
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var hasError by mutableStateOf(false)
    var firstFrameRendered by mutableStateOf(false)
}

