package com.iptv.playxy.ui.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.data.repository.ContentLoadStage
import com.iptv.playxy.data.repository.ContentLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val errorMessage: String? = null,
    val isCancelled: Boolean = false,
    val currentRetry: Int = 0,
    val maxRetries: Int = 3,
    val canRetry: Boolean = false  // True when there's a failure that can be retried
)

@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoadingState())
    val state: StateFlow<LoadingState> = _state.asStateFlow()
    
    private var loadingJob: Job? = null
    private var contentLoadState: ContentLoadState? = null
    
    companion object {
        private const val MAX_RETRIES = 3
    }
    
    init {
        loadContent()
    }
    
    private fun loadContent() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
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
                        errorMessage = "Selecciona o crea un perfil para continuar",
                        canRetry = false
                    )
                    return@launch
                }
                
                // Load content with incremental retry logic
                var currentRetry = 0
                
                while (currentRetry < MAX_RETRIES && !_state.value.isCancelled) {
                    _state.value = _state.value.copy(
                        currentRetry = currentRetry + 1,
                        maxRetries = MAX_RETRIES,
                        hasError = false,
                        errorMessage = null,
                        canRetry = false
                    )
                    
                    if (currentRetry > 0) {
                        val pendingItems = getPendingItemsDescription()
                        _state.value = _state.value.copy(
                            statusMessage = "Reintentando $pendingItems... (${currentRetry + 1}/$MAX_RETRIES)"
                        )
                        delay(2000) // Espera antes de reintentar
                    }
                    
                    // Use incremental loading - only retry what failed
                    val loadResult = repository.loadAllContent(
                        profile.username,
                        profile.password,
                        onStep = { stage ->
                            if (!_state.value.isCancelled) {
                                val (progress, message) = mapStage(stage)
                                val adjustedProgress = adjustProgressForRetry(progress)
                                _state.value = _state.value.copy(
                                    progress = adjustedProgress, 
                                    statusMessage = if (currentRetry > 0) "$message (intento ${currentRetry + 1})" else message
                                )
                            }
                        },
                        previousState = contentLoadState
                    )
                    
                    // Store the state for potential retry
                    contentLoadState = loadResult.state
                    
                    if (_state.value.isCancelled) {
                        return@launch
                    }
                    
                    if (loadResult.result.isSuccess) {
                        _state.value = _state.value.copy(
                            progress = 1f,
                            statusMessage = "Completado",
                            isComplete = true
                        )
                        return@launch
                    }
                    
                    // Check if we have partial success - don't count as full retry if we made progress
                    val madeProgress = contentLoadState?.getCompletedCount() ?: 0 > 0
                    if (!madeProgress) {
                        currentRetry++
                    } else {
                        // Only increment retry if we had errors on remaining items
                        if (contentLoadState?.hasAnyError() == true) {
                            currentRetry++
                        }
                    }
                }
                
                // If we get here, we have some failures
                if (!_state.value.isCancelled) {
                    val state = contentLoadState
                    if (state != null && state.getCompletedCount() > 0) {
                        // Partial success - allow continuing with what we have
                        _state.value = _state.value.copy(
                            hasError = true,
                            errorMessage = "Carga parcial: ${state.getErrorSummary()}",
                            canRetry = state.hasPendingWork(),
                            // Allow user to continue if at least something loaded
                            isComplete = state.getCompletedCount() >= 2 // At least 2 of 3 loaded
                        )
                    } else {
                        _state.value = _state.value.copy(
                            hasError = true,
                            errorMessage = "Error después de $MAX_RETRIES intentos: ${state?.getErrorSummary() ?: "Error de conexión"}",
                            canRetry = true
                        )
                    }
                }
            } catch (e: Exception) {
                if (!_state.value.isCancelled) {
                    _state.value = _state.value.copy(
                        hasError = true,
                        errorMessage = "Error inesperado: ${e.message}",
                        canRetry = true
                    )
                }
            }
        }
    }
    
    private fun getPendingItemsDescription(): String {
        val state = contentLoadState ?: return "contenido"
        val pending = mutableListOf<String>()
        if (!state.liveCompleted) pending.add("canales")
        if (!state.vodCompleted) pending.add("películas")
        if (!state.seriesCompleted) pending.add("series")
        return if (pending.isEmpty()) "contenido" else pending.joinToString(", ")
    }
    
    private fun adjustProgressForRetry(stageProgress: Float): Float {
        val state = contentLoadState ?: return stageProgress
        
        // Calculate base progress from completed items
        val completedProgress = when {
            state.liveCompleted && state.vodCompleted -> 0.6f
            state.liveCompleted -> 0.3f
            else -> 0f
        }
        
        // Add current stage progress scaled to remaining portion
        val remainingPortion = 1f - completedProgress
        return completedProgress + (stageProgress * remainingPortion)
    }
    
    fun cancelLoading() {
        _state.value = _state.value.copy(
            isCancelled = true,
            statusMessage = "Cancelado por el usuario"
        )
        loadingJob?.cancel()
    }
    
    fun retry() {
        // Keep the contentLoadState to retry only failed components
        _state.value = LoadingState(
            currentRetry = 0,
            maxRetries = MAX_RETRIES
        )
        loadContent()
    }
    
    fun retryFresh() {
        // Full reset - retry everything from scratch
        contentLoadState = null
        _state.value = LoadingState()
        loadContent()
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
