package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for series info endpoint (get_series_info)
 */
@JsonClass(generateAdapter = true)
data class SeriesInfoResponse(
    @Json(name = "seasons") val seasons: List<SeasonResponse>?,
    @Json(name = "episodes") val episodes: Map<String, List<EpisodeResponse>>?,
    @Json(name = "info") val info: SeriesInfoDetailsResponse?
)

@JsonClass(generateAdapter = true)
data class SeasonResponse(
    @Json(name = "id") val id: Any?,  // Season TMDB ID - Can be Int or String
    @Json(name = "season_number") val seasonNumber: Any?,  // Can be Int or String
    @Json(name = "name") val name: String?,
    @Json(name = "episode_count") val episodeCount: Any?,  // Can be Int or String
    @Json(name = "overview") val overview: String?,
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "cover_big") val coverBig: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeResponse(
    @Json(name = "id") val id: Any?,  // Can be Int or String
    @Json(name = "episode_num") val episodeNum: Any?,  // Can be Int or String
    @Json(name = "title") val title: String?,
    @Json(name = "container_extension") val containerExtension: String?,
    @Json(name = "info") val info: EpisodeInfoResponse?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "added") val added: Any?,  // Can be String or Long
    @Json(name = "season") val season: Any?,  // Can be Int or String
    @Json(name = "direct_source") val directSource: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeInfoResponse(
    @Json(name = "tmdb_id") val tmdbId: Any?,  // Can be Int, String, or null
    @Json(name = "releasedate") val releaseDate: String?,
    @Json(name = "plot") val plot: String?,
    @Json(name = "duration_secs") val durationSecs: Any?,  // Can be Int or String
    @Json(name = "duration") val duration: String?,
    // video/audio can be objects, empty arrays, or strings depending on the service
    @Json(name = "video") val video: Any?,
    @Json(name = "audio") val audio: Any?,
    @Json(name = "bitrate") val bitrate: Any?,  // Can be Int or String
    @Json(name = "rating") val rating: Any?,  // Can be String or number
    @Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @Json(name = "season") val season: Any?,  // Can be Int or String
    @Json(name = "cover") val cover: String?,
    @Json(name = "cover_big") val coverBig: String?,
    @Json(name = "movie_image") val movieImage: String?,
    @Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @Json(name = "cast") val cast: String?,
    @Json(name = "director") val director: String?
)

@JsonClass(generateAdapter = true)
data class SeriesInfoDetailsResponse(
    @Json(name = "name") val name: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "movie_image") val movieImage: String?,
    @Json(name = "backdrop") val backdrop: String?,
    @Json(name = "plot") val plot: String?,
    @Json(name = "cast") val cast: String?,
    @Json(name = "director") val director: String?,
    @Json(name = "genre") val genre: String?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "last_modified") val lastModified: Any?,  // Can be String or Long
    @Json(name = "rating") val rating: Any?,  // Can be String or number
    @Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @Json(name = "backdrop_path") val backdropPath: Any?,  // Can be List<String> or empty array
    @Json(name = "youtube_trailer") val youtubeTrailer: String?,
    @Json(name = "episode_run_time") val episodeRunTime: Any?,  // Can be String or Int
    @Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @Json(name = "tmdb_id") val tmdbId: Any?  // Can be Int, String, or null
)
