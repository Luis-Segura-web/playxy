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
    @field:Json(name = "tmdb_id") val tmdbId: Any?,  // Can be Int, String, or null
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "o_name") val originalName: String?,
    @field:Json(name = "cover_big") val coverBig: String?,
    @field:Json(name = "movie_image") val movieImage: String?,
    @field:Json(name = "backdrop") val backdrop: String?,
    @field:Json(name = "releasedate") val releaseDate: String?,
    @field:Json(name = "episode_run_time") val episodeRunTime: Any?,  // Can be String or Int
    @field:Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @field:Json(name = "director") val director: String?,
    @field:Json(name = "actors") val actors: String?,
    @field:Json(name = "cast") val cast: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "age") val age: String?,
    @field:Json(name = "mpaa_rating") val mpaaRating: String?,
    @field:Json(name = "rating_count_kinopoisk") val ratingCountKinopoisk: Any?,  // Can be String or Int
    @field:Json(name = "rating") val rating: Any?,  // Can be String or number
    @field:Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @field:Json(name = "country") val country: String?,
    @field:Json(name = "genre") val genre: String?,
    @field:Json(name = "backdrop_path") val backdropPath: Any?,  // Can be List<String> or empty array
    @field:Json(name = "duration_secs") val durationSecs: Any?,  // Can be Int or String
    @field:Json(name = "duration") val duration: String?,
    @field:Json(name = "video") val video: Any?,  // Can be object, array, or string
    @field:Json(name = "audio") val audio: Any?,  // Can be object, array, or string
    @field:Json(name = "bitrate") val bitrate: Any?  // Can be Int or String
)

@JsonClass(generateAdapter = true)
data class MovieData(
    @field:Json(name = "stream_id") val streamId: Any?,  // Can be Int or String
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "added") val added: Any?,  // Can be String or Long
    @field:Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "direct_source") val directSource: String?
)
