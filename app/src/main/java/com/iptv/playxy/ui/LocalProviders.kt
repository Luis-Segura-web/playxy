package com.iptv.playxy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.iptv.playxy.ui.player.PlayerManager

val LocalPlayerManager = staticCompositionLocalOf<PlayerManager> {
    error("PlayerManager no está disponible. Asegúrate de proveerlo desde la Activity.")
}

