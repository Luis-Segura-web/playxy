package com.iptv.playxy.ui.series

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iptv.playxy.domain.Series
import com.iptv.playxy.ui.components.CategoryBar
import com.iptv.playxy.ui.main.SortOrder
import java.text.Normalizer

private val accentRegex = Regex("\\p{M}")
private val numberRegex = Regex("\\d+")

@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel = hiltViewModel(),
    searchQuery: String = "",
    sortOrder: SortOrder = SortOrder.DEFAULT,
    onSeriesClick: (Series) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategory.categoryId,
            onCategorySelected = { viewModel.selectCategory(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Apply search and sort to series
        val processedSeries by remember(uiState.series, searchQuery, sortOrder) {
            derivedStateOf {
                var series = uiState.series
                
                // Apply search filter (accent-insensitive)
                if (searchQuery.isNotEmpty()) {
                    val normalizedQuery = searchQuery.normalizeString()
                    series = series.filter { 
                        it.name.normalizeString().contains(normalizedQuery, ignoreCase = true)
                    }
                }
                
                // Apply sorting
                when (sortOrder) {
                    SortOrder.A_TO_Z -> series.sortedWith(compareBy { it.name.naturalSortKey() })
                    SortOrder.Z_TO_A -> series.sortedWith(compareByDescending { it.name.naturalSortKey() })
                    SortOrder.DATE_NEWEST -> series.sortedByDescending { it.lastModified?.toLongOrNull() ?: 0L }
                    SortOrder.DATE_OLDEST -> series.sortedBy { it.lastModified?.toLongOrNull() ?: Long.MAX_VALUE }
                    SortOrder.DEFAULT -> series
                }
            }
        }

        // Series Grid
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            SeriesGrid(
                series = processedSeries,
                onSeriesClick = { s ->
                    viewModel.onSeriesOpened(s.seriesId)
                    onSeriesClick(s)
                },
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SeriesGrid(
    series: List<Series>,
    onSeriesClick: (Series) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    if (series.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay series disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier,
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(series) { seriesItem ->
                SeriesPosterItem(
                    series = seriesItem,
                    isFavorite = viewModel.uiState.collectAsState().value.favoriteIds.contains(seriesItem.seriesId),
                    onToggleFavorite = { viewModel.toggleFavorite(seriesItem.seriesId) },
                    onClick = { onSeriesClick(seriesItem) }
                )
            }
        }
    }
}

@Composable
fun SeriesPosterItem(
    series: Series,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Poster Image con aspect ratio 2:3 y overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            // Poster Card
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = series.cover,
                    contentDescription = series.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_report_image
                    ),
                    placeholder = androidx.compose.ui.res.painterResource(
                        android.R.drawable.ic_menu_gallery
                    )
                )
            }

            // Overlay superior con degradado vertical
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Rating y Favorito en la misma fila (sin separación)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rating (esquina superior izquierda)
                if (series.rating5Based > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = java.util.Locale.getDefault().let { l -> String.format(l, "%.1f", series.rating5Based) },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Favorito (esquina superior derecha, sin espacio adicional)
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(28.dp)
                        .offset(x = 4.dp, y = (-4).dp)
                        .semantics { contentDescription = "Favorito" }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Series Title (max 3 lines)
        Text(
            text = series.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Helper function to remove accents from strings for search
private fun String.normalizeString(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace(accentRegex, "")
}

// Natural sort key for sorting series names
private fun String.naturalSortKey(): String {
    return this.replace(numberRegex) { matchResult ->
        matchResult.value.padStart(10, '0')
    }
}
