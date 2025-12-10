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
    
    // Helper functions to safely convert Any? to specific types
    private fun Any?.toSafeString(): String = when (this) {
        null -> ""
        is String -> this
        is Double -> if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
        is Float -> if (this == this.toLong().toFloat()) this.toLong().toString() else this.toString()
        is Number -> this.toLong().toString()
        else -> this.toString()
    }
    
    private fun Any?.toSafeStringOrNull(): String? = when (this) {
        null -> null
        is String -> this.ifBlank { null }
        is Double -> if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()
        is Float -> if (this == this.toLong().toFloat()) this.toLong().toString() else this.toString()
        is Number -> this.toLong().toString()
        else -> this.toString().ifBlank { null }
    }
    
    private fun Any?.toSafeInt(): Int = when (this) {
        null -> 0
        is Number -> this.toInt()
        is String -> this.toIntOrNull() ?: 0
        else -> 0
    }
    
    private fun Any?.toSafeFloat(): Float = when (this) {
        null -> 0f
        is Number -> this.toFloat()
        is String -> this.toFloatOrNull() ?: 0f
        else -> 0f
    }
    
    private fun Any?.toSafeBool(trueValues: List<String> = listOf("1", "true")): Boolean = when (this) {
        null -> false
        is Boolean -> this
        is Number -> this.toInt() != 0
        is String -> trueValues.any { this.equals(it, ignoreCase = true) }
        else -> false
    }
    
    @Suppress("UNCHECKED_CAST")
    private fun Any?.toSafeStringList(): List<String> = when (this) {
        null -> emptyList()
        is List<*> -> this.filterIsInstance<String>()
        is String -> if (this.isNotBlank()) listOf(this) else emptyList()
        else -> emptyList()
    }
    
    fun toLiveStream(response: LiveStreamResponse): LiveStream {
        return LiveStream(
            streamId = response.streamId.toSafeString(),
            name = response.name.orEmpty(),
            streamIcon = response.streamIcon,
            isAdult = response.isAdult.toSafeBool(),
            categoryId = response.categoryId.toSafeString(),
            tvArchive = response.tvArchive.toSafeBool(),
            epgChannelId = response.epgChannelId,
            added = response.added.toSafeStringOrNull(),
            customSid = response.customSid,
            directSource = response.directSource,
            tvArchiveDuration = response.tvArchiveDuration.toSafeInt()
        )
    }
    
    fun toVodStream(response: VodStreamResponse): VodStream {
        return VodStream(
            streamId = response.streamId.toSafeString(),
            name = response.name.orEmpty(),
            streamIcon = response.streamIcon,
            tmdbId = TmdbIdValidator.sanitizeTmdbId(response.tmdbId.toSafeStringOrNull()),
            rating = response.rating.toSafeFloat(),
            rating5Based = response.rating5Based.toSafeFloat(),
            containerExtension = response.containerExtension.orEmpty(),
            added = response.added.toSafeStringOrNull(),
            isAdult = response.isAdult.toSafeBool(),
            categoryId = response.categoryId.toSafeString(),
            customSid = response.customSid,
            directSource = response.directSource
        )
    }
    
    fun toSeries(response: SeriesResponse): Series {
        return Series(
            seriesId = response.seriesId.toSafeString(),
            name = response.name.orEmpty(),
            cover = response.cover,
            plot = response.plot,
            cast = response.cast,
            director = response.director,
            genre = response.genre,
            releaseDate = response.releaseDate,
            rating = response.rating.toSafeFloat(),
            rating5Based = response.rating5Based.toSafeFloat(),
            backdropPath = response.backdropPath.toSafeStringList(),
            youtubeTrailer = response.youtubeTrailer,
            episodeRunTime = response.episodeRunTime.toSafeStringOrNull(),
            categoryId = response.categoryId.toSafeString(),
            tmdbId = TmdbIdValidator.sanitizeTmdbId(response.tmdbId.toSafeStringOrNull()),
            lastModified = response.lastModified.toSafeStringOrNull()
        )
    }
    
    fun toCategory(response: CategoryResponse, orderIndex: Int = 0): Category {
        return Category(
            categoryId = response.categoryId.toSafeString(),
            categoryName = response.categoryName.orEmpty(),
            parentId = response.parentId.toSafeString()
        )
    }

    fun toSeriesInfo(response: SeriesInfoResponse, originalSeries: Series): SeriesInfo {
        val allSeasons = response.seasons?.map { toSeason(it) }?.sortedBy { it.seasonNumber } ?: emptyList()
        val episodes = response.episodes ?: emptyMap()
        val infoDetails = response.info

        // Map episodes by season, filtering out empty episode lists
        val episodesBySeason = episodes
            .filterValues { it.isNotEmpty() }  // Only keep seasons with actual episodes
            .mapValues { (_, episodeList) ->
                episodeList.map { toEpisode(it) }.sortedBy { it.episodeNum }
            }

        // Filter seasons to only include those that have episodes
        // This ensures compatibility with providers that list seasons without providing episode data
        val availableSeasonKeys = episodesBySeason.keys.mapNotNull { it.toIntOrNull() }.toSet()
        val seasons = allSeasons.filter { season ->
            val hasEpisodes = availableSeasonKeys.contains(season.seasonNumber)
            if (!hasEpisodes && allSeasons.size > 1) {
                // Log seasons that are excluded (only if there are multiple seasons)
                android.util.Log.d("ResponseMapper", "Filtering out season ${season.seasonNumber} '${season.name}' - no episodes available in response")
            }
            hasEpisodes
        }

        val backdrops = when {
            infoDetails?.backdropPath != null -> infoDetails.backdropPath.toSafeStringList().takeIf { it.isNotEmpty() }
            !infoDetails?.backdrop.isNullOrBlank() -> listOf(infoDetails.backdrop)
            originalSeries.backdropPath.isNotEmpty() -> originalSeries.backdropPath
            else -> null
        } ?: emptyList()

        // Get tmdb_id from info response or keep from original series
        val tmdbId = TmdbIdValidator.sanitizeTmdbId(infoDetails?.tmdbId.toSafeStringOrNull()) 
            ?: originalSeries.tmdbId

        val mergedSeries = originalSeries.copy(
            name = infoDetails?.name ?: originalSeries.name,
            cover = infoDetails?.cover ?: infoDetails?.movieImage ?: originalSeries.cover,
            plot = infoDetails?.plot ?: originalSeries.plot,
            cast = infoDetails?.cast ?: originalSeries.cast,
            director = infoDetails?.director ?: originalSeries.director,
            genre = infoDetails?.genre ?: originalSeries.genre,
            releaseDate = infoDetails?.releaseDate ?: originalSeries.releaseDate,
            rating = infoDetails?.rating.toSafeFloat().takeIf { it > 0f } ?: originalSeries.rating,
            rating5Based = infoDetails?.rating5Based.toSafeFloat().takeIf { it > 0f } ?: originalSeries.rating5Based,
            backdropPath = backdrops,
            youtubeTrailer = infoDetails?.youtubeTrailer ?: originalSeries.youtubeTrailer,
            episodeRunTime = infoDetails?.episodeRunTime.toSafeStringOrNull() ?: originalSeries.episodeRunTime,
            lastModified = infoDetails?.lastModified.toSafeStringOrNull() ?: originalSeries.lastModified,
            tmdbId = tmdbId
        )

        // Log summary for debugging
        android.util.Log.d("ResponseMapper", "toSeriesInfo: ${allSeasons.size} seasons in response, ${seasons.size} with episodes, ${episodesBySeason.values.sumOf { it.size }} total episodes")

        return SeriesInfo(
            seasons = seasons,
            info = mergedSeries,
            episodesBySeason = episodesBySeason
        )
    }

    fun toSeason(response: SeasonResponse): Season {
        return Season(
            id = response.id.toSafeStringOrNull(),
            seasonNumber = response.seasonNumber.toSafeInt(),
            name = response.name ?: "Temporada ${response.seasonNumber.toSafeString().ifBlank { "?" }}",
            episodeCount = response.episodeCount.toSafeInt(),
            cover = response.cover ?: response.coverBig,
            airDate = response.airDate,
            overview = response.overview
        )
    }

    fun toEpisode(response: EpisodeResponse): Episode {
        return Episode(
            id = response.id.toSafeString(),
            episodeNum = response.episodeNum.toSafeInt(),
            title = response.title ?: "Episodio ${response.episodeNum.toSafeString().ifBlank { "?" }}",
            containerExtension = response.containerExtension.orEmpty(),
            info = response.info?.let { toEpisodeInfo(it) },
            customSid = response.customSid,
            added = response.added.toSafeStringOrNull(),
            season = response.season.toSafeInt(),
            directSource = response.directSource
        )
    }

    fun toEpisodeInfo(response: EpisodeInfoResponse): EpisodeInfo {
        return EpisodeInfo(
            tmdbId = TmdbIdValidator.sanitizeTmdbId(response.tmdbId.toSafeStringOrNull()),
            releaseDate = response.releaseDate,
            plot = response.plot,
            duration = response.duration ?: response.durationSecs.toSafeInt().takeIf { it > 0 }?.let {
                "${it / 60} min"
            },
            rating = response.rating.toSafeFloat(),
            cover = response.cover ?: response.coverBig ?: response.movieImage
        )
    }

    fun toVodInfo(response: VodInfoResponse): VodInfo? {
        val info = response.info ?: return null
        val backdrops = when {
            info.backdropPath != null -> info.backdropPath.toSafeStringList().takeIf { it.isNotEmpty() }
            !info.backdrop.isNullOrBlank() -> listOf(info.backdrop)
            else -> null
        }
        return VodInfo(
            tmdbId = TmdbIdValidator.sanitizeTmdbId(info.tmdbId.toSafeStringOrNull()),
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
            rating = info.rating.toSafeStringOrNull(),
            rating5Based = info.rating5Based.toSafeFloat().takeIf { it > 0f }?.toDouble(),
            country = info.country,
            genre = info.genre,
            backdropPath = backdrops,
            durationSecs = info.durationSecs.toSafeInt().takeIf { it > 0 },
            video = info.video,
            audio = info.audio,
            bitrate = info.bitrate.toSafeInt().takeIf { it > 0 }
        )
    }
}
