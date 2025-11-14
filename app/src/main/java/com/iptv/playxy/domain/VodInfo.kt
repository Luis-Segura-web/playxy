package com.iptv.playxy.domain

data class VodInfo(
    val tmdbId: String?,
    val name: String,
    val originalName: String?,
    val coverBig: String?,
    val movieImage: String?,
    val releaseDate: String?,
    val duration: String?,
    val youtubeTrailer: String?,
    val director: String?,
    val actors: String?,
    val cast: String?,
    val description: String?,
    val plot: String?,
    val age: String?,
    val mpaaRating: String?,
    val rating: String?,
    val rating5Based: Double?,
    val country: String?,
    val genre: String?,
    val backdropPath: List<String>?,
    val durationSecs: Int?,
    val video: String?,
    val audio: String?,
    val bitrate: Int?
)

