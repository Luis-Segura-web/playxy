package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * Response for series info endpoint (get_series_info)
 */
data class SeriesInfoResponse(
    @field:Json(name = "seasons") val seasons: List<SeasonResponse>?,
    @field:Json(name = "episodes") val episodes: Map<String, List<EpisodeResponse>>?,
    @field:Json(name = "info") val info: SeriesInfoDetailsResponse?
)

data class SeasonResponse(
    @field:Json(name = "season_number") val seasonNumber: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "episode_count") val episodeCount: String?,
    @field:Json(name = "overview") val overview: String?,
    @field:Json(name = "air_date") val airDate: String?,
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "cover_big") val coverBig: String?
)

data class EpisodeResponse(
    @field:Json(name = "id") val id: String?,
    @field:Json(name = "episode_num") val episodeNum: String?,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "info") val info: EpisodeInfoResponse?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "added") val added: String?,
    @field:Json(name = "season") val season: String?,
    @field:Json(name = "direct_source") val directSource: String?
)

data class EpisodeInfoResponse(
    @field:Json(name = "tmdb_id") val tmdbId: String?,
    @field:Json(name = "releasedate") val releaseDate: String?,
    @field:Json(name = "plot") val plot: String?,
    @field:Json(name = "duration_secs") val durationSecs: String?,
    @field:Json(name = "duration") val duration: String?,
    @field:Json(name = "video") val video: Map<String, Any>?,
    @field:Json(name = "audio") val audio: Map<String, Any>?,
    @field:Json(name = "bitrate") val bitrate: String?,
    @field:Json(name = "rating") val rating: String?,
    @field:Json(name = "season") val season: String?,
    @field:Json(name = "cover") val cover: String?,
    @field:Json(name = "cover_big") val coverBig: String?,
    @field:Json(name = "movie_image") val movieImage: String?
)

data class SeriesInfoDetailsResponse(
    @field:Json(name = "name") val name: String?,
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
    @field:Json(name = "category_id") val categoryId: String?,
    @field:Json(name = "tmdb_id") val tmdbId: String?
)

