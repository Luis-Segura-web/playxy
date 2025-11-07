package com.iptv.playxy.ui

/**
 * Navigation routes for the application
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val LOADING = "loading"
    const val MAIN = "main"
}

/**
 * Main app destinations for bottom navigation
 */
enum class MainDestination(val title: String) {
    HOME("Inicio"),
    TV("TV"),
    MOVIES("Películas"),
    SERIES("Series"),
    SETTINGS("Ajustes")
}
