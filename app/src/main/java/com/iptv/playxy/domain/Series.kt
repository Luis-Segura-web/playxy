package com.iptv.playxy.domain

data class Series(
    val seriesId: String,
    val name: String,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: Float = 0f,
    val rating5Based: Float = 0f,
    val backdropPath: List<String> = emptyList(),
    val youtubeTrailer: String? = null,
    val episodeRunTime: String? = null,
    val categoryId: String,
    val tmdbId: String? = null,
    val lastModified: String? = null
)
