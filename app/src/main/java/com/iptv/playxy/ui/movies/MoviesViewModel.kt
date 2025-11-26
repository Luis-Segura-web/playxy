package com.iptv.playxy.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptv.playxy.data.db.FavoriteVodDao
import com.iptv.playxy.data.db.FavoriteVodEntity
import com.iptv.playxy.data.db.MovieProgressDao
import com.iptv.playxy.data.db.MovieProgressEntity
import com.iptv.playxy.data.db.RecentVodDao
import com.iptv.playxy.data.db.RecentVodEntity
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.domain.VodInfo
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.ui.main.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category = Category("all", "Todas", "0"),
    val isLoading: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val selectedMovieInfo: VodInfo? = null,
    val isLoadingMovieInfo: Boolean = false,
    val movieProgress: MovieProgressEntity? = null,
    val parentalEnabled: Boolean = false,
    val blockedCategories: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DEFAULT
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val favoriteVodDao: FavoriteVodDao,
    private val recentVodDao: RecentVodDao,
    private val movieProgressDao: MovieProgressDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _pagingFlow = MutableStateFlow<kotlinx.coroutines.flow.Flow<PagingData<VodStream>>>(flowOf(PagingData.empty()))
    val pagingFlow: StateFlow<kotlinx.coroutines.flow.Flow<PagingData<VodStream>>> = _pagingFlow.asStateFlow()

    private var currentMovieId: String? = null

    private val favoriteIds = mutableSetOf<String>()
    private val recentIds = ArrayDeque<String>()

    init {
        loadUserProfile()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            loadFavoriteIds()
            loadRecentIds()
            loadCategories()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.getProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadFavoriteIds() {
        val favs = favoriteVodDao.getAllFavorites()
        favoriteIds.clear()
        favoriteIds.addAll(favs.map { it.streamId })
        _uiState.value = _uiState.value.copy(favoriteIds = favoriteIds.toSet())
    }

    private suspend fun loadRecentIds() {
        val recents = recentVodDao.getRecent()
        recentIds.clear()
        recents.forEach { recentIds.addLast(it.streamId) }
        _uiState.value = _uiState.value.copy(recentIds = recentIds.toList())
    }

    private fun normalizeCategories(list: List<Category>, defaultAllName: String): List<Category> {
        val normalized = list.map {
            if (it.categoryName.equals("Todos", ignoreCase = true) || it.categoryName.equals("Todas", ignoreCase = true))
                it.copy(categoryName = defaultAllName)
            else it
        }
        return normalized.distinctBy { it.categoryId to it.categoryName.lowercase() }
    }

    private suspend fun refreshParentalState(): Pair<Boolean, Set<String>> {
        val enabled = repository.isParentalControlEnabled()
        val blocked = if (enabled) repository.getBlockedCategories("vod") else emptySet()
        _uiState.value = _uiState.value.copy(parentalEnabled = enabled, blockedCategories = blocked)
        return enabled to blocked
    }

    private suspend fun buildPagingData() {
        val (parentalEnabled, blockedCategories) = refreshParentalState()
        val category = _uiState.value.selectedCategory
        val search = _uiState.value.searchQuery
        val sortCode = mapSortOrder(_uiState.value.sortOrder)
        val flow = when (category.categoryId) {
            "favorites" -> {
                val favorites = withContext(Dispatchers.Default) {
                    repository.getVodStreams()
                        .filterNot { parentalEnabled && (it.isAdult || blockedCategories.contains(it.categoryId)) }
                        .filter { favoriteIds.contains(it.streamId) }
                        .distinctBy { it.streamId }
                }
                flowOf(PagingData.from(favorites))
            }
            "recents" -> {
                val recents = withContext(Dispatchers.IO) { recentVodDao.getRecent() }
                recentIds.clear()
                recents.forEach { recentIds.addLast(it.streamId) }
                val recentsMovies = withContext(Dispatchers.Default) {
                    val filtered = repository.getVodStreams()
                        .filterNot { parentalEnabled && (it.isAdult || blockedCategories.contains(it.categoryId)) }
                        .associateBy { it.streamId }
                    recents.mapNotNull { filtered[it.streamId] }.distinctBy { it.streamId }
                }
                _uiState.value = _uiState.value.copy(recentIds = recentIds.toList())
                flowOf(PagingData.from(recentsMovies))
            }
            else -> {
                    val categoryId = if (category.categoryId == "all") null else category.categoryId
                    repository.getPagedVodStreams(
                        categoryId = categoryId,
                        searchQuery = search.ifBlank { null },
                        blockAdult = parentalEnabled,
                        blockedCategories = blockedCategories.toList(),
                        sortOrder = sortCode
                    ).cachedIn(viewModelScope)
                }
        }
        _pagingFlow.value = flow
    }

    private fun mapSortOrder(order: SortOrder): Int = when (order) {
        SortOrder.DEFAULT -> 0
        SortOrder.A_TO_Z -> 1
        SortOrder.Z_TO_A -> 2
        SortOrder.DATE_NEWEST -> 3
        SortOrder.DATE_OLDEST -> 4
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val providerCategoriesRaw = repository.getCategories("vod")
                val (parentalEnabled, blocked) = refreshParentalState()
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todas")
                    .filterNot { parentalEnabled && blocked.contains(it.categoryId) }
                val allCategories = buildList {
                    add(Category("all", "Todas", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(providerCategories)
                }
                _uiState.value = _uiState.value.copy(
                    categories = allCategories,
                    selectedCategory = allCategories.first { it.categoryId == "all" }
                )
                refreshPaging()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectCategory(category: Category) {
        if (_uiState.value.selectedCategory.categoryId == category.categoryId) return
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        refreshPaging()
    }

    fun updateFilters(search: String, sortOrder: SortOrder) {
        if (_uiState.value.searchQuery == search && _uiState.value.sortOrder == sortOrder) return
        _uiState.value = _uiState.value.copy(searchQuery = search, sortOrder = sortOrder)
        refreshPaging()
    }

    fun refreshCurrentCategory() {
        refreshPaging()
    }

    private fun refreshPaging() {
        viewModelScope.launch { buildPagingData() }
    }

    private fun registerRecent(streamId: String) {
        viewModelScope.launch {
            recentVodDao.deleteByStream(streamId)
            recentVodDao.insertRecent(
                RecentVodEntity(streamId = streamId, timestamp = System.currentTimeMillis())
            )
            val limit = repository.getRecentsLimit()
            recentVodDao.trim(limit)
            loadRecentIds()
            if (_uiState.value.selectedCategory.categoryId == "recents") {
                refreshPaging()
            }
        }
    }

    fun toggleFavorite(streamId: String) {
        viewModelScope.launch {
            val exists = favoriteVodDao.getFavorite(streamId)
            if (exists != null) favoriteVodDao.deleteFavorite(streamId)
            else favoriteVodDao.insertFavorite(
                FavoriteVodEntity(streamId = streamId, timestamp = System.currentTimeMillis())
            )
            loadFavoriteIds()
            if (_uiState.value.selectedCategory.categoryId == "favorites") {
                refreshPaging()
            }
        }
    }

    // Call when user starts playback explicitly
    fun onMoviePlayed(movie: VodStream) {
        registerRecent(movie.streamId)
    }

    /**
     * Load detailed information for a specific movie
     */
    fun loadMovieInfo(vodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMovieInfo = true)
            try {
                currentMovieId = vodId
                val vodInfo = repository.getVodInfo(vodId)
                val progress = movieProgressDao.getProgress(vodId)
                _uiState.value = _uiState.value.copy(
                    selectedMovieInfo = vodInfo,
                    movieProgress = progress,
                    isLoadingMovieInfo = false
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    selectedMovieInfo = null,
                    movieProgress = null,
                    isLoadingMovieInfo = false
                )
            }
        }
    }

    /**
     * Clear selected movie info
     */
    fun clearMovieInfo() {
        currentMovieId = null
        _uiState.value = _uiState.value.copy(selectedMovieInfo = null, movieProgress = null)
    }

    /**
     * Save movie playback progress
     */
    fun saveMovieProgress(streamId: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            try {
                val entity = MovieProgressEntity(
                    streamId = streamId,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    timestamp = System.currentTimeMillis()
                )
                movieProgressDao.saveProgress(entity)
                if (currentMovieId == streamId) {
                    _uiState.value = _uiState.value.copy(movieProgress = entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Delete movie progress (start from beginning)
     */
    fun deleteMovieProgress(streamId: String) {
        viewModelScope.launch {
            try {
                movieProgressDao.deleteProgress(streamId)
                if (_uiState.value.movieProgress?.streamId == streamId) {
                    _uiState.value = _uiState.value.copy(movieProgress = null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun requiresPinForCategory(categoryId: String): Boolean {
        return repository.isCategoryRestricted("vod", categoryId)
    }

    suspend fun validateParentalPin(pin: String): Boolean {
        return repository.verifyParentalPin(pin)
    }

    suspend fun isParentalEnabled(): Boolean {
        return repository.isParentalControlEnabled()
    }
}

private const val RECENTS_LIMIT = 12
