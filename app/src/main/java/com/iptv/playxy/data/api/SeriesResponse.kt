package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for series
 * Fields match the JSON structure from the provider
 */
data class SeriesResponse(
    @Json(name = "num") val num: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "series_id") val seriesId: String?,
    @Json(name = "tmdb_id") val tmdbId: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "plot") val plot: String?,
    @Json(name = "cast") val cast: String?,
    @Json(name = "director") val director: String?,
    @Json(name = "genre") val genre: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "last_modified") val lastModified: String?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "rating_5based") val rating5Based: String?,
    @Json(name = "backdrop_path") val backdropPath: List<String>?,
    @Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @Json(name = "episode_run_time") val episodeRunTime: String?,
    @Json(name = "category_id") val categoryId: String?
)
