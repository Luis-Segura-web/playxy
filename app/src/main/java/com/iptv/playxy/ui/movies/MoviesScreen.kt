package com.iptv.playxy.ui.movies

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.ui.components.CategoryBar
import com.iptv.playxy.ui.main.SortOrder
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = hiltViewModel(),
    searchQuery: String = "",
    sortOrder: SortOrder = SortOrder.DEFAULT,
    onMovieClick: (VodStream) -> Unit
) {
    LaunchedEffect(searchQuery, sortOrder) { viewModel.updateFilters(searchQuery, sortOrder) }
    val uiState by viewModel.uiState.collectAsState()
    val pagingFlow by viewModel.pagingFlow.collectAsState()
    val moviesPagingItems = pagingFlow.collectAsLazyPagingItems()
    val isSpecialCategory = uiState.selectedCategory.categoryId in setOf("favorites", "recents")

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategory.categoryId,
            onCategorySelected = { viewModel.selectCategory(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Movies Grid
        val showLoading = moviesPagingItems.loadState.refresh is LoadState.Loading && moviesPagingItems.itemCount == 0 && !isSpecialCategory
        if (showLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            MoviesGrid(
                movies = moviesPagingItems,
                onMovieClick = onMovieClick,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun MoviesGrid(
    movies: androidx.paging.compose.LazyPagingItems<VodStream>,
    onMovieClick: (VodStream) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSpecialCategory = uiState.selectedCategory.categoryId in setOf("favorites", "recents")
    val isLoading = movies.loadState.refresh is LoadState.Loading
    val isEmpty = movies.itemCount == 0
    
    // Para categorías especiales, mostrar mensaje vacío inmediatamente si no hay items
    // Para otras categorías, esperar a que termine de cargar
    val shouldShowEmpty = if (isSpecialCategory) {
        isEmpty
    } else {
        isEmpty && !isLoading
    }
    
    if (shouldShowEmpty) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay películas disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // Mínimo 3 columnas fijas
            modifier = modifier,
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(movies.itemCount) { index ->
                val movie = movies[index] ?: return@items
                val isFavorite = viewModel.uiState.collectAsState().value.favoriteIds.contains(movie.streamId)
                MoviePosterItem(
                    movie = movie,
                    isFavorite = isFavorite,
                    onToggleFavorite = { viewModel.toggleFavorite(movie.streamId) },
                    onClick = { onMovieClick(movie) }
                )
            }
        }
    }
}

@Composable
fun MoviePosterItem(
    movie: VodStream,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Poster Image mejorado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clickable(onClick = onClick)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Box {
                    AsyncImage(
                        model = movie.streamIcon,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(
                            android.R.drawable.ic_menu_report_image
                        ),
                        placeholder = androidx.compose.ui.res.painterResource(
                            android.R.drawable.ic_menu_gallery
                        )
                    )
                    
                    // Gradient overlay sutil
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.2f)
                                    )
                                )
                            )
                    )
                }
            }

            // Rating badge moderno
            if (movie.rating5Based > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = java.util.Locale.getDefault().let { l ->
                                String.format(l, "%.1f", movie.rating5Based)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }

            // Favorito minimalista
            Surface(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .semantics { contentDescription = "Favorito" },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF4444) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Título mejorado
        AutoFitGridTitle(
            text = movie.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AutoFitGridTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    minFontSize: TextUnit = 10.sp
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val baseFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified } ?: 14.sp
        val fittedSize = remember(text, maxWidthPx) {
            var size = baseFontSize
            while (size > minFontSize) {
                val result = measurer.measure(
                    text = AnnotatedString(text),
                    style = style.copy(fontSize = size),
                    constraints = Constraints(maxWidth = maxWidthPx),
                    maxLines = maxLines
                )
                if (!result.hasVisualOverflow) break
                size = (size.value - 1f).sp
            }
            size
        }
        Text(
            text = text,
            style = style.copy(fontSize = fittedSize),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            maxLines = maxLines,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
