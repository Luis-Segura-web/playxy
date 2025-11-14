package com.iptv.playxy.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
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
                
                // Load content from API
                _state.value = _state.value.copy(
                    progress = 0.1f,
                    statusMessage = "Conectando al servidor..."
                )
                
                kotlinx.coroutines.delay(500)
                
                _state.value = _state.value.copy(
                    progress = 0.3f,
                    statusMessage = "Descargando Canales de TV..."
                )
                
                kotlinx.coroutines.delay(500)
                
                _state.value = _state.value.copy(
                    progress = 0.5f,
                    statusMessage = "Descargando Películas..."
                )
                
                kotlinx.coroutines.delay(500)
                
                _state.value = _state.value.copy(
                    progress = 0.7f,
                    statusMessage = "Descargando Series..."
                )
                
                kotlinx.coroutines.delay(500)
                
                _state.value = _state.value.copy(
                    progress = 0.9f,
                    statusMessage = "Procesando categorías..."
                )
                
                // Actually load the content
                val result = repository.loadAllContent(profile.username, profile.password)
                
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
    
    fun onNavigationHandled() {
        _state.value = _state.value.copy(isComplete = false)
    }
}
