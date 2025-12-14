package com.iptv.playxy.ui.player

import android.content.pm.ActivityInfo

enum class FullscreenOrientationMode {
    Auto,
    Horizontal,
    Vertical;

    fun toRequestedOrientation(): Int =
        when (this) {
            Auto -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            Horizontal -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            Vertical -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }
}

