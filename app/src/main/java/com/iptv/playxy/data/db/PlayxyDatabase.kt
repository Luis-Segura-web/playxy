package com.iptv.playxy.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfileEntity::class,
        LiveStreamEntity::class,
        VodStreamEntity::class,
        SeriesEntity::class,
        CategoryEntity::class,
        CacheMetadata::class,
        FavoriteChannelEntity::class,
        RecentChannelEntity::class,
        FavoriteVodEntity::class,
        RecentVodEntity::class,
        FavoriteSeriesEntity::class,
        RecentSeriesEntity::class,
        MovieProgressEntity::class,
        SeriesProgressEntity::class,
        EpisodeProgressEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PlayxyDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun liveStreamDao(): LiveStreamDao
    abstract fun vodStreamDao(): VodStreamDao
    abstract fun seriesDao(): SeriesDao
    abstract fun categoryDao(): CategoryDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun favoriteChannelDao(): FavoriteChannelDao
    abstract fun recentChannelDao(): RecentChannelDao
    abstract fun favoriteVodDao(): FavoriteVodDao
    abstract fun recentVodDao(): RecentVodDao
    abstract fun favoriteSeriesDao(): FavoriteSeriesDao
    abstract fun recentSeriesDao(): RecentSeriesDao
    abstract fun movieProgressDao(): MovieProgressDao
    abstract fun seriesProgressDao(): SeriesProgressDao
    abstract fun episodeProgressDao(): EpisodeProgressDao
}
