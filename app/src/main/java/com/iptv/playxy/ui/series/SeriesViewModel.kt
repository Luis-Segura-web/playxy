package com.iptv.playxy.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iptv.playxy.data.db.FavoriteSeriesDao
import com.iptv.playxy.data.db.FavoriteSeriesEntity
import com.iptv.playxy.data.db.RecentSeriesDao
import com.iptv.playxy.data.db.RecentSeriesEntity
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Category
import com.iptv.playxy.domain.Series
import com.iptv.playxy.ui.main.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SeriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category = Category("all", "Todas", "0"),
    val series: List<Series> = emptyList(),
    val isLoading: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val recentIds: List<String> = emptyList(),
    val parentalEnabled: Boolean = false,
    val blockedCategories: Set<String> = emptySet(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DEFAULT
)

@HiltViewModel
class SeriesViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val favoriteSeriesDao: FavoriteSeriesDao,
    private val recentSeriesDao: RecentSeriesDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    private val _pagingFlow = MutableStateFlow<kotlinx.coroutines.flow.Flow<PagingData<Series>>>(flowOf(PagingData.empty()))
    val pagingFlow: StateFlow<kotlinx.coroutines.flow.Flow<PagingData<Series>>> = _pagingFlow.asStateFlow()

    private val favoriteIds = mutableSetOf<String>()
    private val recentIds = ArrayDeque<String>()

    init {
        viewModelScope.launch {
            loadFavoriteIds()
            loadRecentIds()
            loadCategories()
        }
        observeRecentsCleared()
        observePrefEvents()
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
        val blocked = if (enabled) repository.getBlockedCategories("series") else emptySet()
        _uiState.value = _uiState.value.copy(parentalEnabled = enabled, blockedCategories = blocked)
        return enabled to blocked
    }

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

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val providerCategoriesRaw = repository.getCategories("series")
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
                    selectedCategory = allCategories.first { it.categoryId == "all" },
                    isLoading = false
                )
                refreshPaging()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(isLoading = false)
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
        viewModelScope.launch {
            val (parentalEnabled, blockedCategories) = refreshParentalState()
            val category = _uiState.value.selectedCategory
            val search = _uiState.value.searchQuery.ifBlank { null }
            val sortOrder = mapSortOrder(_uiState.value.sortOrder)
            val flow = when (category.categoryId) {
                "favorites" -> {
                    val favs = withContext(Dispatchers.Default) {
                        repository.getSeries()
                            .filterNot { parentalEnabled && blockedCategories.contains(it.categoryId) }
                            .filter { favoriteIds.contains(it.seriesId) }
                            .distinctBy { it.seriesId }
                    }
                    flowOf(PagingData.from(favs))
                }
                "recents" -> {
                    val recents = withContext(Dispatchers.IO) { recentSeriesDao.getRecent() }
                    recentIds.clear()
                    recents.forEach { recentIds.addLast(it.seriesId) }
                    val recentsSeries = withContext(Dispatchers.Default) {
                        val filtered = repository.getSeries()
                            .filterNot { parentalEnabled && blockedCategories.contains(it.categoryId) }
                            .associateBy { it.seriesId }
                        recents.mapNotNull { filtered[it.seriesId] }.distinctBy { it.seriesId }
                    }
                    _uiState.value = _uiState.value.copy(recentIds = recentIds.toList())
                    flowOf(PagingData.from(recentsSeries))
                }
                else -> {
                    val categoryId = if (category.categoryId == "all") null else category.categoryId
                    repository.getPagedSeries(
                        categoryId = categoryId,
                        searchQuery = search,
                        blockedCategories = blockedCategories.toList(),
                        sortOrder = sortOrder,
                        pageSize = 50
                    ).cachedIn(viewModelScope)
                }
            }
            _pagingFlow.value = flow
        }
    }

    private fun observeRecentsCleared() {
        viewModelScope.launch {
            repository.recentsCleared().collect { type ->
                if (type == "series" || type == "all") {
                    recentIds.clear()
                    _uiState.value = _uiState.value.copy(recentIds = emptyList())
                    if (_uiState.value.selectedCategory.categoryId == "recents") {
                        refreshPaging()
                    }
                }
            }
        }
    }

    private fun observePrefEvents() {
        viewModelScope.launch {
            repository.prefEvents().collect { event ->
                if (event == "parental" || event == "blocked_series") {
                    loadCategories()
                    refreshPaging()
                }
            }
        }
    }

    private fun mapSortOrder(order: SortOrder): Int = when (order) {
        SortOrder.DEFAULT -> 0
        SortOrder.A_TO_Z -> 1
        SortOrder.Z_TO_A -> 2
        else -> 0
    }

    fun toggleFavorite(seriesId: String) {
        viewModelScope.launch {
            val exists = favoriteSeriesDao.getFavorite(seriesId)
            if (exists != null) favoriteSeriesDao.deleteFavorite(seriesId) else favoriteSeriesDao.insertFavorite(
                FavoriteSeriesEntity(seriesId = seriesId, timestamp = System.currentTimeMillis())
            )
            loadFavoriteIds()
            if (_uiState.value.selectedCategory.categoryId == "favorites") refreshPaging()
        }
    }

    fun onSeriesOpened(seriesId: String) {
        viewModelScope.launch {
            recentSeriesDao.deleteBySeries(seriesId)
            recentSeriesDao.insertRecent(
                RecentSeriesEntity(seriesId = seriesId, timestamp = System.currentTimeMillis())
            )
            val limit = repository.getRecentsLimit()
            recentSeriesDao.trim(limit)
            loadRecentIds()
            if (_uiState.value.selectedCategory.categoryId == "recents") refreshPaging()
        }
    }

    suspend fun requiresPinForCategory(categoryId: String): Boolean {
        return repository.isCategoryRestricted("series", categoryId)
    }

    suspend fun validateParentalPin(pin: String): Boolean {
        return repository.verifyParentalPin(pin)
    }
}
