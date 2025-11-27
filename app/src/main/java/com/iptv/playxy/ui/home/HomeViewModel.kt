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
                    // Get all VOD streams and Series with TMDB data
                    val allMovies = repository.getVodStreams()
                    val allSeries = repository.getSeries()
                    // Filter by TMDB ID availability for better quality content
                    val moviesWithTmdb = allMovies.filter { !it.tmdbId.isNullOrBlank() }
                    val seriesWithTmdb = allSeries.filter { !it.tmdbId.isNullOrBlank() }
                    // Normalize rating to base 5 for sorting/filters
                    fun movieRating(v: com.iptv.playxy.domain.VodStream): Double {
                        val base5 = if (v.rating5Based > 0f) v.rating5Based else v.rating / 2f
                        return base5.toDouble()
                    }
                    fun seriesRating(s: com.iptv.playxy.domain.Series): Double {
                        val base5 = if (s.rating5Based > 0f) s.rating5Based else s.rating / 2f
                        return base5.toDouble()
                    }
                    val sortedMovies = moviesWithTmdb.sortedByDescending { movieRating(it) }
                    val sortedSeries = seriesWithTmdb.sortedByDescending { seriesRating(it) }
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
                    val featured = (featuredMovies + featuredSeries).shuffled().take(6)
                    // Trending Movies: High rated, shuffled
                    val trending = sortedMovies.take(30).shuffled().take(20).map { movie ->
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
                    val trendingSer = sortedSeries.take(30).shuffled().take(20).map { series ->
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
                    // Recent Movies: Sort by added date
                    val recentMov = moviesWithTmdb
                        .filter { !it.added.isNullOrBlank() }
                        .sortedByDescending { it.added }
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
                    // Recent Series
                    val recentSer = seriesWithTmdb
                        .filter { !it.releaseDate.isNullOrBlank() }
                        .sortedByDescending { it.releaseDate }
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
                    // High Rated Movies
                    val highRatedMov = sortedMovies
                        .filter { it.rating >= 3.5 }
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
                    // High Rated Series
                    val highRatedSer = sortedSeries
                        .filter { it.rating >= 3.5 }
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
