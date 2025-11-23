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
    val lastUpdateTime: Long = 0L,
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
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            val liveStreams = repository.getLiveStreams()
            val vodStreams = repository.getVodStreams()
            val series = repository.getSeries()
            val lastUpdate = repository.getLastProviderUpdateTime()
            
            _state.value = _state.value.copy(
                liveStreamCount = liveStreams.size,
                vodStreamCount = vodStreams.size,
                seriesCount = series.size,
                lastUpdateTime = lastUpdate
            )
        }
    }
    
    fun onDestinationChange(destination: MainDestination) {
        _state.value = _state.value.copy(currentDestination = destination)
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
    
    fun onReload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isReloading = true)
            repository.clearCache()
            loadStats()
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
}
