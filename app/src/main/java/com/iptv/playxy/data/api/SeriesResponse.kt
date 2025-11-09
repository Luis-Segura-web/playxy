package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for series
 * Fields match the JSON structure from the provider
 */
data class SeriesResponse(
    @field:Json(name = "num") val num: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "series_id") val seriesId: String?,
    @field:Json(name = "tmdb_id") val tmdbId: String?,
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "cast") val cast: String?,
    @field:Json(name = "director") val director: String?,
    @field:Json(name = "genre") val genre: String?,
    @field:Json(name = "releaseDate") val releaseDate: String?,
    @field:Json(name = "last_modified") val lastModified: String?,
    @field:Json(name = "rating") val rating: String?,
    @field:Json(name = "rating_5based") val rating5Based: String?,
    @field:Json(name = "backdrop_path") val backdropPath: List<String>?,
    @field:Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @field:Json(name = "episode_run_time") val episodeRunTime: String?,
    @field:Json(name = "category_id") val categoryId: String?
)
