package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for series
 * Fields match the JSON structure from the provider
 */
@JsonClass(generateAdapter = true)
data class SeriesResponse(
    @field:Json(name = "num") val num: Any?,  // Can be Int or String
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "series_id") val seriesId: Any?,  // Can be Int or String
    @field:Json(name = "tmdb_id") val tmdbId: Any?,  // Can be Int, String, or null
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "cast") val cast: String?,
    @field:Json(name = "director") val director: String?,
    @field:Json(name = "genre") val genre: String?,
    @field:Json(name = "releaseDate") val releaseDate: String?,
    @field:Json(name = "last_modified") val lastModified: Any?,  // Can be String or Long
    @field:Json(name = "rating") val rating: Any?,  // Can be String or number
    @field:Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @field:Json(name = "backdrop_path") val backdropPath: Any?,  // Can be List<String> or empty array
    @field:Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @field:Json(name = "episode_run_time") val episodeRunTime: Any?,  // Can be String or Int
    @field:Json(name = "category_id") val categoryId: Any?  // Can be String or Int
)
