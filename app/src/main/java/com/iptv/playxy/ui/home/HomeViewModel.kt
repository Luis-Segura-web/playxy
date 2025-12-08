package com.iptv.playxy.ui.home
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.playxy.data.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
data class HomeState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val featuredContent: List<HomeContentItem> = emptyList(),
    val trendingMovies: List<HomeContentItem> = emptyList(),
    val trendingSeries: List<HomeContentItem> = emptyList(),
    val recentMovies: List<HomeContentItem> = emptyList(),
    val recentSeries: List<HomeContentItem> = emptyList(),
    val highRatedMovies: List<HomeContentItem> = emptyList(),
    val highRatedSeries: List<HomeContentItem> = emptyList()
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    fun loadHomeContent() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    // Get all VOD streams and Series
                    val allMovies = repository.getVodStreams()
                    val allSeries = repository.getSeries()
                    
                    // Preferir contenido con TMDB, pero incluir todo el catálogo
                    // Algunos servicios no envían tmdb_id en las listas
                    val moviesWithTmdb = allMovies.filter { !it.tmdbId.isNullOrBlank() }
                    val seriesWithTmdb = allSeries.filter { !it.tmdbId.isNullOrBlank() }
                    
                    // Si no hay contenido con TMDB, usar todo el catálogo
                    val moviesForDisplay = if (moviesWithTmdb.isNotEmpty()) moviesWithTmdb else allMovies
                    val seriesForDisplay = if (seriesWithTmdb.isNotEmpty()) seriesWithTmdb else allSeries
                    // Normalize rating to base 5 for sorting/filters
                    fun movieRating(v: com.iptv.playxy.domain.VodStream): Double {
                        val base5 = if (v.rating5Based > 0f) v.rating5Based else v.rating / 2f
                        return base5.toDouble()
                    }
                    fun seriesRating(s: com.iptv.playxy.domain.Series): Double {
                        val base5 = if (s.rating5Based > 0f) s.rating5Based else s.rating / 2f
                        return base5.toDouble()
                    }
                    val sortedMovies = moviesForDisplay.sortedByDescending { movieRating(it) }
                    val sortedSeries = seriesForDisplay.sortedByDescending { seriesRating(it) }
                    // Featured: Mix of top rated movies and series
                    val featuredMovies = sortedMovies.take(5).map { movie ->
                        HomeContentItem(
                            title = movie.name,
                            poster = movie.streamIcon,
                            backdrop = null, // VodStream doesn't have backdrop
                            year = movie.added?.take(4), // Use added date as fallback
                            rating = movieRating(movie),
                            description = null, // VodStream doesn't have plot
                            streamId = movie.streamId,
                            categoryId = movie.categoryId
                        )
                    }
                    val featuredSeries = sortedSeries.take(5).map { series ->
                        HomeContentItem(
                            title = series.name,
                            poster = series.cover,
                            backdrop = series.backdropPath.firstOrNull(),
                            year = series.releaseDate?.take(4),
                            rating = seriesRating(series),
                            description = series.plot,
                            seriesId = series.seriesId,
                            categoryId = series.categoryId
                        )
                    }
                    val featured = (featuredMovies + featuredSeries)
                        .distinctBy { it.streamId ?: it.seriesId }
                        .shuffled()
                        .take(6)
                    // Trending Movies: High rated, shuffled
                    val trending = sortedMovies.distinctBy { it.streamId }.take(30).shuffled().take(20).map { movie ->
                        HomeContentItem(
                            title = movie.name,
                            poster = movie.streamIcon,
                            backdrop = null,
                            year = movie.added?.take(4),
                            rating = movieRating(movie),
                            description = null,
                            streamId = movie.streamId,
                            categoryId = movie.categoryId
                        )
                    }
                    // Trending Series
                    val trendingSer = sortedSeries.distinctBy { it.seriesId }.take(30).shuffled().take(20).map { series ->
                        HomeContentItem(
                            title = series.name,
                            poster = series.cover,
                            backdrop = series.backdropPath.firstOrNull(),
                            year = series.releaseDate?.take(4),
                            rating = seriesRating(series),
                            description = series.plot,
                            seriesId = series.seriesId,
                            categoryId = series.categoryId
                        )
                    }
                    // Recent Movies: Sort by added date, fallback to all movies if none have dates
                    val moviesWithDates = moviesForDisplay.filter { !it.added.isNullOrBlank() }
                    val recentMovSource = if (moviesWithDates.isNotEmpty()) {
                        moviesWithDates.sortedByDescending { it.added }
                    } else {
                        moviesForDisplay // usar todas las películas sin ordenar por fecha
                    }
                    val recentMov = recentMovSource
                        .distinctBy { it.streamId }
                        .take(20)
                        .map { movie ->
                            HomeContentItem(
                                title = movie.name,
                            poster = movie.streamIcon,
                            backdrop = null,
                            year = movie.added?.take(4),
                            rating = movieRating(movie),
                            description = null,
                            streamId = movie.streamId,
                            categoryId = movie.categoryId
                        )
                    }
                    // Recent Series: Sort by releaseDate or lastModified, fallback to all series
                    val seriesWithDates = seriesForDisplay.filter { !it.releaseDate.isNullOrBlank() || !it.lastModified.isNullOrBlank() }
                    val recentSerSource = if (seriesWithDates.isNotEmpty()) {
                        seriesWithDates.sortedByDescending { it.releaseDate ?: it.lastModified }
                    } else {
                        seriesForDisplay // usar todas las series sin ordenar por fecha
                    }
                    val recentSer = recentSerSource
                        .distinctBy { it.seriesId }
                        .take(20)
                        .map { series ->
                            HomeContentItem(
                                title = series.name,
                            poster = series.cover,
                            backdrop = series.backdropPath.firstOrNull(),
                            year = series.releaseDate?.take(4) ?: series.lastModified?.take(4),
                            rating = seriesRating(series),
                            description = series.plot,
                            seriesId = series.seriesId,
                            categoryId = series.categoryId
                        )
                    }
                    // High Rated Movies: fallback to top rated if none >= 3.5
                    val moviesHighRated = sortedMovies.distinctBy { it.streamId }.filter { movieRating(it) >= 3.5 }
                    val highRatedMovSource = if (moviesHighRated.isNotEmpty()) moviesHighRated else sortedMovies.distinctBy { it.streamId }
                    val highRatedMov = highRatedMovSource
                        .take(20)
                        .map { movie ->
                            HomeContentItem(
                                title = movie.name,
                                poster = movie.streamIcon,
                                backdrop = null,
                                year = movie.added?.take(4),
                                rating = movieRating(movie),
                                description = null,
                                streamId = movie.streamId,
                                categoryId = movie.categoryId
                            )
                        }
                    // High Rated Series: fallback to top rated if none >= 3.5
                    val seriesHighRated = sortedSeries.distinctBy { it.seriesId }.filter { seriesRating(it) >= 3.5 }
                    val highRatedSerSource = if (seriesHighRated.isNotEmpty()) seriesHighRated else sortedSeries.distinctBy { it.seriesId }
                    val highRatedSer = highRatedSerSource
                        .take(20)
                        .map { series ->
                            HomeContentItem(
                                title = series.name,
                                poster = series.cover,
                                backdrop = series.backdropPath.firstOrNull(),
                                year = series.releaseDate?.take(4),
                                rating = seriesRating(series),
                                description = series.plot,
                                seriesId = series.seriesId,
                                categoryId = series.categoryId
                            )
                        }
                    HomeState(
                        isLoading = false,
                        featuredContent = featured,
                        trendingMovies = trending,
                        trendingSeries = trendingSer,
                        recentMovies = recentMov,
                        recentSeries = recentSer,
                        highRatedMovies = highRatedMov,
                        highRatedSeries = highRatedSer
                    )
                }
                _state.value = result
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido al cargar el contenido"
                )
            }
        }
    }
}
