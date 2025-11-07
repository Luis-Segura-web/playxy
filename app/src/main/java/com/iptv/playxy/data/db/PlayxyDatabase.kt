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
        RecentChannelEntity::class
    ],
    version = 3,
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
}
