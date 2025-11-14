package com.iptv.playxy.util

import com.iptv.playxy.data.api.CategoryResponse
import com.iptv.playxy.data.api.LiveStreamResponse
import com.iptv.playxy.data.api.SeriesResponse
import com.iptv.playxy.data.api.VodStreamResponse
import com.iptv.playxy.data.api.SeriesInfoResponse
import com.iptv.playxy.data.api.SeasonResponse
import com.iptv.playxy.data.api.EpisodeResponse
import com.iptv.playxy.data.api.EpisodeInfoResponse
import com.iptv.playxy.data.api.VodInfoResponse
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.Series
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.domain.SeriesInfo
import com.iptv.playxy.domain.Season
import com.iptv.playxy.domain.Episode
import com.iptv.playxy.domain.EpisodeInfo
import com.iptv.playxy.domain.VodInfo

/**
 * Mapper object to convert API response models to domain models
 * Handles all type conversions and null safety
 */
object ResponseMapper {
    
    fun toLiveStream(response: LiveStreamResponse): LiveStream {
        return LiveStream(
            streamId = response.streamId.orEmpty(),
            name = response.name.orEmpty(),
            streamIcon = response.streamIcon,
            isAdult = response.isAdult?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: false,
            categoryId = response.categoryId.orEmpty(),
            tvArchive = response.tvArchive?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: false,
            epgChannelId = response.epgChannelId,
            added = response.added,
            customSid = response.customSid,
            directSource = response.directSource,
            tvArchiveDuration = response.tvArchiveDuration?.toIntOrNull() ?: 0
        )
    }
    
    fun toVodStream(response: VodStreamResponse): VodStream {
        return VodStream(
            streamId = response.streamId.orEmpty(),
            name = response.name.orEmpty(),
            streamIcon = response.streamIcon,
            tmdbId = response.tmdbId,
            rating = response.rating?.toFloatOrNull() ?: 0f,
            rating5Based = response.rating5Based?.toFloatOrNull() ?: 0f,
            containerExtension = response.containerExtension.orEmpty(),
            added = response.added,
            isAdult = response.isAdult?.let { it == "1" || it.equals("true", ignoreCase = true) } ?: false,
            categoryId = response.categoryId.orEmpty(),
            customSid = response.customSid,
            directSource = response.directSource
        )
    }
    
    fun toSeries(response: SeriesResponse): Series {
        return Series(
            seriesId = response.seriesId.orEmpty(),
            name = response.name.orEmpty(),
            cover = response.cover,
            plot = response.plot,
            cast = response.cast,
            director = response.director,
            genre = response.genre,
            releaseDate = response.releaseDate,
            rating = response.rating?.toFloatOrNull() ?: 0f,
            rating5Based = response.rating5Based?.toFloatOrNull() ?: 0f,
            backdropPath = response.backdropPath ?: emptyList(),
            youtubeTrailer = response.youtubeTrailer,
            episodeRunTime = response.episodeRunTime,
            categoryId = response.categoryId.orEmpty(),
            tmdbId = response.tmdbId,
            lastModified = response.lastModified
        )
    }
    
    fun toCategory(response: CategoryResponse, orderIndex: Int = 0): Category {
        return Category(
            categoryId = response.categoryId.orEmpty(),
            categoryName = response.categoryName.orEmpty(),
            parentId = response.parentId.orEmpty()
        )
    }

    fun toSeriesInfo(response: SeriesInfoResponse, originalSeries: Series): SeriesInfo {
        val seasons = response.seasons?.map { toSeason(it) }?.sortedBy { it.seasonNumber } ?: emptyList()
        val episodes = response.episodes ?: emptyMap()

        return SeriesInfo(
            seasons = seasons,
            info = originalSeries,
            episodesBySeason = episodes.mapValues { (_, episodeList) ->
                episodeList.map { toEpisode(it) }.sortedBy { it.episodeNum }
            }
        )
    }

    fun toSeason(response: SeasonResponse): Season {
        return Season(
            seasonNumber = response.seasonNumber?.toIntOrNull() ?: 0,
            name = response.name ?: "Temporada ${response.seasonNumber ?: "?"}",
            episodeCount = response.episodeCount?.toIntOrNull() ?: 0,
            cover = response.cover ?: response.coverBig,
            airDate = response.airDate
        )
    }

    fun toEpisode(response: EpisodeResponse): Episode {
        return Episode(
            id = response.id.orEmpty(),
            episodeNum = response.episodeNum?.toIntOrNull() ?: 0,
            title = response.title ?: "Episodio ${response.episodeNum ?: "?"}",
            containerExtension = response.containerExtension.orEmpty(),
            info = response.info?.let { toEpisodeInfo(it) },
            customSid = response.customSid,
            added = response.added,
            season = response.season?.toIntOrNull() ?: 0,
            directSource = response.directSource
        )
    }

    fun toEpisodeInfo(response: EpisodeInfoResponse): EpisodeInfo {
        return EpisodeInfo(
            tmdbId = response.tmdbId,
            releaseDate = response.releaseDate,
            plot = response.plot,
            duration = response.duration ?: response.durationSecs?.let {
                val seconds = it.toIntOrNull() ?: 0
                "${seconds / 60} min"
            },
            rating = response.rating?.toFloatOrNull() ?: 0f,
            cover = response.cover ?: response.coverBig ?: response.movieImage
        )
    }

    fun toVodInfo(response: VodInfoResponse): VodInfo? {
        val info = response.info ?: return null
        return VodInfo(
            tmdbId = info.tmdbId,
            name = info.name ?: "",
            originalName = info.originalName,
            coverBig = info.coverBig,
            movieImage = info.movieImage,
            releaseDate = info.releaseDate,
            duration = info.duration,
            youtubeTrailer = info.youtubeTrailer,
            director = info.director,
            actors = info.actors,
            cast = info.cast,
            description = info.description,
            plot = info.plot,
            age = info.age,
            mpaaRating = info.mpaaRating,
            rating = info.rating,
            rating5Based = info.rating5Based,
            country = info.country,
            genre = info.genre,
            backdropPath = info.backdropPath,
            durationSecs = info.durationSecs,
            video = info.video,
            audio = info.audio,
            bitrate = info.bitrate
        )
    }
}
