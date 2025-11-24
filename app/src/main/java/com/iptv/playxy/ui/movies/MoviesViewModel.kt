package com.iptv.playxy.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MoviesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category = Category("all","Todas","0"),
    val movies: List<VodStream> = emptyList(),
    val isLoading: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val selectedMovieInfo: VodInfo? = null,
    val isLoadingMovieInfo: Boolean = false,
    val movieProgress: MovieProgressEntity? = null
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

    // In-memory favorites & recents (since DB layer not defined for VOD yet)
    private val favoriteIds = mutableSetOf<String>()
    private val recentIds = ArrayDeque<String>()
    private val maxRecents = RECENTS_LIMIT
    private val nameCache = mutableMapOf<String, NameCache>()

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

    init {
        loadUserProfile()
        loadCategories()
        viewModelScope.launch {
            loadFavoriteIds()
            loadRecentIds()
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

    private fun normalizeCategories(list: List<Category>, defaultAllName: String): List<Category> {
        // Unificar nombres 'Todas'/'Todos' y quitar duplicados por id
        val normalized = list.map {
            if (it.categoryName.equals("Todos", ignoreCase = true) || it.categoryName.equals("Todas", ignoreCase = true))
                it.copy(categoryName = defaultAllName)
            else it
        }
        return normalized.distinctBy { it.categoryId to it.categoryName.lowercase() }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val providerCategoriesRaw = repository.getCategories("vod")
                val blocked = repository.getBlockedCategories("vod")
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todas")
                    .filterNot { blocked.contains(it.categoryId) }
                val allCategories = buildList {
                    add(Category("all", "Todas", "0"))
                    add(Category("favorites", "Favoritos", "0"))
                    add(Category("recents", "Recientes", "0"))
                    addAll(providerCategories)
                }
                _uiState.value = _uiState.value.copy(
                    categories = allCategories,
                    isLoading = false,
                    selectedCategory = allCategories.first { it.categoryId == "all" }
                )
                // Cargar películas iniciales
                loadAllMovies()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        when (category.categoryId) {
            "all" -> loadAllMovies()
            "favorites" -> loadFavoriteMovies()
            "recents" -> loadRecentMovies()
            else -> loadMovies(category.categoryId)
        }
    }

    private fun loadFavoriteMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val blockAdult = repository.isParentalControlEnabled()
                val blockedCategories = repository.getBlockedCategories("vod")
                val favs = withContext(Dispatchers.Default) {
                    repository.getVodStreams()
                        .asSequence()
                        .filterNot { (blockAdult && it.isAdult) || blockedCategories.contains(it.categoryId) }
                        .filter { favoriteIds.contains(it.streamId) }
                        .toList()
                }
                _uiState.value = _uiState.value.copy(movies = favs, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadRecentMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val blockAdult = repository.isParentalControlEnabled()
                val blockedCategories = repository.getBlockedCategories("vod")
                val recents = withContext(Dispatchers.Default) {
                    val filtered = repository.getVodStreams()
                        .filterNot { (blockAdult && it.isAdult) || blockedCategories.contains(it.categoryId) }
                        .associateBy { it.streamId }
                    recentIds.mapNotNull { id -> filtered[id] }
                }
                _uiState.value = _uiState.value.copy(movies = recents, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
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
            if (_uiState.value.selectedCategory?.categoryId == "recents") {
                loadRecentMovies()
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
            if (_uiState.value.selectedCategory?.categoryId == "favorites") {
                loadFavoriteMovies()
            }
        }
    }

    // Call when user starts playback explicitly
    fun onMoviePlayed(movie: VodStream) {
        registerRecent(movie.streamId)
    }

    private fun loadMovies(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val blockAdult = repository.isParentalControlEnabled()
                val blockedCategories = repository.getBlockedCategories("vod")
                val movies = withContext(Dispatchers.Default) {
                    repository.getVodStreamsByCategory(categoryId)
                        .filterNot { (blockAdult && it.isAdult) || blockedCategories.contains(it.categoryId) }
                        .also { list -> list.forEach { cacheNameData(it) } }
                }
                _uiState.value = _uiState.value.copy(movies = movies, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadAllMovies() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val blockAdult = repository.isParentalControlEnabled()
                val blockedCategories = repository.getBlockedCategories("vod")
                val movies = withContext(Dispatchers.Default) {
                    repository.getVodStreams()
                        .distinctBy { it.streamId }
                        .filterNot { (blockAdult && it.isAdult) || blockedCategories.contains(it.categoryId) }
                        .also { list -> list.forEach { cacheNameData(it) } }
                }
                _uiState.value = _uiState.value.copy(movies = movies, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Load detailed information for a specific movie
     */
    fun loadMovieInfo(vodId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMovieInfo = true)
            try {
                val vodInfo = repository.getVodInfo(vodId)
                // Cargar progreso de reproducción
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
        _uiState.value = _uiState.value.copy(selectedMovieInfo = null, movieProgress = null)
    }

    /**
     * Save movie playback progress
     */
    fun saveMovieProgress(streamId: String, positionMs: Long, durationMs: Long) {
        viewModelScope.launch {
            try {
                movieProgressDao.saveProgress(
                    MovieProgressEntity(
                        streamId = streamId,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        timestamp = System.currentTimeMillis()
                    )
                )
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
                // Actualizar UI si el progreso eliminado es de la película actualmente seleccionada
                if (_uiState.value.movieProgress?.streamId == streamId) {
                    _uiState.value = _uiState.value.copy(movieProgress = null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getNormalizedName(movie: VodStream): String {
        return nameCache[movie.streamId]?.normalizedName ?: cacheNameData(movie).normalizedName
    }

    fun getNaturalSortKey(movie: VodStream): String {
        return nameCache[movie.streamId]?.sortKey ?: cacheNameData(movie).sortKey
    }

    private fun cacheNameData(movie: VodStream): NameCache {
        val normalized = normalizeString(movie.name)
        val sortKey = naturalSortKey(movie.name)
        return NameCache(normalizedName = normalized, sortKey = sortKey).also {
            nameCache[movie.streamId] = it
        }
    }

    private fun normalizeString(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
        return normalized.replace(accentRegex, "")
    }

private fun naturalSortKey(input: String): String {
        val builder = StringBuilder()
        var lastIndex = 0
        numberRegex.findAll(input).forEach { match ->
            builder.append(input.substring(lastIndex, match.range.first).lowercase())
            builder.append(match.value.padStart(10, '0'))
            lastIndex = match.range.last + 1
        }
        if (lastIndex < input.length) {
            builder.append(input.substring(lastIndex).lowercase())
        }
        return builder.toString()
    }
}

private val accentRegex = Regex("\\p{M}")
private val numberRegex = Regex("\\d+")
private const val RECENTS_LIMIT = 12

private data class NameCache(
    val normalizedName: String,
    val sortKey: String
)
