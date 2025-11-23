package com.iptv.playxy.data.repository

import com.iptv.playxy.data.api.ApiServiceFactory
import com.iptv.playxy.data.db.*
import com.iptv.playxy.domain.*
import com.iptv.playxy.util.EntityMapper
import com.iptv.playxy.util.ResponseMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing IPTV content data
 * Coordinates between local database and remote API
 */
@Singleton
class IptvRepository @Inject constructor(
    private val apiServiceFactory: ApiServiceFactory,
    private val database: PlayxyDatabase
) {
    private val userProfileDao = database.userProfileDao()
    private val liveStreamDao = database.liveStreamDao()
    private val vodStreamDao = database.vodStreamDao()
    private val seriesDao = database.seriesDao()
    private val categoryDao = database.categoryDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    
    // Cache expiration time (24 hours)
    private val cacheExpirationTime = 24 * 60 * 60 * 1000L
    private val liveCacheKey = "live_content"
    private val vodCacheKey = "vod_content"
    private val seriesCacheKey = "series_content"
    private val allCacheKey = "all_content"
    
    // User Profile operations
    suspend fun getProfile(): UserProfile? {
        return userProfileDao.getProfile()?.let { EntityMapper.toDomain(it) }
    }
    
    fun getProfileFlow(): Flow<UserProfile?> {
        return userProfileDao.getProfileFlow().map { it?.let { EntityMapper.toDomain(it) } }
    }
    
    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertProfile(EntityMapper.toEntity(profile))
    }
    
    suspend fun deleteProfile() {
        userProfileDao.deleteAllProfiles()
    }
    
    suspend fun validateCredentials(username: String, password: String, baseUrl: String): Boolean {
        return try {
            // Create API service with the provided base URL
            val apiService = apiServiceFactory.createService(baseUrl)
            val response = apiService.validateCredentials(username, password)
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Content loading operations
    suspend fun loadAllContent(username: String, password: String): Result<Unit> {
        return try {
            // Get the user profile to obtain the base URL
            val profile = userProfileDao.getProfile()
            if (profile == null) {
                return Result.failure(Exception("No user profile found"))
            }

            // Create API service with the user's base URL
            val apiService = apiServiceFactory.createService(profile.url)

            // Load all content types
            loadLiveStreams(apiService, username, password)
            loadVodStreams(apiService, username, password)
            loadSeries(apiService, username, password)
            loadCategories(apiService, username, password)

            // Update cache metadata
            val currentTime = System.currentTimeMillis()
            cacheMetadataDao.insertCacheMetadata(CacheMetadata(allCacheKey, currentTime))
            cacheMetadataDao.insertCacheMetadata(CacheMetadata(liveCacheKey, currentTime))
            cacheMetadataDao.insertCacheMetadata(CacheMetadata(vodCacheKey, currentTime))
            cacheMetadataDao.insertCacheMetadata(CacheMetadata(seriesCacheKey, currentTime))
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun loadLiveStreams(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val response = apiService.getLiveStreams(username, password)
        if (response.isSuccessful) {
            val streams = response.body()?.map { ResponseMapper.toLiveStream(it) } ?: emptyList()
            liveStreamDao.deleteAll()
            liveStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadVodStreams(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val response = apiService.getVodStreams(username, password)
        if (response.isSuccessful) {
            val streams = response.body()?.map { ResponseMapper.toVodStream(it) } ?: emptyList()
            vodStreamDao.deleteAll()
            vodStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadSeries(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val response = apiService.getSeries(username, password)
        if (response.isSuccessful) {
            val series = response.body()?.map { ResponseMapper.toSeries(it) } ?: emptyList()
            seriesDao.deleteAll()
            seriesDao.insertAll(series.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadCategories(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        categoryDao.deleteAll()
        
        // Load live categories
        val liveResponse = apiService.getLiveCategories(username, password)
        if (liveResponse.isSuccessful) {
            val categories = liveResponse.body()?.mapIndexed { index, it ->
                ResponseMapper.toCategory(it, index)
            } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "live") })
        }
        
        // Load VOD categories
        val vodResponse = apiService.getVodCategories(username, password)
        if (vodResponse.isSuccessful) {
            val categories = vodResponse.body()?.mapIndexed { index, it ->
                ResponseMapper.toCategory(it, index)
            } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "vod") })
        }
        
        // Load series categories
        val seriesResponse = apiService.getSeriesCategories(username, password)
        if (seriesResponse.isSuccessful) {
            val categories = seriesResponse.body()?.mapIndexed { index, it ->
                ResponseMapper.toCategory(it, index)
            } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "series") })
        }
    }

    /**
     * Refresh only live streams from the provider and update cache metadata
     */
    suspend fun refreshLiveStreams(): Result<Unit> {
        return refreshFromProvider(liveCacheKey) { apiService, username, password ->
            loadLiveStreams(apiService, username, password)
        }
    }

    /**
     * Refresh only VOD streams from the provider and update cache metadata
     */
    suspend fun refreshVodStreams(): Result<Unit> {
        return refreshFromProvider(vodCacheKey) { apiService, username, password ->
            loadVodStreams(apiService, username, password)
        }
    }

    /**
     * Refresh only series from the provider and update cache metadata
     */
    suspend fun refreshSeries(): Result<Unit> {
        return refreshFromProvider(seriesCacheKey) { apiService, username, password ->
            loadSeries(apiService, username, password)
        }
    }

    private suspend fun refreshFromProvider(
        cacheKey: String,
        loader: suspend (apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) -> Unit
    ): Result<Unit> {
        return try {
            val profile = userProfileDao.getProfile() ?: return Result.failure(Exception("No user profile found"))
            val apiService = apiServiceFactory.createService(profile.url)

            loader(apiService, profile.username, profile.password)

            val currentTime = System.currentTimeMillis()
            cacheMetadataDao.insertCacheMetadata(CacheMetadata(cacheKey, currentTime))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Cache management
    suspend fun isCacheValid(): Boolean {
        val metadata = cacheMetadataDao.getCacheMetadata(allCacheKey)
        return if (metadata != null) {
            val currentTime = System.currentTimeMillis()
            (currentTime - metadata.lastUpdated) < cacheExpirationTime
        } else {
            false
        }
    }
    
    suspend fun hasCachedContent(): Boolean {
        val liveStreams = liveStreamDao.getAllLiveStreams()
        val vodStreams = vodStreamDao.getAllVodStreams()
        val series = seriesDao.getAllSeries()
        return liveStreams.isNotEmpty() || vodStreams.isNotEmpty() || series.isNotEmpty()
    }
    
    suspend fun clearCache() {
        liveStreamDao.deleteAll()
        vodStreamDao.deleteAll()
        seriesDao.deleteAll()
        categoryDao.deleteAll()
        cacheMetadataDao.deleteAll()
    }
    
    suspend fun getLastProviderUpdateTime(): Long {
        val metadata = cacheMetadataDao.getCacheMetadata(allCacheKey)
        return metadata?.lastUpdated ?: 0L
    }

    suspend fun getLastLiveUpdateTime(): Long {
        val metadata = cacheMetadataDao.getCacheMetadata(liveCacheKey)
        return metadata?.lastUpdated ?: 0L
    }

    suspend fun getLastVodUpdateTime(): Long {
        val metadata = cacheMetadataDao.getCacheMetadata(vodCacheKey)
        return metadata?.lastUpdated ?: 0L
    }

    suspend fun getLastSeriesUpdateTime(): Long {
        val metadata = cacheMetadataDao.getCacheMetadata(seriesCacheKey)
        return metadata?.lastUpdated ?: 0L
    }
    
    // Content retrieval from cache
    suspend fun getLiveStreams(): List<LiveStream> {
        return liveStreamDao.getAllLiveStreams().map { EntityMapper.liveStreamToDomain(it) }
    }
    
    suspend fun getLiveStreamsByCategory(categoryId: String): List<LiveStream> {
        return liveStreamDao.getLiveStreamsByCategory(categoryId).map { EntityMapper.liveStreamToDomain(it) }
    }

    suspend fun getVodStreams(): List<VodStream> {
        return vodStreamDao.getAllVodStreams().map { EntityMapper.vodStreamToDomain(it) }
    }
    
    suspend fun getVodStreamsByCategory(categoryId: String): List<VodStream> {
        return vodStreamDao.getVodStreamsByCategory(categoryId).map { EntityMapper.vodStreamToDomain(it) }
    }

    suspend fun getSeries(): List<Series> {
        return seriesDao.getAllSeries().map { EntityMapper.seriesToDomain(it) }
    }
    
    suspend fun getSeriesByCategory(categoryId: String): List<Series> {
        return seriesDao.getSeriesByCategory(categoryId).map { EntityMapper.seriesToDomain(it) }
    }

    suspend fun getCategories(type: String): List<Category> {
        return categoryDao.getCategoriesByType(type).map { EntityMapper.categoryToDomain(it) }
    }

    suspend fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategories().map { EntityMapper.categoryToDomain(it) }
    }

    /**
     * Get series information including seasons and episodes
     * @param seriesId The series ID to fetch info for
     * @return SeriesInfo with seasons and episodes, or null if error
     */
    suspend fun getSeriesInfo(seriesId: String): SeriesInfo? {
        return try {
            // Get the user profile to obtain credentials and base URL
            val profile = userProfileDao.getProfile() ?: return null

            // Get the series from cache to have base info
            val seriesEntity = seriesDao.getAllSeries().find { it.seriesId == seriesId }
            val series = seriesEntity?.let { EntityMapper.seriesToDomain(it) } ?: return null

            // Create API service with the user's base URL
            val apiService = apiServiceFactory.createService(profile.url)

            // Fetch series info from API
            val response = apiService.getSeriesInfo(
                username = profile.username,
                password = profile.password,
                seriesId = seriesId
            )

            if (response.isSuccessful && response.body() != null) {
                ResponseMapper.toSeriesInfo(response.body()!!, series)
            } else {
                // Return empty series info if API call fails
                SeriesInfo(
                    seasons = emptyList(),
                    info = series,
                    episodesBySeason = emptyMap()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get detailed VOD information from the provider
     */
    suspend fun getVodInfo(vodId: String): VodInfo? {
        return try {
            // Get the user profile to obtain credentials and base URL
            val profile = userProfileDao.getProfile() ?: return null

            // Create API service with the user's base URL
            val apiService = apiServiceFactory.createService(profile.url)

            // Fetch VOD info from API
            val response = apiService.getVodInfo(
                username = profile.username,
                password = profile.password,
                vodId = vodId
            )

            if (response.isSuccessful && response.body() != null) {
                ResponseMapper.toVodInfo(response.body()!!)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
