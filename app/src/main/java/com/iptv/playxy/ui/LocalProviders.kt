package com.iptv.playxy.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.iptv.playxy.ui.pip.PipController
import com.iptv.playxy.ui.player.PlayerManager

val LocalPlayerManager = staticCompositionLocalOf<PlayerManager> {
    error("PlayerManager no está disponible. Asegúrate de proveerlo desde la Activity.")
}

val LocalPipController = staticCompositionLocalOf<PipController> {
    error("PipController no está disponible. Asegúrate de proveerlo desde la Activity.")
}
