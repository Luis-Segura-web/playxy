package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for series info endpoint (get_series_info)
 */
@JsonClass(generateAdapter = true)
data class SeriesInfoResponse(
    @field:Json(name = "seasons") val seasons: List<SeasonResponse>?,
    @field:Json(name = "episodes") val episodes: Map<String, List<EpisodeResponse>>?,
    @field:Json(name = "info") val info: SeriesInfoDetailsResponse?
)

@JsonClass(generateAdapter = true)
data class SeasonResponse(
    @field:Json(name = "id") val id: Any?,  // Season TMDB ID - Can be Int or String
    @field:Json(name = "season_number") val seasonNumber: Any?,  // Can be Int or String
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "episode_count") val episodeCount: Any?,  // Can be Int or String
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "air_date") val airDate: String?,
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "cover_big") val coverBig: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeResponse(
    @field:Json(name = "id") val id: Any?,  // Can be Int or String
    @field:Json(name = "episode_num") val episodeNum: Any?,  // Can be Int or String
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "info") val info: EpisodeInfoResponse?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "added") val added: Any?,  // Can be String or Long
    @field:Json(name = "season") val season: Any?,  // Can be Int or String
    @field:Json(name = "direct_source") val directSource: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeInfoResponse(
    @field:Json(name = "tmdb_id") val tmdbId: Any?,  // Can be Int, String, or null
    @field:Json(name = "releasedate") val releaseDate: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "duration_secs") val durationSecs: Any?,  // Can be Int or String
    @field:Json(name = "duration") val duration: String?,
    // video/audio can be objects, empty arrays, or strings depending on the service
    @field:Json(name = "video") val video: Any?,
    @field:Json(name = "audio") val audio: Any?,
    @field:Json(name = "bitrate") val bitrate: Any?,  // Can be Int or String
    @field:Json(name = "rating") val rating: Any?,  // Can be String or number
    @field:Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @field:Json(name = "season") val season: Any?,  // Can be Int or String
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "cover_big") val coverBig: String?,
    @field:Json(name = "movie_image") val movieImage: String?,
    @field:Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @field:Json(name = "cast") val cast: String?,
    @field:Json(name = "director") val director: String?
)

@JsonClass(generateAdapter = true)
data class SeriesInfoDetailsResponse(
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "movie_image") val movieImage: String?,
    @field:Json(name = "backdrop") val backdrop: String?,
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
    @field:Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @field:Json(name = "tmdb_id") val tmdbId: Any?  // Can be Int, String, or null
)
