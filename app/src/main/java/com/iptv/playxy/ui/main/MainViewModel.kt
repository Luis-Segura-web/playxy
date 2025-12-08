package com.iptv.playxy.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.ui.MainDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SortOrder {
    DEFAULT,
    A_TO_Z,
    Z_TO_A,
    DATE_NEWEST,
    DATE_OLDEST
}

data class MainState(
    val currentDestination: MainDestination = MainDestination.HOME,
    val liveStreamCount: Int = 0,
    val vodStreamCount: Int = 0,
    val seriesCount: Int = 0,
    val isLoggingOut: Boolean = false,
    val searchQuery: String = "",
    val debouncedSearchQuery: String = "",
    val isSearching: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val lastLiveUpdateTime: Long = 0L,
    val lastVodUpdateTime: Long = 0L,
    val lastSeriesUpdateTime: Long = 0L,
    val isReloading: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()
    
    private var searchJob: Job? = null
    
    init {
        viewModelScope.launch {
            loadStats()
        }
        observePrefEvents()
    }
    
    private suspend fun loadStats() {
        val parentalEnabled = repository.isParentalControlEnabled()
        val blockedLive = if (parentalEnabled) repository.getBlockedCategories("live") else emptySet()
        val blockedVod = if (parentalEnabled) repository.getBlockedCategories("vod") else emptySet()
        val blockedSeries = if (parentalEnabled) repository.getBlockedCategories("series") else emptySet()
        val liveStreams = repository.getLiveStreams()
            .filterNot { parentalEnabled && (blockedLive.contains(it.categoryId) || it.isAdult) }
        val vodStreams = repository.getVodStreams()
            .filterNot { parentalEnabled && (blockedVod.contains(it.categoryId) || it.isAdult) }
        val series = repository.getSeries()
            .filterNot { parentalEnabled && blockedSeries.contains(it.categoryId) }
        val lastLiveUpdate = repository.getLastLiveUpdateTime()
        val lastVodUpdate = repository.getLastVodUpdateTime()
        val lastSeriesUpdate = repository.getLastSeriesUpdateTime()
        
        _state.value = _state.value.copy(
            liveStreamCount = liveStreams.size,
            vodStreamCount = vodStreams.size,
            seriesCount = series.size,
            lastLiveUpdateTime = lastLiveUpdate,
            lastVodUpdateTime = lastVodUpdate,
            lastSeriesUpdateTime = lastSeriesUpdate
        )
    }
    
    fun onDestinationChange(destination: MainDestination) {
        _state.value = _state.value.copy(currentDestination = destination)
        viewModelScope.launch {
            loadStats()
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Start new debounced search
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(500) // Wait 500ms after user stops typing
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(debouncedSearchQuery = query)
            }
        }
    }
    
    fun onSearchActiveChange(isActive: Boolean) {
        if (!isActive) {
            // Cancel any pending search job
            searchJob?.cancel()
        }
        _state.value = _state.value.copy(
            isSearching = isActive,
            searchQuery = if (!isActive) "" else _state.value.searchQuery,
            debouncedSearchQuery = if (!isActive) "" else _state.value.debouncedSearchQuery
        )
    }
    
    fun onSortOrderChange(order: SortOrder) {
        _state.value = _state.value.copy(sortOrder = order)
    }

    suspend fun fetchHomeHighlights(): HomeHighlights {
        val allVod = repository.getVodStreams()
        val allSeries = repository.getSeries()
        
        // Preferir contenido con TMDB, pero si no hay, usar todo el catálogo
        val vodWithTmdb = allVod.filter { !it.tmdbId.isNullOrBlank() }
        val seriesWithTmdb = allSeries.filter { !it.tmdbId.isNullOrBlank() }
        
        val vod = if (vodWithTmdb.isNotEmpty()) vodWithTmdb else allVod
        val series = if (seriesWithTmdb.isNotEmpty()) seriesWithTmdb else allSeries
        
        val movieLinks = vod.take(30).map {
            HomeLink(
                title = it.name,
                poster = it.streamIcon,
                year = null,
                availableStreamId = it.streamId,
                availableCategoryId = it.categoryId
            )
        }
        val seriesLinks = series.take(30).map {
            HomeLink(
                title = it.name,
                poster = it.cover,
                year = it.releaseDate?.take(4),
                availableSeriesId = it.seriesId,
                availableCategoryId = it.categoryId
            )
        }
        return HomeHighlights(movies = movieLinks, series = seriesLinks)
    }
    
    fun onReload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isReloading = true)
            val destination = _state.value.currentDestination
            val result = withContext(Dispatchers.IO) {
                when (destination) {
                    MainDestination.TV -> repository.refreshLiveStreams()
                    MainDestination.MOVIES -> repository.refreshVodStreams()
                    MainDestination.SERIES -> repository.refreshSeries()
                    else -> Result.success(Unit)
                }
            }
            if (result.isSuccess) {
                loadStats()
            }
            _state.value = _state.value.copy(isReloading = false)
        }
    }
    
    fun onLogout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoggingOut = true)
            repository.deleteProfile()
            repository.clearCache()
        }
    }
    
    fun onForceReload() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }

    private fun observePrefEvents() {
        viewModelScope.launch {
            repository.prefEvents().collect { event ->
                when {
                    event.startsWith("parental") || event.startsWith("blocked_") -> loadStats()
                }
            }
        }
    }
}
