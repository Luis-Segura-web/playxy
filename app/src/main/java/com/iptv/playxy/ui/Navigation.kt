package com.iptv.playxy.ui

/**
 * Navigation routes for the application
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val LOADING = "loading"
    const val MAIN = "main"
    const val MOVIE_DETAIL = "movie_detail/{streamId}/{categoryId}"
    const val SERIES_DETAIL = "series_detail/{seriesId}/{categoryId}"

    fun movieDetail(streamId: String, categoryId: String) = "movie_detail/$streamId/$categoryId"
    fun seriesDetail(seriesId: String, categoryId: String) = "series_detail/$seriesId/$categoryId"
}

/**
 * Main app destinations for bottom navigation
 */
enum class MainDestination(val title: String) {
    HOME("Inicio"),
    TV("TV"),
    MOVIES("Pelculas"),
    SERIES("Series"),
    SETTINGS("Ajustes")
}

