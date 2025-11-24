package com.iptv.playxy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val recentsLimit: Int = 12,
    val parentalEnabled: Boolean = false,
    val parentalPin: String = "",
    val isSaving: Boolean = false,
    val liveCategories: List<com.iptv.playxy.domain.Category> = emptyList(),
    val vodCategories: List<com.iptv.playxy.domain.Category> = emptyList(),
    val seriesCategories: List<com.iptv.playxy.domain.Category> = emptyList(),
    val blockedLive: Set<String> = emptySet(),
    val blockedVod: Set<String> = emptySet(),
    val blockedSeries: Set<String> = emptySet()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val limit = repository.getRecentsLimit()
            val parentalEnabled = repository.isParentalControlEnabled()
            val pin = repository.getParentalPin().orEmpty()
            val liveCats = repository.getCategories("live")
            val vodCats = repository.getCategories("vod")
            val seriesCats = repository.getCategories("series")
            val blockedLive = repository.getBlockedCategories("live")
            val blockedVod = repository.getBlockedCategories("vod")
            val blockedSeries = repository.getBlockedCategories("series")
            _uiState.value = _uiState.value.copy(
                recentsLimit = limit,
                parentalEnabled = parentalEnabled,
                parentalPin = pin,
                liveCategories = liveCats,
                vodCategories = vodCats,
                seriesCategories = seriesCats,
                blockedLive = blockedLive,
                blockedVod = blockedVod,
                blockedSeries = blockedSeries
            )
        }
    }

    fun onRecentsLimitSelected(limit: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, recentsLimit = limit)
            repository.updateRecentsLimit(limit)
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun toggleParental(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(parentalEnabled = enabled, isSaving = true)
            repository.updateParentalControl(enabled, _uiState.value.parentalPin.ifBlank { null })
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun updatePin(pin: String) {
        _uiState.value = _uiState.value.copy(parentalPin = pin)
    }

    fun savePin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            repository.updateParentalControl(_uiState.value.parentalEnabled, _uiState.value.parentalPin.ifBlank { null })
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    fun clearRecentChannels() {
        viewModelScope.launch { repository.clearRecentChannels() }
    }

    fun clearRecentMovies() {
        viewModelScope.launch { repository.clearRecentVod() }
    }

    fun clearRecentSeries() {
        viewModelScope.launch { repository.clearRecentSeries() }
    }

    fun clearAllRecents() {
        viewModelScope.launch { repository.clearAllRecents() }
    }

    fun toggleBlockedCategory(type: String, categoryId: String) {
        viewModelScope.launch {
            val current = when (type) {
                "live" -> _uiState.value.blockedLive
                "vod" -> _uiState.value.blockedVod
                "series" -> _uiState.value.blockedSeries
                else -> emptySet()
            }
            val updated = if (current.contains(categoryId)) current - categoryId else current + categoryId
            when (type) {
                "live" -> _uiState.value = _uiState.value.copy(blockedLive = updated)
                "vod" -> _uiState.value = _uiState.value.copy(blockedVod = updated)
                "series" -> _uiState.value = _uiState.value.copy(blockedSeries = updated)
            }
            repository.updateBlockedCategories(type, updated)
        }
    }
}
