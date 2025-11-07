package com.iptv.playxy.data.api

import com.google.gson.annotations.SerializedName

/**
 * API response model for series
 * Fields match the JSON structure from the provider
 */
data class SeriesResponse(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("series_id") val seriesId: String?,
    @SerializedName("tmdb_id") val tmdbId: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("last_modified") val lastModified: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("rating_5based") val rating5Based: String?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") val categoryId: String?
)
