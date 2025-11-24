package com.iptv.playxy.data.repository

import com.iptv.playxy.data.api.ApiServiceFactory
import com.iptv.playxy.data.db.*
import com.iptv.playxy.domain.*
import com.iptv.playxy.util.EntityMapper
import com.iptv.playxy.util.ResponseMapper
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing IPTV content data
 * Coordinates between local database and remote API
 */
@Singleton
class IptvRepository @Inject constructor(
    private val apiServiceFactory: ApiServiceFactory,
    private val database: PlayxyDatabase,
    private val prefs: PreferencesManager
) {
    private val userProfileDao = database.userProfileDao()
    private val liveStreamDao = database.liveStreamDao()
    private val vodStreamDao = database.vodStreamDao()
    private val seriesDao = database.seriesDao()
    private val categoryDao = database.categoryDao()
    private val cacheMetadataDao = database.cacheMetadataDao()
    private val recentChannelDao = database.recentChannelDao()
    private val recentVodDao = database.recentVodDao()
    private val recentSeriesDao = database.recentSeriesDao()
    
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
        return loadAllContent(username, password) {}
    }

    suspend fun loadAllContent(
        username: String,
        password: String,
        onStep: (ContentLoadStage) -> Unit
    ): Result<Unit> {
        return try {
            // Get the user profile to obtain the base URL
            val profile = userProfileDao.getProfile()
            if (profile == null) {
                return Result.failure(Exception("No user profile found"))
            }

            // Create API service with the user's base URL
            onStep(ContentLoadStage.CONNECTING)
            val apiService = apiServiceFactory.createService(profile.url)

            // Descargas concurrentes, pero avances de UI secuenciales por tipo.
            coroutineScope {
                // Lanzamos descargas en paralelo
                val liveStreamsDeferred = async { downloadLiveStreams(apiService, username, password) }
                val liveCategoriesDeferred = async { downloadLiveCategories(apiService, username, password) }
                val vodStreamsDeferred = async { downloadVodStreams(apiService, username, password) }
                val vodCategoriesDeferred = async { downloadVodCategories(apiService, username, password) }
                val seriesStreamsDeferred = async { downloadSeries(apiService, username, password) }
                val seriesCategoriesDeferred = async { downloadSeriesCategories(apiService, username, password) }

                onStep(ContentLoadStage.DOWNLOADING_LIVE)
                val liveStreams = liveStreamsDeferred.await()
                val liveCategories = liveCategoriesDeferred.await()
                onStep(ContentLoadStage.PROCESSING_LIVE)
                awaitAll(
                    async { persistLiveStreams(liveStreams) },
                    async { persistCategories(liveCategories, "live") }
                )

                onStep(ContentLoadStage.DOWNLOADING_VOD)
                val vodStreams = vodStreamsDeferred.await()
                val vodCategories = vodCategoriesDeferred.await()
                onStep(ContentLoadStage.PROCESSING_VOD)
                awaitAll(
                    async { persistVodStreams(vodStreams) },
                    async { persistCategories(vodCategories, "vod") }
                )

                onStep(ContentLoadStage.DOWNLOADING_SERIES)
                val seriesStreams = seriesStreamsDeferred.await()
                val seriesCategories = seriesCategoriesDeferred.await()
                onStep(ContentLoadStage.PROCESSING_SERIES)
                awaitAll(
                    async { persistSeries(seriesStreams) },
                    async { persistCategories(seriesCategories, "series") }
                )

                onStep(ContentLoadStage.LOADING_CATEGORIES)

                // Update cache metadata
                val currentTime = System.currentTimeMillis()
                cacheMetadataDao.insertCacheMetadata(CacheMetadata(allCacheKey, currentTime))
                cacheMetadataDao.insertCacheMetadata(CacheMetadata(liveCacheKey, currentTime))
                cacheMetadataDao.insertCacheMetadata(CacheMetadata(vodCacheKey, currentTime))
                cacheMetadataDao.insertCacheMetadata(CacheMetadata(seriesCacheKey, currentTime))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private suspend fun loadLiveStreams(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val streams = downloadLiveStreams(apiService, username, password)
        persistLiveStreams(streams)
    }
    
    private suspend fun loadVodStreams(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val streams = downloadVodStreams(apiService, username, password)
        persistVodStreams(streams)
    }
    
    private suspend fun loadSeries(apiService: com.iptv.playxy.data.api.IptvApiService, username: String, password: String) {
        val series = downloadSeries(apiService, username, password)
        persistSeries(series)
    }

    private suspend fun downloadLiveStreams(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<LiveStream> {
        val response = apiService.getLiveStreams(username, password)
        return if (response.isSuccessful) {
            response.body()?.map { ResponseMapper.toLiveStream(it) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun persistLiveStreams(streams: List<LiveStream>) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                liveStreamDao.deleteAll()
                liveStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
            }
        }
    }

    private suspend fun downloadVodStreams(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<VodStream> {
        val response = apiService.getVodStreams(username, password)
        return if (response.isSuccessful) {
            response.body()?.map { ResponseMapper.toVodStream(it) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun persistVodStreams(streams: List<VodStream>) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                vodStreamDao.deleteAll()
                vodStreamDao.insertAll(streams.map { EntityMapper.toEntity(it) })
            }
        }
    }

    private suspend fun downloadSeries(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<Series> {
        val response = apiService.getSeries(username, password)
        return if (response.isSuccessful) {
            response.body()?.map { ResponseMapper.toSeries(it) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun persistSeries(series: List<Series>) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                seriesDao.deleteAll()
                seriesDao.insertAll(series.map { EntityMapper.toEntity(it) })
            }
        }
    }

    private suspend fun downloadLiveCategories(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<Category> {
        val response = apiService.getLiveCategories(username, password)
        return if (response.isSuccessful) {
            response.body()?.mapIndexed { index, it -> ResponseMapper.toCategory(it, index) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun downloadVodCategories(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<Category> {
        val response = apiService.getVodCategories(username, password)
        return if (response.isSuccessful) {
            response.body()?.mapIndexed { index, it -> ResponseMapper.toCategory(it, index) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun downloadSeriesCategories(
        apiService: com.iptv.playxy.data.api.IptvApiService,
        username: String,
        password: String
    ): List<Category> {
        val response = apiService.getSeriesCategories(username, password)
        return if (response.isSuccessful) {
            response.body()?.mapIndexed { index, it -> ResponseMapper.toCategory(it, index) } ?: emptyList()
        } else emptyList()
    }

    private suspend fun persistCategories(categories: List<Category>, type: String) {
        if (categories.isEmpty()) return
        withContext(Dispatchers.IO) {
            database.withTransaction {
                categoryDao.deleteByType(type)
                categoryDao.insertAll(categories.map { EntityMapper.toEntity(it, type) })
            }
        }
    }

    suspend fun getRecentsLimit(): Int = withContext(Dispatchers.IO) { prefs.getRecentsLimit() }

    suspend fun updateRecentsLimit(limit: Int) = withContext(Dispatchers.IO) {
        prefs.setRecentsLimit(limit)
        trimAllRecents(limit)
    }

    private suspend fun trimAllRecents(limit: Int) {
        recentChannelDao.trim(limit)
        recentVodDao.trim(limit)
        recentSeriesDao.trim(limit)
    }

    suspend fun clearRecentChannels() = withContext(Dispatchers.IO) { recentChannelDao.deleteAll() }
    suspend fun clearRecentVod() = withContext(Dispatchers.IO) { recentVodDao.deleteAll() }
    suspend fun clearRecentSeries() = withContext(Dispatchers.IO) { recentSeriesDao.deleteAll() }
    suspend fun clearAllRecents() = withContext(Dispatchers.IO) {
        recentChannelDao.deleteAll()
        recentVodDao.deleteAll()
        recentSeriesDao.deleteAll()
    }

    suspend fun isParentalControlEnabled(): Boolean = withContext(Dispatchers.IO) { prefs.isParentalControlEnabled() }

    suspend fun updateParentalControl(enabled: Boolean, pin: String?) = withContext(Dispatchers.IO) {
        prefs.setParentalControlEnabled(enabled)
        pin?.let { prefs.setParentalPin(it) }
    }

    suspend fun getParentalPin(): String? = withContext(Dispatchers.IO) { prefs.getParentalPin() }

    suspend fun getBlockedCategories(type: String): Set<String> = withContext(Dispatchers.IO) { prefs.getBlockedCategories(type) }

    suspend fun updateBlockedCategories(type: String, ids: Set<String>) = withContext(Dispatchers.IO) {
        prefs.setBlockedCategories(type, ids)
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

enum class ContentLoadStage {
    CONNECTING,
    DOWNLOADING_LIVE,
    PROCESSING_LIVE,
    DOWNLOADING_VOD,
    PROCESSING_VOD,
    DOWNLOADING_SERIES,
    PROCESSING_SERIES,
    LOADING_CATEGORIES
}
