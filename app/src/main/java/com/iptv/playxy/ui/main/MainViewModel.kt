package com.iptv.playxy.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.ui.MainDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainState(
    val currentDestination: MainDestination = MainDestination.HOME,
    val liveStreamCount: Int = 0,
    val vodStreamCount: Int = 0,
    val seriesCount: Int = 0,
    val isLoggingOut: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()
    
    init {
        loadStats()
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            val liveStreams = repository.getLiveStreams()
            val vodStreams = repository.getVodStreams()
            val series = repository.getSeries()
            
            _state.value = _state.value.copy(
                liveStreamCount = liveStreams.size,
                vodStreamCount = vodStreams.size,
                seriesCount = series.size
            )
        }
    }
    
    fun onDestinationChange(destination: MainDestination) {
        _state.value = _state.value.copy(currentDestination = destination)
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
