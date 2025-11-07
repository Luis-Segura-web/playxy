package com.iptv.playxy.data.repository

import com.iptv.playxy.data.api.IptvApiService
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
    private val apiService: IptvApiService,
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
            // Create a temporary api service with the base URL
            val response = apiService.validateCredentials(username, password)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    // Content loading operations
    suspend fun loadAllContent(username: String, password: String): Result<Unit> {
        return try {
            // Load all content types in parallel (simplified sequential for now)
            loadLiveStreams(username, password)
            loadVodStreams(username, password)
            loadSeries(username, password)
            loadCategories(username, password)
            
            // Update cache metadata
            val currentTime = System.currentTimeMillis()
            cacheMetadataDao.insertCacheMetadata(CacheMetadata("all_content", currentTime))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun loadLiveStreams(username: String, password: String) {
        val response = apiService.getLiveStreams(username, password)
        if (response.isSuccessful) {
            val streams = response.body()?.map { ResponseMapper.toLiveStream(it) } ?: emptyList()
            liveStreamDao.deleteAll()
            liveStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadVodStreams(username: String, password: String) {
        val response = apiService.getVodStreams(username, password)
        if (response.isSuccessful) {
            val streams = response.body()?.map { ResponseMapper.toVodStream(it) } ?: emptyList()
            vodStreamDao.deleteAll()
            vodStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadSeries(username: String, password: String) {
        val response = apiService.getSeries(username, password)
        if (response.isSuccessful) {
            val series = response.body()?.map { ResponseMapper.toSeries(it) } ?: emptyList()
            seriesDao.deleteAll()
            seriesDao.insertAll(series.map { EntityMapper.toEntity(it) })
        }
    }
    
    private suspend fun loadCategories(username: String, password: String) {
        categoryDao.deleteAll()
        
        // Load live categories
        val liveResponse = apiService.getLiveCategories(username, password)
        if (liveResponse.isSuccessful) {
            val categories = liveResponse.body()?.map { ResponseMapper.toCategory(it) } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "live") })
        }
        
        // Load VOD categories
        val vodResponse = apiService.getVodCategories(username, password)
        if (vodResponse.isSuccessful) {
            val categories = vodResponse.body()?.map { ResponseMapper.toCategory(it) } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "vod") })
        }
        
        // Load series categories
        val seriesResponse = apiService.getSeriesCategories(username, password)
        if (seriesResponse.isSuccessful) {
            val categories = seriesResponse.body()?.map { ResponseMapper.toCategory(it) } ?: emptyList()
            categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, "series") })
        }
    }
    
    // Cache management
    suspend fun isCacheValid(): Boolean {
        val metadata = cacheMetadataDao.getCacheMetadata("all_content")
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
}
