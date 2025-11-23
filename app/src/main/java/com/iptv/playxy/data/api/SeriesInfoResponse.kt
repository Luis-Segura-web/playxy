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
    @Json(name = "season_number") val seasonNumber: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "episode_count") val episodeCount: String?,
    @Json(name = "overview") val overview: String?,
    @Json(name = "air_date") val airDate: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "cover_big") val coverBig: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "episode_num") val episodeNum: String?,
    @Json(name = "title") val title: String?,
    @Json(name = "container_extension") val containerExtension: String?,
    @Json(name = "info") val info: EpisodeInfoResponse?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "added") val added: String?,
    @Json(name = "season") val season: String?,
    @Json(name = "direct_source") val directSource: String?
)

@JsonClass(generateAdapter = true)
data class EpisodeInfoResponse(
    @Json(name = "tmdb_id") val tmdbId: String?,
    @Json(name = "releasedate") val releaseDate: String?,
    @Json(name = "plot") val plot: String?,
    @Json(name = "duration_secs") val durationSecs: String?,
    @Json(name = "duration") val duration: String?,
    @Json(name = "video") val video: Map<String, Any>?,
    @Json(name = "audio") val audio: Map<String, Any>?,
    @Json(name = "bitrate") val bitrate: String?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "season") val season: String?,
    @Json(name = "cover") val cover: String?,
    @Json(name = "cover_big") val coverBig: String?,
    @Json(name = "movie_image") val movieImage: String?
)

@JsonClass(generateAdapter = true)
data class SeriesInfoDetailsResponse(
    @Json(name = "name") val name: String?,
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
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "tmdb_id") val tmdbId: String?
)
