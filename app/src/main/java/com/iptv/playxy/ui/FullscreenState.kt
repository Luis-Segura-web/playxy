package com.iptv.playxy.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * CompositionLocal to share fullscreen state across the app
 */
val LocalFullscreenState = compositionLocalOf { mutableStateOf(false) }

