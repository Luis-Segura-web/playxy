package com.iptv.playxy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val profileName: String,
    val username: String,
    val password: String,
    val url: String,
    val lastUpdated: Long,
    val isValid: Boolean
)

@Entity(
    tableName = "live_streams",
    primaryKeys = ["streamId", "categoryId"]
)
data class LiveStreamEntity(
    val streamId: String,
    val categoryId: String,
    val name: String,
    val streamIcon: String?,
    val isAdult: Boolean,
    val tvArchive: Boolean,
    val epgChannelId: String?,
    val added: String?,
    val customSid: String?,
    val directSource: String?,
    val tvArchiveDuration: Int
)

@Entity(
    tableName = "vod_streams",
    primaryKeys = ["streamId", "categoryId"]
)
data class VodStreamEntity(
    val streamId: String,
    val categoryId: String,
    val name: String,
    val streamIcon: String?,
    val tmdbId: String?,
    val rating: Float,
    val rating5Based: Float,
    val containerExtension: String,
    val added: String?,
    val isAdult: Boolean,
    val customSid: String?,
    val directSource: String?
)

@Entity(
    tableName = "series",
    primaryKeys = ["seriesId", "categoryId"]
)
data class SeriesEntity(
    val seriesId: String,
    val categoryId: String,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: Float,
    val rating5Based: Float,
    @TypeConverters(Converters::class)
    val backdropPath: String,
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val tmdbId: String?,
    val lastModified: String?
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: String,
    val type: String // "live", "vod", or "series"
)

@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey val key: String,
    val lastUpdated: Long
)
