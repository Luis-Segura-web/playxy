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
    @param:TypeConverters(Converters::class)
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
    val type: String, // "live", "vod", or "series"
    val orderIndex: Int = 0 // Preserve provider's order
)

@Entity(tableName = "cache_metadata")
data class CacheMetadata(
    @PrimaryKey val key: String,
    val lastUpdated: Long
)

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val channelId: String,
    val timestamp: Long
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val timestamp: Long
)

@Entity(tableName = "favorite_vod")
data class FavoriteVodEntity(
    @PrimaryKey val streamId: String,
    val timestamp: Long
)

@Entity(tableName = "recent_vod")
data class RecentVodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val streamId: String,
    val timestamp: Long
)

@Entity(tableName = "favorite_series")
data class FavoriteSeriesEntity(
    @PrimaryKey val seriesId: String,
    val timestamp: Long
)

@Entity(tableName = "recent_series")
data class RecentSeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val seriesId: String,
    val timestamp: Long
)

// Progreso de reproducción de películas
@Entity(tableName = "movie_progress")
data class MovieProgressEntity(
    @PrimaryKey val streamId: String,
    val positionMs: Long, // Posición en milisegundos
    val durationMs: Long, // Duración total
    val timestamp: Long // Última actualización
)

// Progreso de reproducción de series (último episodio visto)
@Entity(tableName = "series_progress")
data class SeriesProgressEntity(
    @PrimaryKey val seriesId: String,
    val lastEpisodeId: String,
    val lastSeasonNumber: Int,
    val lastEpisodeNumber: Int,
    val positionMs: Long, // Posición en el episodio
    val timestamp: Long // Última actualización
)

// Progreso de episodios individuales
@Entity(tableName = "episode_progress")
data class EpisodeProgressEntity(
    @PrimaryKey val episodeId: String, // ID del episodio
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val positionMs: Long, // Posición en milisegundos
    val durationMs: Long, // Duración total
    val timestamp: Long // Última actualización
)

