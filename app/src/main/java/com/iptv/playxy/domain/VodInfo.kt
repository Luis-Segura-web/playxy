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
    val bitrate: Int?,
    val tmdbCast: List<TmdbCast> = emptyList(),
    val tmdbCollection: List<TmdbMovieLink> = emptyList(),
    val tmdbSimilar: List<TmdbMovieLink> = emptyList()
)

data class TmdbCast(
    val id: Int?,
    val name: String,
    val character: String?,
    val profile: String?
)

data class TmdbMovieLink(
    val tmdbId: Int,
    val title: String,
    val poster: String?,
    val availableStreamId: String? = null,
    val availableCategoryId: String? = null,
    val tmdbTitle: String? = null,
    val character: String? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val backdrop: String? = null,
    val rating: Double? = null
)

data class ActorDetails(
    val name: String,
    val profile: String?,
    val biography: String?,
    val birthday: String?,
    val placeOfBirth: String?,
    val availableMovies: List<TmdbMovieLink>,
    val unavailableMovies: List<TmdbMovieLink>
)
