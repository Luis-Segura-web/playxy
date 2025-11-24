package com.iptv.playxy.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.data.repository.ContentLoadStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoadingState(
    val progress: Float = 0f,
    val statusMessage: String = "Iniciando...",
    val isComplete: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoadingState())
    val state: StateFlow<LoadingState> = _state.asStateFlow()
    
    init {
        loadContent()
    }
    
    private fun loadContent() {
        viewModelScope.launch {
            try {
                // Check if cache is valid
                val hasCache = repository.hasCachedContent()
                val isCacheValid = repository.isCacheValid()
                
                if (hasCache && isCacheValid) {
                    _state.value = _state.value.copy(
                        progress = 1f,
                        statusMessage = "Cargando desde caché...",
                        isComplete = true
                    )
                    return@launch
                }
                
                // Get user profile
                val profile = repository.getProfile()
                if (profile == null) {
                    _state.value = _state.value.copy(
                        hasError = true,
                        errorMessage = "No se encontró perfil de usuario"
                    )
                    return@launch
                }
                
                // Actually load the content with feedback
                val result = repository.loadAllContent(
                    profile.username,
                    profile.password
                ) { stage ->
                    val (progress, message) = mapStage(stage)
                    _state.value = _state.value.copy(progress = progress, statusMessage = message)
                }
                
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        progress = 1f,
                        statusMessage = "Completado",
                        isComplete = true
                    )
                } else {
                    _state.value = _state.value.copy(
                        hasError = true,
                        errorMessage = "Error al cargar contenido: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    hasError = true,
                    errorMessage = "Error inesperado: ${e.message}"
                )
            }
        }
    }
    
    private fun mapStage(stage: ContentLoadStage): Pair<Float, String> {
        return when (stage) {
            ContentLoadStage.CONNECTING -> 0.05f to "Conectando al servidor..."
            ContentLoadStage.DOWNLOADING_LIVE -> 0.15f to "Descargando canales y categorías..."
            ContentLoadStage.PROCESSING_LIVE -> 0.3f to "Procesando canales y categorías..."
            ContentLoadStage.DOWNLOADING_VOD -> 0.45f to "Descargando películas y categorías..."
            ContentLoadStage.PROCESSING_VOD -> 0.6f to "Procesando películas y categorías..."
            ContentLoadStage.DOWNLOADING_SERIES -> 0.75f to "Descargando series y categorías..."
            ContentLoadStage.PROCESSING_SERIES -> 0.88f to "Procesando series y categorías..."
            ContentLoadStage.LOADING_CATEGORIES -> 0.95f to "Sincronizando caché..."
        }
    }
    
    fun onNavigationHandled() {
        _state.value = _state.value.copy(isComplete = false)
    }
}
