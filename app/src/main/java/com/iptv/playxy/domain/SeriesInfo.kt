package com.iptv.playxy.domain

data class SeriesInfo(
    val seasons: List<Season> = emptyList(),
    val info: Series,
    val episodesBySeason: Map<String, List<Episode>> = emptyMap()
)

data class Season(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val cover: String? = null,
    val airDate: String? = null
)

data class Episode(
    val id: String,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String,
    val info: EpisodeInfo? = null,
    val customSid: String? = null,
    val added: String? = null,
    val season: Int,
    val directSource: String? = null
)

data class EpisodeInfo(
    val tmdbId: String? = null,
    val releaseDate: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val rating: Float = 0f,
    val cover: String? = null
)

