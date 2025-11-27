package com.iptv.playxy.ui

/**
 * Navigation routes for the application
 */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val LOADING = "loading"
    const val MAIN = "main"
    const val MOVIE_DETAIL = "movie_detail/{streamId}/{categoryId}?fromLink={fromLink}"
    const val SERIES_DETAIL = "series_detail/{seriesId}/{categoryId}"
    const val ACTOR_DETAIL = "actor_detail/{actorId}?actorName={actorName}&actorProfile={actorProfile}"

    fun movieDetail(streamId: String, categoryId: String, fromLink: Boolean = false) =
        "movie_detail/$streamId/$categoryId?fromLink=$fromLink"
    fun seriesDetail(seriesId: String, categoryId: String) = "series_detail/$seriesId/$categoryId"
    fun actorDetail(cast: com.iptv.playxy.domain.TmdbCast): String {
        val encodedName = android.net.Uri.encode(cast.name)
        val encodedProfile = android.net.Uri.encode(cast.profile ?: "")
        val actorId = cast.id ?: -1
        return "actor_detail/$actorId?actorName=$encodedName&actorProfile=$encodedProfile"
    }
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
