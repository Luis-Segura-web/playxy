package com.iptv.playxy.util

import com.iptv.playxy.data.api.CategoryResponse
import com.iptv.playxy.data.api.LiveStreamResponse
import com.iptv.playxy.data.api.SeriesResponse
import com.iptv.playxy.data.api.VodStreamResponse
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.Series
import com.iptv.playxy.domain.VodStream

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
    
    fun toCategory(response: CategoryResponse): Category {
        return Category(
            categoryId = response.categoryId.orEmpty(),
            categoryName = response.categoryName.orEmpty(),
            parentId = response.parentId.orEmpty()
        )
    }
}
