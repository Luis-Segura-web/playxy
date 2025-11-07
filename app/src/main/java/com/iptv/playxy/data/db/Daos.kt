package com.iptv.playxy.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    suspend fun getProfile(): UserProfileEntity?
    
    @Query("SELECT * FROM user_profiles WHERE id = 1")
    fun getProfileFlow(): Flow<UserProfileEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)
    
    @Query("DELETE FROM user_profiles")
    suspend fun deleteAllProfiles()
}

@Dao
interface LiveStreamDao {
    @Query("SELECT * FROM live_streams")
    suspend fun getAllLiveStreams(): List<LiveStreamEntity>
    
    @Query("SELECT * FROM live_streams WHERE categoryId = :categoryId")
    suspend fun getLiveStreamsByCategory(categoryId: String): List<LiveStreamEntity>
    
    @Query("SELECT * FROM live_streams WHERE streamId = :streamId")
    suspend fun getLiveStreamsByStreamId(streamId: String): List<LiveStreamEntity>

    @Query("SELECT * FROM live_streams WHERE streamId = :streamId AND categoryId = :categoryId")
    suspend fun getLiveStream(streamId: String, categoryId: String): LiveStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(streams: List<LiveStreamEntity>)
    
    @Query("DELETE FROM live_streams")
    suspend fun deleteAll()
}

@Dao
interface VodStreamDao {
    @Query("SELECT * FROM vod_streams")
    suspend fun getAllVodStreams(): List<VodStreamEntity>
    
    @Query("SELECT * FROM vod_streams WHERE categoryId = :categoryId")
    suspend fun getVodStreamsByCategory(categoryId: String): List<VodStreamEntity>
    
    @Query("SELECT * FROM vod_streams WHERE streamId = :streamId")
    suspend fun getVodStreamsByStreamId(streamId: String): List<VodStreamEntity>

    @Query("SELECT * FROM vod_streams WHERE streamId = :streamId AND categoryId = :categoryId")
    suspend fun getVodStream(streamId: String, categoryId: String): VodStreamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(streams: List<VodStreamEntity>)
    
    @Query("DELETE FROM vod_streams")
    suspend fun deleteAll()
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series")
    suspend fun getAllSeries(): List<SeriesEntity>
    
    @Query("SELECT * FROM series WHERE categoryId = :categoryId")
    suspend fun getSeriesByCategory(categoryId: String): List<SeriesEntity>
    
    @Query("SELECT * FROM series WHERE seriesId = :seriesId")
    suspend fun getSeriesBySeriesId(seriesId: String): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE seriesId = :seriesId AND categoryId = :categoryId")
    suspend fun getSeries(seriesId: String, categoryId: String): SeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>)
    
    @Query("DELETE FROM series")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE key = :key")
    suspend fun getCacheMetadata(key: String): CacheMetadata?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: CacheMetadata)
    
    @Query("DELETE FROM cache_metadata")
    suspend fun deleteAll()
}
