package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for detailed VOD information
 */
@JsonClass(generateAdapter = true)
data class VodInfoResponse(
    @field:Json(name = "info") val info: VodInfo?,
    @field:Json(name = "movie_data") val movieData: MovieData?
)

@JsonClass(generateAdapter = true)
data class VodInfo(
    @field:Json(name = "tmdb_id") val tmdbId: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "o_name") val originalName: String?,
    @field:Json(name = "cover_big") val coverBig: String?,
    @field:Json(name = "movie_image") val movieImage: String?,
    @field:Json(name = "releasedate") val releaseDate: String?,
    @field:Json(name = "episode_run_time") val episodeRunTime: String?,
    @field:Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @field:Json(name = "director") val director: String?,
    @field:Json(name = "actors") val actors: String?,
    @field:Json(name = "cast") val cast: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "age") val age: String?,
    @field:Json(name = "mpaa_rating") val mpaaRating: String?,
    @field:Json(name = "rating_count_kinopoisk") val ratingCountKinopoisk: String?,
    @field:Json(name = "rating") val rating: String?,
    @field:Json(name = "rating_5based") val rating5Based: Double?,
    @field:Json(name = "country") val country: String?,
    @field:Json(name = "genre") val genre: String?,
    @field:Json(name = "backdrop_path") val backdropPath: List<String>?,
    @field:Json(name = "duration_secs") val durationSecs: Int?,
    @field:Json(name = "duration") val duration: String?,
    @field:Json(name = "video") val video: String?,
    @field:Json(name = "audio") val audio: String?,
    @field:Json(name = "bitrate") val bitrate: Int?
)

@JsonClass(generateAdapter = true)
data class MovieData(
    @field:Json(name = "stream_id") val streamId: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "added") val added: String?,
    @field:Json(name = "category_id") val categoryId: String?,
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "direct_source") val directSource: String?
)

