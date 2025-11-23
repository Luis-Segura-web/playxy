package com.iptv.playxy.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.db.FavoriteSeriesDao
import com.iptv.playxy.data.db.FavoriteSeriesEntity
import com.iptv.playxy.data.db.RecentSeriesDao
import com.iptv.playxy.data.db.RecentSeriesEntity
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.Series
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category = Category("all","Todas","0"),
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList()
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val favoriteSeriesDao: FavoriteSeriesDao,
    private val recentSeriesDao: RecentSeriesDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    // In-memory favorites & recents for Series
    private val favoriteIds = mutableSetOf<String>()
    private val recentIds = ArrayDeque<String>()
    private val maxRecents = 30
    private val nameCache = mutableMapOf<String, NameCache>()

    private suspend fun loadFavoriteIds() {
        val favs = favoriteSeriesDao.getAllFavorites()
        favoriteIds.clear()
        favoriteIds.addAll(favs.map { it.seriesId })
        _uiState.value = _uiState.value.copy(favoriteIds = favoriteIds.toSet())
    }

    private suspend fun loadRecentIds() {
        val recents = recentSeriesDao.getRecent()
        recentIds.clear()
        recents.forEach { recentIds.addLast(it.seriesId) }
        _uiState.value = _uiState.value.copy(recentIds = recentIds.toList())
    }

    init {
        loadCategories()
        viewModelScope.launch {
            loadFavoriteIds()
            loadRecentIds()
        }
    }

    private fun normalizeCategories(list: List<Category>, defaultAllName: String): List<Category> {
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
                val providerCategoriesRaw = repository.getCategories("series")
                val providerCategories = normalizeCategories(providerCategoriesRaw, "Todas")
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
                loadAllSeries()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        when (category.categoryId) {
            "all" -> loadAllSeries()
            "favorites" -> loadFavoriteSeries()
            "recents" -> loadRecentSeries()
            else -> loadSeries(category.categoryId)
        }
    }

    private fun loadFavoriteSeries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val all = repository.getSeries()
                val favs = all.filter { favoriteIds.contains(it.seriesId) }
                favs.forEach { cacheNameData(it) }
                _uiState.value = _uiState.value.copy(series = favs, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadRecentSeries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val all = repository.getSeries()
                // Mostrar solo la última serie reproducida (sin duplicados)
                val lastSeriesId = recentIds.firstOrNull()
                val recents = if (lastSeriesId != null) {
                    all.filter { it.seriesId == lastSeriesId }
                } else {
                    emptyList()
                }
                recents.forEach { cacheNameData(it) }
                _uiState.value = _uiState.value.copy(series = recents, isLoading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleFavorite(seriesId: String) {
        viewModelScope.launch {
            val exists = favoriteSeriesDao.getFavorite(seriesId)
            if (exists != null) favoriteSeriesDao.deleteFavorite(seriesId) else favoriteSeriesDao.insertFavorite(
                FavoriteSeriesEntity(seriesId = seriesId, timestamp = System.currentTimeMillis())
            )
            loadFavoriteIds()
            if (_uiState.value.selectedCategory?.categoryId == "favorites") {
                loadFavoriteSeries()
            }
        }
    }

    fun onSeriesOpened(seriesId: String) {
        viewModelScope.launch {
            recentSeriesDao.insertRecent(
                RecentSeriesEntity(seriesId = seriesId, timestamp = System.currentTimeMillis())
            )
            loadRecentIds()
            if (_uiState.value.selectedCategory?.categoryId == "recents") loadRecentSeries()
        }
    }

    private fun loadSeries(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val series = repository.getSeriesByCategory(categoryId)
                _uiState.value = _uiState.value.copy(
                    series = series,
                    isLoading = false
                )
                series.forEach { cacheNameData(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadAllSeries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val series = repository.getSeries().distinctBy { it.seriesId }
                _uiState.value = _uiState.value.copy(
                    series = series,
                    isLoading = false
                )
                series.forEach { cacheNameData(it) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun getNormalizedName(series: Series): String {
        return nameCache[series.seriesId]?.normalizedName ?: cacheNameData(series).normalizedName
    }

    fun getNaturalSortKey(series: Series): String {
        return nameCache[series.seriesId]?.sortKey ?: cacheNameData(series).sortKey
    }

    private fun cacheNameData(series: Series): NameCache {
        val normalized = normalizeString(series.name)
        val sortKey = naturalSortKey(series.name)
        return NameCache(normalizedName = normalized, sortKey = sortKey).also {
            nameCache[series.seriesId] = it
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

private data class NameCache(
    val normalizedName: String,
    val sortKey: String
)
