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

    @Query(
        """
        SELECT * FROM live_streams
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%')
          AND (:allowBlocked = 1 OR categoryId NOT IN (:blockedCategories))
          AND (:blockAdult = 0 OR isAdult = 0)
        ORDER BY
          CASE WHEN :sortOrder = 0 THEN rowid END ASC,
          CASE WHEN :sortOrder = 1 THEN name COLLATE NOCASE END ASC,
          CASE WHEN :sortOrder = 2 THEN name COLLATE NOCASE END DESC,
          rowid ASC
        """
    )
    fun pagingLiveStreams(
        categoryId: String?,
        searchQuery: String?,
        allowBlocked: Boolean,
        blockedCategories: List<String>,
        blockAdult: Boolean,
        sortOrder: Int
    ): androidx.paging.PagingSource<Int, LiveStreamEntity>

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
    
    @androidx.room.Query(
        """
        SELECT * FROM vod_streams
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%')
          AND (:allowBlocked = 1 OR categoryId NOT IN (:blockedCategories))
          AND (:blockAdult = 0 OR isAdult = 0)
        ORDER BY
          CASE WHEN :sortOrder = 0 THEN rowid END ASC,
          CASE WHEN :sortOrder = 1 THEN name END COLLATE NOCASE ASC,
          CASE WHEN :sortOrder = 2 THEN name END COLLATE NOCASE DESC,
          CASE WHEN :sortOrder = 3 THEN added END DESC,
          CASE WHEN :sortOrder = 4 THEN added END ASC,
          rowid ASC
        """
    )
    fun pagingVodStreams(
        categoryId: String?,
        searchQuery: String?,
        allowBlocked: Boolean,
        blockedCategories: List<String>,
        blockAdult: Boolean,
        sortOrder: Int
    ): androidx.paging.PagingSource<Int, VodStreamEntity>

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

    @Query(
        """
        SELECT * FROM series
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:searchQuery IS NULL OR name LIKE '%' || :searchQuery || '%')
          AND (:allowBlocked = 1 OR categoryId NOT IN (:blockedCategories))
        ORDER BY
          CASE WHEN :sortOrder = 0 THEN rowid END ASC,
          CASE WHEN :sortOrder = 1 THEN name COLLATE NOCASE END ASC,
          CASE WHEN :sortOrder = 2 THEN name COLLATE NOCASE END DESC,
          rowid ASC
        """
    )
    fun pagingSeries(
        categoryId: String?,
        searchQuery: String?,
        allowBlocked: Boolean,
        blockedCategories: List<String>,
        sortOrder: Int
    ): androidx.paging.PagingSource<Int, SeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<SeriesEntity>)
    
    @Query("DELETE FROM series")
    suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC")
    suspend fun getAllCategories(): List<CategoryEntity>
    
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY orderIndex ASC")
    suspend fun getCategoriesByType(type: String): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteByType(type: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface CacheMetadataDao {
    @Query("SELECT * FROM cache_metadata WHERE `key` = :key")
    suspend fun getCacheMetadata(`key`: String): CacheMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheMetadata(metadata: CacheMetadata)
    
    @Query("DELETE FROM cache_metadata")
    suspend fun deleteAll()
}

@Dao
interface FavoriteChannelDao {
    @Query("SELECT * FROM favorite_channels ORDER BY timestamp DESC")
    suspend fun getAllFavorites(): List<FavoriteChannelEntity>
    
    @Query("SELECT * FROM favorite_channels WHERE channelId = :channelId")
    suspend fun getFavorite(channelId: String): FavoriteChannelEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteChannelEntity)
    
    @Query("DELETE FROM favorite_channels WHERE channelId = :channelId")
    suspend fun deleteFavorite(channelId: String)
    
    @Query("DELETE FROM favorite_channels")
    suspend fun deleteAll()
}

@Dao
interface RecentChannelDao {
    @Query("SELECT * FROM recent_channels ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentChannels(): List<RecentChannelEntity>
    
    @Query("SELECT * FROM recent_channels WHERE channelId = :channelId")
    suspend fun getRecent(channelId: String): RecentChannelEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentChannelEntity)
    
    @Query("DELETE FROM recent_channels WHERE channelId = :channelId")
    suspend fun deleteRecent(channelId: String)

    @Query(
        """
        DELETE FROM recent_channels 
        WHERE channelId NOT IN (
            SELECT channelId FROM recent_channels ORDER BY timestamp DESC LIMIT :limit
        )
        """
    )
    suspend fun trim(limit: Int)
    
    @Query("DELETE FROM recent_channels")
    suspend fun deleteAll()
}

@Dao
interface FavoriteVodDao {
    @Query("SELECT * FROM favorite_vod ORDER BY timestamp DESC")
    suspend fun getAllFavorites(): List<FavoriteVodEntity>

    @Query("SELECT * FROM favorite_vod WHERE streamId = :streamId")
    suspend fun getFavorite(streamId: String): FavoriteVodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteVodEntity)

    @Query("DELETE FROM favorite_vod WHERE streamId = :streamId")
    suspend fun deleteFavorite(streamId: String)
}

@Dao
interface RecentVodDao {
    @Query("SELECT * FROM recent_vod ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecent(): List<RecentVodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentVodEntity)

    @Query("DELETE FROM recent_vod WHERE streamId = :streamId")
    suspend fun deleteByStream(streamId: String)

    @Query("DELETE FROM recent_vod")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM recent_vod 
        WHERE streamId NOT IN (
            SELECT streamId FROM recent_vod ORDER BY timestamp DESC LIMIT :limit
        )
        """
    )
    suspend fun trim(limit: Int)
}

@Dao
interface FavoriteSeriesDao {
    @Query("SELECT * FROM favorite_series ORDER BY timestamp DESC")
    suspend fun getAllFavorites(): List<FavoriteSeriesEntity>

    @Query("SELECT * FROM favorite_series WHERE seriesId = :seriesId")
    suspend fun getFavorite(seriesId: String): FavoriteSeriesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteSeriesEntity)

    @Query("DELETE FROM favorite_series WHERE seriesId = :seriesId")
    suspend fun deleteFavorite(seriesId: String)
}

@Dao
interface RecentSeriesDao {
    @Query("SELECT * FROM recent_series ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecent(): List<RecentSeriesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentSeriesEntity)

    @Query("DELETE FROM recent_series WHERE seriesId = :seriesId")
    suspend fun deleteBySeries(seriesId: String)

    @Query("DELETE FROM recent_series")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM recent_series 
        WHERE seriesId NOT IN (
            SELECT seriesId FROM recent_series ORDER BY timestamp DESC LIMIT :limit
        )
        """
    )
    suspend fun trim(limit: Int)
}

@Dao
interface MovieProgressDao {
    @Query("SELECT * FROM movie_progress WHERE streamId = :streamId")
    suspend fun getProgress(streamId: String): MovieProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: MovieProgressEntity)

    @Query("DELETE FROM movie_progress WHERE streamId = :streamId")
    suspend fun deleteProgress(streamId: String)

    @Query("DELETE FROM movie_progress")
    suspend fun deleteAll()
}

@Dao
interface SeriesProgressDao {
    @Query("SELECT * FROM series_progress WHERE seriesId = :seriesId")
    suspend fun getProgress(seriesId: String): SeriesProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: SeriesProgressEntity)

    @Query("DELETE FROM series_progress WHERE seriesId = :seriesId")
    suspend fun deleteProgress(seriesId: String)

    @Query("DELETE FROM series_progress")
    suspend fun deleteAll()
}

@Dao
interface EpisodeProgressDao {
    @Query("SELECT * FROM episode_progress WHERE episodeId = :episodeId")
    suspend fun getProgress(episodeId: String): EpisodeProgressEntity?

    @Query("SELECT * FROM episode_progress WHERE seriesId = :seriesId")
    suspend fun getAllProgressForSeries(seriesId: String): List<EpisodeProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: EpisodeProgressEntity)

    @Query("DELETE FROM episode_progress WHERE episodeId = :episodeId")
    suspend fun deleteProgress(episodeId: String)

    @Query("DELETE FROM episode_progress WHERE seriesId = :seriesId")
    suspend fun deleteProgressForSeries(seriesId: String)

    @Query("DELETE FROM episode_progress")
    suspend fun deleteAll()
}
