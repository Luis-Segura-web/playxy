package com.iptv.playxy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.data.repository.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val recentsLimit: Int = PreferencesManager.DEFAULT_RECENTS_LIMIT,
    val recentsLimitInput: String = PreferencesManager.DEFAULT_RECENTS_LIMIT.toString(),
    val parentalEnabled: Boolean = false,
    val parentalPin: String = "",
    val tmdbEnabled: Boolean = false,
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
    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events

    init {
        viewModelScope.launch {
            val limit = repository.getRecentsLimit()
            val parentalEnabled = repository.isParentalControlEnabled()
            val pin = repository.getParentalPin().orEmpty()
            val tmdbEnabled = repository.isTmdbEnabled()
            val liveCats = repository.getCategories("live")
            val vodCats = repository.getCategories("vod")
            val seriesCats = repository.getCategories("series")
            val blockedLive = repository.getBlockedCategories("live")
            val blockedVod = repository.getBlockedCategories("vod")
            val blockedSeries = repository.getBlockedCategories("series")
            _uiState.value = _uiState.value.copy(
                recentsLimit = limit,
                recentsLimitInput = limit.toString(),
                parentalEnabled = parentalEnabled,
                parentalPin = pin,
                tmdbEnabled = tmdbEnabled,
                liveCategories = liveCats,
                vodCategories = vodCats,
                seriesCategories = seriesCats,
                blockedLive = blockedLive,
                blockedVod = blockedVod,
                blockedSeries = blockedSeries
            )
        }
    }

    fun onRecentsLimitInputChange(input: String) {
        // Solo dígitos, limitar a 3 caracteres para evitar valores absurdos
        val sanitized = input.filter { it.isDigit() }.take(3)
        _uiState.value = _uiState.value.copy(recentsLimitInput = sanitized)
    }

    fun saveRecentsLimit() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val parsed = _uiState.value.recentsLimitInput.filter { it.isDigit() }.toIntOrNull()
            val limit = parsed?.takeIf { it > 0 } ?: PreferencesManager.DEFAULT_RECENTS_LIMIT
            repository.updateRecentsLimit(limit)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                recentsLimit = limit,
                recentsLimitInput = limit.toString()
            )
            _events.emit(SettingsEvent.ShowMessage("Límite de recientes actualizado a $limit"))
        }
    }

    fun toggleParental(enabled: Boolean) {
        viewModelScope.launch {
            setParentalEnabled(enabled)
        }
    }

    fun toggleTmdb(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTmdbEnabled(enabled)
            _uiState.value = _uiState.value.copy(tmdbEnabled = enabled)
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
        viewModelScope.launch {
            repository.clearRecentChannels()
            _events.emit(SettingsEvent.ShowMessage("Canales recientes limpiados"))
        }
    }

    fun clearRecentMovies() {
        viewModelScope.launch {
            repository.clearRecentVod()
            _events.emit(SettingsEvent.ShowMessage("Películas recientes limpiadas"))
        }
    }

    fun clearRecentSeries() {
        viewModelScope.launch {
            repository.clearRecentSeries()
            _events.emit(SettingsEvent.ShowMessage("Series recientes limpiadas"))
        }
    }

    fun clearAllRecents() {
        viewModelScope.launch {
            repository.clearAllRecents()
            _events.emit(SettingsEvent.ShowMessage("Todos los recientes han sido limpiados"))
        }
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

    suspend fun hasPinConfigured(): Boolean {
        return repository.hasParentalPin()
    }

    suspend fun isPinValid(pin: String): Boolean {
        return repository.verifyParentalPin(pin)
    }

    suspend fun configurePin(pin: String, enableAfterSave: Boolean) {
        _uiState.value = _uiState.value.copy(isSaving = true)
        repository.updateParentalControl(enableAfterSave, pin)
        _uiState.value = _uiState.value.copy(
            parentalPin = pin,
            parentalEnabled = enableAfterSave,
            isSaving = false
        )
    }

    suspend fun changePin(currentPin: String, newPin: String): Boolean {
        val isValid = repository.verifyParentalPin(currentPin)
        if (isValid) {
            repository.updateParentalControl(_uiState.value.parentalEnabled, newPin)
            _uiState.value = _uiState.value.copy(parentalPin = newPin)
        }
        return isValid
    }

    suspend fun setParentalEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(parentalEnabled = enabled, isSaving = true)
        repository.updateParentalControl(enabled, _uiState.value.parentalPin.ifBlank { null })
        _uiState.value = _uiState.value.copy(isSaving = false)
    }

    suspend fun saveBlockedCategories(
        live: Set<String>,
        vod: Set<String>,
        series: Set<String>
    ) {
        repository.updateBlockedCategories("live", live)
        repository.updateBlockedCategories("vod", vod)
        repository.updateBlockedCategories("series", series)
        _uiState.value = _uiState.value.copy(
            blockedLive = live,
            blockedVod = vod,
            blockedSeries = series
        )
    }
}

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}
