package com.iptv.playxy.data.repository

import com.iptv.playxy.data.api.ApiServiceFactory
import com.iptv.playxy.data.api.TmdbApiService
import com.iptv.playxy.data.api.TmdbMovieResponse
import com.iptv.playxy.data.api.TmdbSeriesResponse
import com.iptv.playxy.data.api.TmdbCollectionResponse
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.paging.filter
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate

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
    private val recentsEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private val prefEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private val vodInfoCacheMutex = Mutex()
    private val vodInfoCache = mutableMapOf<String, CachedVodInfo>()
    private val seriesInfoCacheMutex = Mutex()
    private val seriesInfoCache = mutableMapOf<String, CachedSeriesInfo>()
    private val actorCacheMutex = Mutex()
    private val actorCache = mutableMapOf<Int, CachedActorDetails>()
    private val tmdbApiService: TmdbApiService by lazy {
        apiServiceFactory.createTmdbService(TMDB_API_KEY)
    }
    
    // Cache expiration time (24 hours)
    private val cacheExpirationTime = 24 * 60 * 60 * 1000L
    private val liveCacheKey = "live_content"
    private val vodCacheKey = "vod_content"
    private val seriesCacheKey = "series_content"
    private val allCacheKey = "all_content"
    private val vodInfoCacheTtlMs = 4 * 60 * 60 * 1000L // 4 hours
    private val seriesInfoCacheTtlMs = 4 * 60 * 60 * 1000L // 4 horas
    private val actorCacheTtlMs = 4 * 60 * 60 * 1000L // 4 horas

    fun recentsCleared(): SharedFlow<String> = recentsEvents.asSharedFlow()
    fun prefEvents(): SharedFlow<String> = prefEvents.asSharedFlow()
    
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

    private fun parseExpirySeconds(expDate: String?): Long? {
        val value = expDate?.takeIf { it.isNotBlank() && it.lowercase() != "null" }?.toLongOrNull()
        return value?.takeIf { it > 0 }
    }

    private fun updateProfileWithAccountInfo(
        profile: UserProfile,
        loginResponse: com.iptv.playxy.data.api.LoginResponse?
    ): UserProfile {
        val userInfo = loginResponse?.userInfo
        val expiry = parseExpirySeconds(userInfo?.expDate)
        val maxCons = userInfo?.maxConnections?.toIntOrNull()
        val status = userInfo?.status
        return profile.copy(expiry = expiry, maxConnections = maxCons, status = status)
    }
    
    suspend fun deleteProfile() {
        userProfileDao.deleteAllProfiles()
    }
    
    suspend fun validateCredentials(username: String, password: String, baseUrl: String): Boolean {
        return try {
            // Create API service with the provided base URL
            val apiService = apiServiceFactory.createService(baseUrl)
            val response = apiService.validateCredentials(username, password)
            response.isSuccessful && response.body()?.userInfo?.status?.equals("active", true) == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun fetchAccountInfo(
        username: String,
        password: String,
        baseUrl: String
    ): com.iptv.playxy.data.api.LoginResponse? {
        return try {
            val apiService = apiServiceFactory.createService(baseUrl)
            val response = apiService.validateCredentials(username, password)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun refreshStoredProfileInfo(): UserProfile? {
        val stored = userProfileDao.getProfile() ?: return null
        val domain = EntityMapper.toDomain(stored)
        val login = fetchAccountInfo(domain.username, domain.password, domain.url)
        val updated = updateProfileWithAccountInfo(domain, login)
        saveProfile(updated)
        return updated
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

    suspend fun clearRecentChannels() = withContext(Dispatchers.IO) {
        recentChannelDao.deleteAll()
        recentsEvents.tryEmit("live")
    }

    suspend fun clearRecentVod() = withContext(Dispatchers.IO) {
        recentVodDao.deleteAll()
        recentsEvents.tryEmit("vod")
    }

    suspend fun clearRecentSeries() = withContext(Dispatchers.IO) {
        recentSeriesDao.deleteAll()
        recentsEvents.tryEmit("series")
    }

    suspend fun clearAllRecents() = withContext(Dispatchers.IO) {
        recentChannelDao.deleteAll()
        recentVodDao.deleteAll()
        recentSeriesDao.deleteAll()
        recentsEvents.tryEmit("all")
    }

    suspend fun isTmdbEnabled(): Boolean = withContext(Dispatchers.IO) { prefs.isTmdbEnabled() }

    suspend fun setTmdbEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.setTmdbEnabled(enabled)
        prefEvents.tryEmit("tmdb")
    }

    suspend fun isParentalControlEnabled(): Boolean = withContext(Dispatchers.IO) { prefs.isParentalControlEnabled() }

    suspend fun updateParentalControl(enabled: Boolean, pin: String?) = withContext(Dispatchers.IO) {
        prefs.setParentalControlEnabled(enabled)
        pin?.let { prefs.setParentalPin(it) }
        prefEvents.tryEmit("parental")
    }

    suspend fun getParentalPin(): String? = withContext(Dispatchers.IO) { prefs.getParentalPin() }

    suspend fun hasParentalPin(): Boolean = withContext(Dispatchers.IO) {
        prefs.getParentalPin().isNullOrBlank().not()
    }

    suspend fun verifyParentalPin(pin: String): Boolean = withContext(Dispatchers.IO) {
        prefs.getParentalPin() == pin
    }

    suspend fun setParentalPin(pin: String) = withContext(Dispatchers.IO) {
        prefs.setParentalPin(pin)
    }

    suspend fun isCategoryRestricted(type: String, categoryId: String): Boolean = withContext(Dispatchers.IO) {
        prefs.isParentalControlEnabled() && prefs.getBlockedCategories(type).contains(categoryId)
    }

    suspend fun getBlockedCategories(type: String): Set<String> = withContext(Dispatchers.IO) { prefs.getBlockedCategories(type) }

    suspend fun updateBlockedCategories(type: String, ids: Set<String>) = withContext(Dispatchers.IO) {
        prefs.setBlockedCategories(type, ids)
        prefEvents.tryEmit("blocked_$type")
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
    
    fun getPagedVodStreams(
        categoryId: String?,
        searchQuery: String?,
        blockAdult: Boolean,
        blockedCategories: List<String>,
        sortOrder: Int,
        pageSize: Int = 40
    ): Flow<PagingData<VodStream>> {
        val allowBlocked = blockedCategories.isEmpty()
        val flow = Pager(
            config = PagingConfig(pageSize = pageSize, initialLoadSize = pageSize, enablePlaceholders = false)
        ) {
            vodStreamDao.pagingVodStreams(
                categoryId = categoryId,
                searchQuery = searchQuery?.ifBlank { null },
                allowBlocked = allowBlocked,
                blockedCategories = blockedCategories,
                blockAdult = blockAdult,
                sortOrder = sortOrder
            )
        }.flow.map { pagingData -> pagingData.map { EntityMapper.vodStreamToDomain(it) } }
        return if (categoryId == null) flow.distinctVodStreams() else flow
    }

    suspend fun getSeries(): List<Series> {
        return seriesDao.getAllSeries().map { EntityMapper.seriesToDomain(it) }
    }
    
    suspend fun getSeriesByCategory(categoryId: String): List<Series> {
        return seriesDao.getSeriesByCategory(categoryId).map { EntityMapper.seriesToDomain(it) }
    }

    fun getPagedLiveStreams(
        categoryId: String?,
        searchQuery: String?,
        blockAdult: Boolean,
        blockedCategories: List<String>,
        sortOrder: Int,
        pageSize: Int = 50
    ): Flow<PagingData<LiveStream>> {
        val allowBlocked = blockedCategories.isEmpty()
        val flow = Pager(
            config = PagingConfig(pageSize = pageSize, initialLoadSize = pageSize, enablePlaceholders = false)
        ) {
            liveStreamDao.pagingLiveStreams(
                categoryId = categoryId,
                searchQuery = searchQuery?.ifBlank { null },
                allowBlocked = allowBlocked,
                blockedCategories = blockedCategories,
                blockAdult = blockAdult,
                sortOrder = sortOrder
            )
        }.flow.map { pagingData -> pagingData.map { EntityMapper.liveStreamToDomain(it) } }
        return if (categoryId == null) flow.distinctLiveStreams() else flow
    }

    fun getPagedSeries(
        categoryId: String?,
        searchQuery: String?,
        blockedCategories: List<String>,
        sortOrder: Int,
        pageSize: Int = 50
    ): Flow<PagingData<Series>> {
        val allowBlocked = blockedCategories.isEmpty()
        val flow = Pager(
            config = PagingConfig(pageSize = pageSize, initialLoadSize = pageSize, enablePlaceholders = false)
        ) {
            seriesDao.pagingSeries(
                categoryId = categoryId,
                searchQuery = searchQuery?.ifBlank { null },
                allowBlocked = allowBlocked,
                blockedCategories = blockedCategories,
                sortOrder = sortOrder
            )
        }.flow.map { pagingData -> pagingData.map { EntityMapper.seriesToDomain(it) } }
        return if (categoryId == null) flow.distinctSeries() else flow
    }

    private fun Flow<PagingData<LiveStream>>.distinctLiveStreams(): Flow<PagingData<LiveStream>> {
        return map { paging ->
            val seen = mutableSetOf<String>()
            paging.filter { seen.add(it.streamId) }
        }
    }

    private fun Flow<PagingData<VodStream>>.distinctVodStreams(): Flow<PagingData<VodStream>> {
        return map { paging ->
            val seen = mutableSetOf<String>()
            paging.filter { seen.add(it.streamId) }
        }
    }

    private fun Flow<PagingData<Series>>.distinctSeries(): Flow<PagingData<Series>> {
        return map { paging ->
            val seen = mutableSetOf<String>()
            paging.filter { seen.add(it.seriesId) }
        }
    }

    private suspend fun getCachedVodInfo(vodId: String, useTmdb: Boolean): VodInfo? {
        val now = System.currentTimeMillis()
        return vodInfoCacheMutex.withLock {
            val iterator = vodInfoCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value.timestamp > vodInfoCacheTtlMs) iterator.remove()
            }
            vodInfoCache[vodId]
                ?.takeIf { now - it.timestamp <= vodInfoCacheTtlMs && it.usesTmdb == useTmdb }
                ?.info
        }
    }

    private suspend fun cacheVodInfo(vodId: String, info: VodInfo, usesTmdb: Boolean) {
        vodInfoCacheMutex.withLock {
            vodInfoCache[vodId] = CachedVodInfo(info, System.currentTimeMillis(), usesTmdb)
        }
    }

    private suspend fun getCachedSeriesInfo(seriesId: String, useTmdb: Boolean): SeriesInfo? {
        val now = System.currentTimeMillis()
        return seriesInfoCacheMutex.withLock {
            val iterator = seriesInfoCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value.timestamp > seriesInfoCacheTtlMs) iterator.remove()
            }
            seriesInfoCache[seriesId]
                ?.takeIf { now - it.timestamp <= seriesInfoCacheTtlMs && it.usesTmdb == useTmdb }
                ?.info
        }
    }

    private suspend fun cacheSeriesInfo(seriesId: String, info: SeriesInfo, usesTmdb: Boolean) {
        seriesInfoCacheMutex.withLock {
            seriesInfoCache[seriesId] = CachedSeriesInfo(info, System.currentTimeMillis(), usesTmdb)
        }
    }

    private suspend fun getCachedActor(actorId: Int): com.iptv.playxy.domain.ActorDetails? {
        val now = System.currentTimeMillis()
        return actorCacheMutex.withLock {
            val iterator = actorCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value.timestamp > actorCacheTtlMs) iterator.remove()
            }
            actorCache[actorId]?.takeIf { now - it.timestamp <= actorCacheTtlMs }?.details
        }
    }

    private suspend fun cacheActor(actorId: Int, details: com.iptv.playxy.domain.ActorDetails) {
        actorCacheMutex.withLock {
            actorCache[actorId] = CachedActorDetails(details, System.currentTimeMillis())
        }
    }

    private suspend fun fetchTmdbMovie(tmdbId: String): TmdbMovieResponse? {
        return try {
            val response = tmdbApiService.getMovie(tmdbId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchTmdbCollection(collectionId: Int): TmdbCollectionResponse? {
        return try {
            val response = tmdbApiService.getCollection(collectionId.toString())
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun fetchTmdbSeries(tmdbId: String): TmdbSeriesResponse? {
        return try {
            val response = tmdbApiService.getSeries(tmdbId)
            if (response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getActorDetails(cast: TmdbCast): com.iptv.playxy.domain.ActorDetails? {
        if (!isTmdbEnabled()) return null
        val castId = cast.id ?: return null
        getCachedActor(castId)?.let { return it }
        return try {
            val response = tmdbApiService.getPerson(castId.toString())
            if (response.isSuccessful) {
                val body = response.body() ?: return null
                val profile = cast.profile ?: tmdbProfileUrl(body.profilePath)
                val credits = body.combinedCredits?.cast.orEmpty()
                val allStreams = vodStreamDao.getAllVodStreams().map { EntityMapper.vodStreamToDomain(it) }
                val streamsByTmdb = allStreams.groupBy { it.tmdbId }
                val allSeries = seriesDao.getAllSeries().map { EntityMapper.seriesToDomain(it) }
                val seriesByTmdb = allSeries.groupBy { it.tmdbId }

                fun mapMovieLinks(includeUnavailable: Boolean): List<com.iptv.playxy.domain.TmdbMovieLink> {
                    val list = mutableListOf<com.iptv.playxy.domain.TmdbMovieLink>()
                    credits.forEach { result ->
                        if (result.mediaType != null && result.mediaType != "movie") return@forEach
                        val id = result.id ?: return@forEach
                        val tmdbTitle = result.title ?: result.name ?: return@forEach
                        val poster = tmdbPosterUrl(result.posterPath)
                        val matches = streamsByTmdb[id.toString()].orEmpty()
                        val releaseDate = result.releaseDate
                        val overview = result.overview
                        val backdrop = tmdbBackdropUrl(result.backdropPath)
                        val rating = result.voteAverage
                        if (matches.isEmpty()) {
                            if (includeUnavailable) {
                                list += com.iptv.playxy.domain.TmdbMovieLink(
                                    tmdbId = id,
                                    title = tmdbTitle,
                                    poster = poster,
                                    availableStreamId = null,
                                    availableCategoryId = null,
                                    tmdbTitle = tmdbTitle,
                                    character = result.character,
                                    releaseDate = releaseDate,
                                    overview = overview,
                                    backdrop = backdrop,
                                    rating = rating
                                )
                            }
                        } else {
                            matches.forEach { stream ->
                                list += com.iptv.playxy.domain.TmdbMovieLink(
                                    tmdbId = id,
                                    title = stream.name,
                                    poster = stream.streamIcon ?: poster,
                                    availableStreamId = stream.streamId,
                                    availableCategoryId = stream.categoryId,
                                    tmdbTitle = tmdbTitle,
                                    character = result.character,
                                    releaseDate = releaseDate,
                                    overview = overview,
                                    backdrop = backdrop,
                                    rating = rating
                                )
                            }
                        }
                    }
                    return list.distinctBy { it.availableStreamId ?: "tmdb-${it.tmdbId}" }
                }

                fun mapSeriesLinks(includeUnavailable: Boolean): List<com.iptv.playxy.domain.TmdbSeriesLink> {
                    val list = mutableListOf<com.iptv.playxy.domain.TmdbSeriesLink>()
                    credits.forEach { result ->
                        val media = result.mediaType
                        if (media != null && media != "tv") return@forEach
                        val id = result.id ?: return@forEach
                        val tmdbTitle = result.name ?: result.title ?: return@forEach
                        val poster = tmdbPosterUrl(result.posterPath)
                        val matches = seriesByTmdb[id.toString()].orEmpty()
                        val airDate = result.firstAirDate
                        val overview = result.overview
                        val backdrop = tmdbBackdropUrl(result.backdropPath)
                        val rating = result.voteAverage
                        if (matches.isEmpty()) {
                            if (includeUnavailable) {
                                list += com.iptv.playxy.domain.TmdbSeriesLink(
                                    tmdbId = id,
                                    title = tmdbTitle,
                                    poster = poster,
                                    availableSeriesId = null,
                                    availableCategoryId = null,
                                    tmdbTitle = tmdbTitle,
                                    character = result.character,
                                    firstAirDate = airDate,
                                    overview = overview,
                                    backdrop = backdrop,
                                    rating = rating
                                )
                            }
                        } else {
                            matches.forEach { series ->
                                list += com.iptv.playxy.domain.TmdbSeriesLink(
                                    tmdbId = id,
                                    title = series.name,
                                    poster = series.cover ?: poster,
                                    availableSeriesId = series.seriesId,
                                    availableCategoryId = series.categoryId,
                                    tmdbTitle = tmdbTitle,
                                    character = result.character,
                                    firstAirDate = airDate,
                                    overview = overview,
                                    backdrop = backdrop,
                                    rating = rating
                                )
                            }
                        }
                    }
                    return list.distinctBy { it.availableSeriesId ?: "tmdb-${it.tmdbId}" }
                }

                val availableMovies = mapMovieLinks(includeUnavailable = false)
                val unavailableMovies = mapMovieLinks(includeUnavailable = true).filter { it.availableStreamId == null }
                val availableSeries = mapSeriesLinks(includeUnavailable = false)
                val unavailableSeries = mapSeriesLinks(includeUnavailable = true).filter { it.availableSeriesId == null }

                com.iptv.playxy.domain.ActorDetails(
                    name = cast.name,
                    profile = profile,
                    biography = body.biography,
                    birthday = body.birthday,
                    placeOfBirth = body.placeOfBirth,
                    availableMovies = availableMovies,
                    unavailableMovies = unavailableMovies,
                    availableSeries = availableSeries,
                    unavailableSeries = unavailableSeries
                ).also { cacheActor(castId, it) }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun tmdbPosterUrl(path: String?): String? = path?.let { "https://image.tmdb.org/t/p/w400$it" }
    private fun tmdbBackdropUrl(path: String?): String? = path?.let { "https://image.tmdb.org/t/p/w780$it" }
    private fun tmdbProfileUrl(path: String?): String? = path?.let { "https://image.tmdb.org/t/p/w185$it" }

    private fun tmdbBackdropList(path: String?, images: List<String> = emptyList()): List<String> {
        val single = path?.let { "https://image.tmdb.org/t/p/w780$it" }
        val fromList = images.map { "https://image.tmdb.org/t/p/w780$it" }
        return buildList {
            if (single != null) add(single)
            addAll(fromList)
        }.distinct().filter { it.isNotBlank() }
    }

    private fun formatRuntimeMinutes(minutes: Int?): String? {
        if (minutes == null || minutes <= 0) return null
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "%d:%02d".format(hours, mins) else "%d min".format(mins)
    }

    private fun selectTrailerKey(videos: List<com.iptv.playxy.data.api.TmdbVideo>?): String? {
        return videos
            ?.filter { it.site.equals("YouTube", true) }
            ?.sortedWith(
                compareByDescending<com.iptv.playxy.data.api.TmdbVideo> { it.official == true }
                    .thenByDescending { it.type.equals("Trailer", true) }
            )
            ?.firstOrNull()?.key
    }

    private suspend fun mergeVodInfoWithTmdb(
        provider: VodInfo,
        tmdb: TmdbMovieResponse,
        collection: TmdbCollectionResponse?
    ): VodInfo {
        val backdropsFromImages = tmdb.images?.backdrops?.mapNotNull { it.filePath }.orEmpty()
        val backdropList = when {
            !provider.backdropPath.isNullOrEmpty() -> provider.backdropPath!!
            !tmdb.backdropPath.isNullOrBlank() -> tmdbBackdropList(tmdb.backdropPath)
            backdropsFromImages.isNotEmpty() -> tmdbBackdropList(null, backdropsFromImages)
            else -> emptyList()
        }
        val poster = provider.movieImage ?: provider.coverBig ?: tmdbPosterUrl(tmdb.posterPath)
        val trailer = provider.youtubeTrailer ?: selectTrailerKey(tmdb.videos?.results)
        val director = provider.director
            ?: tmdb.credits?.crew?.firstOrNull { it.job.equals("Director", true) }?.name
        val cast = provider.cast ?: provider.actors
            ?: tmdb.credits?.cast
                ?.sortedBy { it.order ?: Int.MAX_VALUE }
                ?.take(10)
                ?.joinToString(",") { it.name.orEmpty() }

        val genres = provider.genre ?: tmdb.genres
            ?.mapNotNull { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")

        val rating5Based = provider.rating5Based ?: tmdb.voteAverage?.div(2.0)
        val rating = provider.rating ?: tmdb.voteAverage?.toString()

        val castList = tmdb.credits?.cast
            ?.sortedBy { it.order ?: Int.MAX_VALUE }
            ?.take(15)
            ?.mapNotNull {
                val name = it.name ?: return@mapNotNull null
                com.iptv.playxy.domain.TmdbCast(
                    id = it.id,
                    name = name,
                    character = it.character,
                    profile = tmdbProfileUrl(it.profilePath)
                )
            }
            ?: emptyList()

        val allStreams = vodStreamDao.getAllVodStreams().map { EntityMapper.vodStreamToDomain(it) }
        val streamsByTmdb = allStreams.groupBy { it.tmdbId }

        fun mapMovieLinks(
            results: List<com.iptv.playxy.data.api.TmdbMovieResult>?,
            includeUnavailable: Boolean
        ): List<com.iptv.playxy.domain.TmdbMovieLink> {
            val links = mutableListOf<com.iptv.playxy.domain.TmdbMovieLink>()
            results.orEmpty().forEach { result ->
                val id = result.id ?: return@forEach
                val tmdbTitle = result.title ?: result.name ?: return@forEach
                val poster = tmdbPosterUrl(result.posterPath)
                val matches = streamsByTmdb[id.toString()].orEmpty()
                val releaseDate = result.releaseDate
                val overview = result.overview
                val backdrop = tmdbBackdropUrl(result.backdropPath)
                val rating = result.voteAverage
                if (matches.isEmpty()) {
                    if (includeUnavailable) {
                        links += com.iptv.playxy.domain.TmdbMovieLink(
                            tmdbId = id,
                            title = tmdbTitle,
                            poster = poster,
                            availableStreamId = null,
                            availableCategoryId = null,
                            tmdbTitle = tmdbTitle,
                            character = result.character,
                            releaseDate = releaseDate,
                            overview = overview,
                            backdrop = backdrop,
                            rating = rating
                        )
                    }
                } else {
                    matches.forEach { stream ->
                        links += com.iptv.playxy.domain.TmdbMovieLink(
                            tmdbId = id,
                            title = stream.name,
                            poster = stream.streamIcon ?: poster,
                            availableStreamId = stream.streamId,
                            availableCategoryId = stream.categoryId,
                            tmdbTitle = tmdbTitle,
                            character = result.character,
                            releaseDate = releaseDate,
                            overview = overview,
                            backdrop = backdrop,
                            rating = rating
                        )
                    }
                }
            }
            return links.distinctBy { it.availableStreamId ?: "tmdb-${it.tmdbId}" }
        }

        val collectionLinks = if (collection != null) mapMovieLinks(collection.parts, includeUnavailable = true) else emptyList()
        val similarLinks = mapMovieLinks(tmdb.similar?.results, includeUnavailable = false) +
            mapMovieLinks(tmdb.recommendations?.results, includeUnavailable = false)

        // Variantes locales con mismo TMDB que la película actual
        val localVariants = streamsByTmdb[provider.tmdbId]?.map { stream ->
            com.iptv.playxy.domain.TmdbMovieLink(
                tmdbId = provider.tmdbId?.toIntOrNull() ?: tmdb.id ?: -1,
                title = stream.name,
                poster = stream.streamIcon ?: provider.movieImage ?: provider.coverBig,
                availableStreamId = stream.streamId,
                availableCategoryId = stream.categoryId,
                tmdbTitle = tmdb.title ?: provider.name,
                character = null,
                releaseDate = tmdb.releaseDate ?: provider.releaseDate,
                overview = tmdb.overview ?: provider.plot,
                backdrop = tmdbBackdropUrl(tmdb.backdropPath)
            )
        }.orEmpty()

        fun releaseDateKey(date: String?): Long =
            date?.let {
                runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull()
            } ?: Long.MAX_VALUE

        return provider.copy(
            name = provider.name.ifBlank { tmdb.title.orEmpty() },
            originalName = provider.originalName ?: tmdb.originalTitle ?: tmdb.title,
            coverBig = provider.coverBig ?: tmdbPosterUrl(tmdb.posterPath),
            movieImage = poster,
            releaseDate = provider.releaseDate ?: tmdb.releaseDate,
            duration = provider.duration ?: formatRuntimeMinutes(tmdb.runtime),
            durationSecs = provider.durationSecs ?: tmdb.runtime?.times(60),
            youtubeTrailer = trailer,
            director = director,
            cast = cast,
            description = provider.description ?: tmdb.overview,
            plot = provider.plot ?: tmdb.overview,
            rating = rating,
            rating5Based = rating5Based,
            genre = genres,
            backdropPath = backdropList.ifEmpty { null },
            tmdbCast = castList,
            tmdbCollection = (collectionLinks + localVariants)
                .distinctBy { it.availableStreamId ?: "tmdb-${it.tmdbId}" }
                .sortedWith(compareBy<com.iptv.playxy.domain.TmdbMovieLink> { releaseDateKey(it.releaseDate) }
                    .thenBy { it.tmdbTitle ?: it.title }),
            tmdbSimilar = similarLinks.filter { it.availableStreamId != null }
        )
    }

    private suspend fun mergeSeriesInfoWithTmdb(seriesInfo: SeriesInfo, tmdb: TmdbSeriesResponse): SeriesInfo {
        val provider = seriesInfo.info
        val backdropsFromImages = tmdb.images?.backdrops?.mapNotNull { it.filePath }.orEmpty()
        val backdropList = when {
            provider.backdropPath.isNotEmpty() -> provider.backdropPath
            !tmdb.backdropPath.isNullOrBlank() -> tmdbBackdropList(tmdb.backdropPath)
            backdropsFromImages.isNotEmpty() -> tmdbBackdropList(null, backdropsFromImages)
            else -> emptyList()
        }
        val poster = provider.cover ?: tmdbPosterUrl(tmdb.posterPath)
        val trailer = provider.youtubeTrailer ?: selectTrailerKey(tmdb.videos?.results)
        val director = provider.director
            ?: tmdb.credits?.crew?.firstOrNull { it.job.equals("Director", true) }?.name
        val cast = provider.cast
            ?: tmdb.credits?.cast
                ?.sortedBy { it.order ?: Int.MAX_VALUE }
                ?.take(10)
                ?.joinToString(",") { it.name.orEmpty() }

        val genres = provider.genre ?: tmdb.genres
            ?.mapNotNull { it.name }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")

        val rating5Based = provider.rating5Based.takeIf { it > 0 } ?: tmdb.voteAverage?.div(2.0)?.toFloat()
        val rating = provider.rating.takeIf { it > 0f } ?: tmdb.voteAverage?.toFloat()

        val castList = tmdb.credits?.cast
            ?.sortedBy { it.order ?: Int.MAX_VALUE }
            ?.take(15)
            ?.mapNotNull {
                val name = it.name ?: return@mapNotNull null
                com.iptv.playxy.domain.TmdbCast(
                    id = it.id,
                    name = name,
                    character = it.character,
                    profile = tmdbProfileUrl(it.profilePath)
                )
            }
            ?: emptyList()

        val allSeries = seriesDao.getAllSeries().map { EntityMapper.seriesToDomain(it) }
        val seriesByTmdb = allSeries.groupBy { it.tmdbId }

        fun mapSeriesLinks(
            results: List<com.iptv.playxy.data.api.TmdbSeriesResult>?,
            includeUnavailable: Boolean
        ): List<com.iptv.playxy.domain.TmdbSeriesLink> {
            val links = mutableListOf<com.iptv.playxy.domain.TmdbSeriesLink>()
            results.orEmpty().forEach { result ->
                val id = result.id ?: return@forEach
                val tmdbTitle = result.name ?: return@forEach
                val poster = tmdbPosterUrl(result.posterPath)
                val matches = seriesByTmdb[id.toString()].orEmpty()
                val airDate = result.firstAirDate
                val overview = result.overview
                val backdrop = tmdbBackdropUrl(result.backdropPath)
                val rating = result.voteAverage
                if (matches.isEmpty()) {
                    if (includeUnavailable) {
                        links += com.iptv.playxy.domain.TmdbSeriesLink(
                            tmdbId = id,
                            title = tmdbTitle,
                            poster = poster,
                            availableSeriesId = null,
                            availableCategoryId = null,
                            tmdbTitle = tmdbTitle,
                            character = result.character,
                            firstAirDate = airDate,
                            overview = overview,
                            backdrop = backdrop,
                            rating = rating
                        )
                    }
                } else {
                    matches.forEach { series ->
                        links += com.iptv.playxy.domain.TmdbSeriesLink(
                            tmdbId = id,
                            title = series.name,
                            poster = series.cover ?: poster,
                            availableSeriesId = series.seriesId,
                            availableCategoryId = series.categoryId,
                            tmdbTitle = tmdbTitle,
                            character = result.character,
                            firstAirDate = airDate,
                            overview = overview,
                            backdrop = backdrop,
                            rating = rating
                        )
                    }
                }
            }
            return links.distinctBy { it.availableSeriesId ?: "tmdb-${it.tmdbId}" }
        }

        val similarLinks = mapSeriesLinks(tmdb.similar?.results, includeUnavailable = false) +
            mapSeriesLinks(tmdb.recommendations?.results, includeUnavailable = false)

        val collectionLinks = provider.tmdbId?.let { id ->
            seriesByTmdb[id]?.map { series ->
                com.iptv.playxy.domain.TmdbSeriesLink(
                    tmdbId = id.toIntOrNull() ?: tmdb.id ?: -1,
                    title = series.name,
                    poster = series.cover ?: tmdbPosterUrl(tmdb.posterPath),
                    availableSeriesId = series.seriesId,
                    availableCategoryId = series.categoryId,
                    tmdbTitle = tmdb.name ?: provider.name,
                    firstAirDate = series.releaseDate ?: tmdb.firstAirDate,
                    overview = series.plot ?: tmdb.overview,
                    backdrop = series.backdropPath.firstOrNull() ?: tmdbBackdropUrl(tmdb.backdropPath),
                    rating = tmdb.voteAverage
                )
            }
        }.orEmpty()

        val mergedSeries = provider.copy(
            name = provider.name.ifBlank { tmdb.name.orEmpty() },
            plot = provider.plot ?: tmdb.overview,
            cast = cast ?: provider.cast,
            director = director,
            genre = genres,
            releaseDate = provider.releaseDate ?: tmdb.firstAirDate,
            rating = rating ?: provider.rating,
            rating5Based = rating5Based ?: provider.rating5Based,
            backdropPath = backdropList,
            youtubeTrailer = trailer,
            episodeRunTime = provider.episodeRunTime ?: tmdb.episodeRunTime?.firstOrNull()?.toString(),
            cover = poster,
            tmdbId = provider.tmdbId ?: tmdb.id?.toString(),
            lastModified = provider.lastModified
        )

        return seriesInfo.copy(
            info = mergedSeries,
            tmdbCast = castList,
            tmdbSimilar = similarLinks.filter { it.availableSeriesId != null },
            tmdbCollection = collectionLinks.distinctBy { it.availableSeriesId ?: "tmdb-${it.tmdbId}" }
        )
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
        val useTmdb = isTmdbEnabled()
        getCachedSeriesInfo(seriesId, useTmdb)?.let { return it }
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
                val providerInfo = ResponseMapper.toSeriesInfo(response.body()!!, series)
                val merged = if (useTmdb && !providerInfo.info.tmdbId.isNullOrBlank()) {
                    val tmdbResponse = fetchTmdbSeries(providerInfo.info.tmdbId!!)
                    if (tmdbResponse != null) mergeSeriesInfoWithTmdb(providerInfo, tmdbResponse) else providerInfo
                } else providerInfo
                merged.also { cacheSeriesInfo(seriesId, it, useTmdb) }
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
        val useTmdb = isTmdbEnabled()
        getCachedVodInfo(vodId, useTmdb)?.let { return it }
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
                val providerInfo = ResponseMapper.toVodInfo(response.body()!!)
                val merged = if (useTmdb && providerInfo != null && !providerInfo.tmdbId.isNullOrBlank()) {
                    val tmdbResponse = fetchTmdbMovie(providerInfo.tmdbId!!)
                    val collection = tmdbResponse?.belongsToCollection?.id?.let { fetchTmdbCollection(it) }
                    if (tmdbResponse != null) mergeVodInfoWithTmdb(providerInfo, tmdbResponse, collection) else providerInfo
                } else providerInfo
                merged?.also { cacheVodInfo(vodId, it, useTmdb) }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get trending movies from TMDB
     */
    suspend fun getTrendingMovies(): List<com.iptv.playxy.data.api.TmdbMovieResult> {
        return try {
            val response = tmdbApiService.getTrendingMovies()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get trending series from TMDB
     */
    suspend fun getTrendingSeries(): List<com.iptv.playxy.data.api.TmdbSeriesResult> {
        return try {
            val response = tmdbApiService.getTrendingSeries()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get popular movies from TMDB
     */
    suspend fun getPopularMovies(): List<com.iptv.playxy.data.api.TmdbMovieResult> {
        return try {
            val response = tmdbApiService.getPopularMovies()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get popular series from TMDB
     */
    suspend fun getPopularSeries(): List<com.iptv.playxy.data.api.TmdbSeriesResult> {
        return try {
            val response = tmdbApiService.getPopularSeries()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get top rated movies from TMDB
     */
    suspend fun getTopRatedMovies(): List<com.iptv.playxy.data.api.TmdbMovieResult> {
        return try {
            val response = tmdbApiService.getTopRatedMovies()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get top rated series from TMDB
     */
    suspend fun getTopRatedSeries(): List<com.iptv.playxy.data.api.TmdbSeriesResult> {
        return try {
            val response = tmdbApiService.getTopRatedSeries()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get now playing movies from TMDB
     */
    suspend fun getNowPlayingMovies(): List<com.iptv.playxy.data.api.TmdbMovieResult> {
        return try {
            val response = tmdbApiService.getNowPlayingMovies()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get on the air series from TMDB
     */
    suspend fun getOnTheAirSeries(): List<com.iptv.playxy.data.api.TmdbSeriesResult> {
        return try {
            val response = tmdbApiService.getOnTheAirSeries()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.results ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

private data class CachedVodInfo(
    val info: VodInfo,
    val timestamp: Long,
    val usesTmdb: Boolean
)

private data class CachedSeriesInfo(
    val info: SeriesInfo,
    val timestamp: Long,
    val usesTmdb: Boolean
)

private data class CachedActorDetails(
    val details: com.iptv.playxy.domain.ActorDetails,
    val timestamp: Long
)

private const val TMDB_API_KEY = "0a82c6ff2b4b130f83facf56ae9a89b1"

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
