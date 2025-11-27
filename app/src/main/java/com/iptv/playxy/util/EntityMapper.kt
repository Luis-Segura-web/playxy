package com.iptv.playxy.util

import com.iptv.playxy.data.db.*
import com.iptv.playxy.domain.*

/**
 * Mapper object to convert between database entities and domain models
 */
object EntityMapper {
    
    // User Profile mappings
    fun toEntity(profile: UserProfile): UserProfileEntity {
        return UserProfileEntity(
            id = profile.id,
            profileName = profile.profileName,
            username = profile.username,
            password = profile.password,
            url = profile.url,
            lastUpdated = profile.lastUpdated,
            isValid = profile.isValid,
            expiry = profile.expiry,
            maxConnections = profile.maxConnections,
            status = profile.status
        )
    }
    
    fun toDomain(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            profileName = entity.profileName,
            username = entity.username,
            password = entity.password,
            url = entity.url,
            lastUpdated = entity.lastUpdated,
            isValid = entity.isValid,
            expiry = entity.expiry,
            maxConnections = entity.maxConnections,
            status = entity.status
        )
    }
    
    // LiveStream mappings
    fun toEntity(stream: LiveStream): LiveStreamEntity {
        return LiveStreamEntity(
            streamId = stream.streamId,
            name = stream.name,
            streamIcon = stream.streamIcon,
            isAdult = stream.isAdult,
            categoryId = stream.categoryId,
            tvArchive = stream.tvArchive,
            epgChannelId = stream.epgChannelId,
            added = stream.added,
            customSid = stream.customSid,
            directSource = stream.directSource,
            tvArchiveDuration = stream.tvArchiveDuration
        )
    }
    
    fun liveStreamToDomain(entity: LiveStreamEntity): LiveStream {
        return LiveStream(
            streamId = entity.streamId,
            name = entity.name,
            streamIcon = entity.streamIcon,
            isAdult = entity.isAdult,
            categoryId = entity.categoryId,
            tvArchive = entity.tvArchive,
            epgChannelId = entity.epgChannelId,
            added = entity.added,
            customSid = entity.customSid,
            directSource = entity.directSource,
            tvArchiveDuration = entity.tvArchiveDuration
        )
    }
    
    // VodStream mappings
    fun toEntity(stream: VodStream): VodStreamEntity {
        return VodStreamEntity(
            streamId = stream.streamId,
            name = stream.name,
            streamIcon = stream.streamIcon,
            tmdbId = stream.tmdbId,
            rating = stream.rating,
            rating5Based = stream.rating5Based,
            containerExtension = stream.containerExtension,
            added = stream.added,
            isAdult = stream.isAdult,
            categoryId = stream.categoryId,
            customSid = stream.customSid,
            directSource = stream.directSource
        )
    }
    
    fun vodStreamToDomain(entity: VodStreamEntity): VodStream {
        return VodStream(
            streamId = entity.streamId,
            name = entity.name,
            streamIcon = entity.streamIcon,
            tmdbId = entity.tmdbId,
            rating = entity.rating,
            rating5Based = entity.rating5Based,
            containerExtension = entity.containerExtension,
            added = entity.added,
            isAdult = entity.isAdult,
            categoryId = entity.categoryId,
            customSid = entity.customSid,
            directSource = entity.directSource
        )
    }
    
    // Series mappings
    fun toEntity(series: Series): SeriesEntity {
        return SeriesEntity(
            seriesId = series.seriesId,
            name = series.name,
            cover = series.cover,
            plot = series.plot,
            cast = series.cast,
            director = series.director,
            genre = series.genre,
            releaseDate = series.releaseDate,
            rating = series.rating,
            rating5Based = series.rating5Based,
            backdropPath = Converters().fromStringList(series.backdropPath),
            youtubeTrailer = series.youtubeTrailer,
            episodeRunTime = series.episodeRunTime,
            categoryId = series.categoryId,
            tmdbId = series.tmdbId,
            lastModified = series.lastModified
        )
    }
    
    fun seriesToDomain(entity: SeriesEntity): Series {
        return Series(
            seriesId = entity.seriesId,
            name = entity.name,
            cover = entity.cover,
            plot = entity.plot,
            cast = entity.cast,
            director = entity.director,
            genre = entity.genre,
            releaseDate = entity.releaseDate,
            rating = entity.rating,
            rating5Based = entity.rating5Based,
            backdropPath = Converters().toStringList(entity.backdropPath),
            youtubeTrailer = entity.youtubeTrailer,
            episodeRunTime = entity.episodeRunTime,
            categoryId = entity.categoryId,
            tmdbId = entity.tmdbId,
            lastModified = entity.lastModified
        )
    }
    
    // Category mappings
    fun toEntity(category: Category, type: String): CategoryEntity {
        return CategoryEntity(
            categoryId = category.categoryId,
            categoryName = category.categoryName,
            parentId = category.parentId,
            type = type,
            orderIndex = 0
        )
    }
    
    fun categoryToDomain(entity: CategoryEntity): Category {
        return Category(
            categoryId = entity.categoryId,
            categoryName = entity.categoryName,
            parentId = entity.parentId
        )
    }

    // FavoriteChannel mappings
    fun toEntity(favorite: FavoriteChannel): FavoriteChannelEntity {
        return FavoriteChannelEntity(
            channelId = favorite.channelId,
            timestamp = favorite.timestamp
        )
    }
    
    fun favoriteChannelToDomain(entity: FavoriteChannelEntity): FavoriteChannel {
        return FavoriteChannel(
            channelId = entity.channelId,
            timestamp = entity.timestamp
        )
    }
    
    // RecentChannel mappings
    fun toEntity(recent: RecentChannel, id: Int = 0): RecentChannelEntity {
        return RecentChannelEntity(
            id = id,
            channelId = recent.channelId,
            timestamp = recent.timestamp
        )
    }
    
    fun recentChannelToDomain(entity: RecentChannelEntity): RecentChannel {
        return RecentChannel(
            channelId = entity.channelId,
            timestamp = entity.timestamp
        )
    }
}
