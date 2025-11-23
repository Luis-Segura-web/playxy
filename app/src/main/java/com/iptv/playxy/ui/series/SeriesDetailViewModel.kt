package com.iptv.playxy.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.db.EpisodeProgressDao
import com.iptv.playxy.data.db.EpisodeProgressEntity
import com.iptv.playxy.data.db.SeriesProgressDao
import com.iptv.playxy.data.db.SeriesProgressEntity
import com.iptv.playxy.data.repository.IptvRepository
import com.iptv.playxy.domain.Episode
import com.iptv.playxy.domain.Series
import com.iptv.playxy.domain.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesDetailUiState(
    val series: Series? = null,
    val seasons: Map<Int, List<Episode>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastEpisode: Episode? = null, // Último episodio visto
    val episodeProgress: Map<String, EpisodeProgressEntity> = emptyMap(), // Progreso de cada episodio
    val currentPlayingEpisodeId: String? = null // ID del episodio actual en reproducción
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val seriesProgressDao: SeriesProgressDao,
    private val episodeProgressDao: EpisodeProgressDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _userProfile.value = repository.getProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadSeriesInfo(seriesId: String, categoryId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Get series info from repository
                val seriesInfo = repository.getSeriesInfo(seriesId)

                if (seriesInfo != null) {
                    // Convert episodesBySeason map from String keys to Int keys
                    val seasonMap = seriesInfo.episodesBySeason.mapKeys { (key, _) ->
                        key.toIntOrNull() ?: 0
                    }

                    // Cargar progreso guardado (último episodio visto)
                    val progress = seriesProgressDao.getProgress(seriesId)
                    val lastEpisode = if (progress != null) {
                        // Buscar el episodio en la lista
                        seasonMap.values.flatten().find { it.id == progress.lastEpisodeId }
                    } else null

                    // Cargar progreso de todos los episodios
                    val episodeProgressList = episodeProgressDao.getAllProgressForSeries(seriesId)
                    val episodeProgressMap = episodeProgressList.associateBy { it.episodeId }

                    _uiState.value = _uiState.value.copy(
                        series = seriesInfo.info,
                        seasons = seasonMap,
                        lastEpisode = lastEpisode,
                        currentPlayingEpisodeId = lastEpisode?.id,
                        episodeProgress = episodeProgressMap,
                        isLoading = false
                    )
                } else {
                    // Fallback to just series without episodes
                    val seriesList = repository.getSeriesByCategory(categoryId)
                    val series = seriesList.find { it.seriesId == seriesId }

                    _uiState.value = _uiState.value.copy(
                        series = series,
                        seasons = emptyMap(),
                        lastEpisode = null,
                        episodeProgress = emptyMap(),
                        isLoading = false,
                        error = if (series == null) "Serie no encontrada" else "No se pudieron cargar las temporadas"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al cargar información: ${e.message}"
                )
            }
        }
    }

    // Guardar progreso (último episodio visto)
    fun saveProgress(seriesId: String, episode: Episode, positionMs: Long = 0L) {
        viewModelScope.launch {
            try {
                seriesProgressDao.saveProgress(
                    SeriesProgressEntity(
                        seriesId = seriesId,
                        lastEpisodeId = episode.id,
                        lastSeasonNumber = episode.season,
                        lastEpisodeNumber = episode.episodeNum,
                        positionMs = positionMs,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Obtener progreso guardado
    suspend fun getProgress(seriesId: String): SeriesProgressEntity? {
        return try {
            seriesProgressDao.getProgress(seriesId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Guardar progreso de un episodio específico
    fun saveEpisodeProgress(
        episodeId: String,
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        positionMs: Long,
        durationMs: Long
    ) {
        viewModelScope.launch {
            try {
                episodeProgressDao.saveProgress(
                    EpisodeProgressEntity(
                        episodeId = episodeId,
                        seriesId = seriesId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        timestamp = System.currentTimeMillis()
                    )
                )
                
                // Actualizar el mapa en el estado
                val progressList = episodeProgressDao.getAllProgressForSeries(seriesId)
                _uiState.value = _uiState.value.copy(
                    episodeProgress = progressList.associateBy { it.episodeId }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Establecer el episodio que se está reproduciendo actualmente
    fun setCurrentPlayingEpisode(episodeId: String?) {
        _uiState.value = _uiState.value.copy(currentPlayingEpisodeId = episodeId)
    }
}

