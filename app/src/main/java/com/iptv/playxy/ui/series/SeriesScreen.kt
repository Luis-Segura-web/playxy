package com.iptv.playxy.ui.series

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iptv.playxy.domain.Series
import com.iptv.playxy.ui.components.CategoryBar
import com.iptv.playxy.ui.main.SortOrder
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState

@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel = hiltViewModel(),
    searchQuery: String = "",
    sortOrder: SortOrder = SortOrder.DEFAULT,
    onSeriesClick: (Series) -> Unit
)
{
    LaunchedEffect(searchQuery, sortOrder) { viewModel.updateFilters(searchQuery, sortOrder) }
    val uiState by viewModel.uiState.collectAsState()
    val pagingFlow by viewModel.pagingFlow.collectAsState()
    val seriesPaging = pagingFlow.collectAsLazyPagingItems()
    val isSpecialCategory = uiState.selectedCategory.categoryId in setOf("favorites", "recents")

    Column(modifier = Modifier.fillMaxSize()) {
        CategoryBar(
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategory.categoryId,
            onCategorySelected = { viewModel.selectCategory(it) },
            modifier = Modifier.fillMaxWidth()
        )

        val showLoading = seriesPaging.loadState.refresh is LoadState.Loading && seriesPaging.itemCount == 0 && !isSpecialCategory
        if (showLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            SeriesGrid(
                series = seriesPaging,
                onSeriesClick = { series ->
                    viewModel.onSeriesOpened(series.seriesId)
                    onSeriesClick(series)
                },
                modifier = Modifier.fillMaxSize(),
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun SeriesGrid(
    series: androidx.paging.compose.LazyPagingItems<Series>,
    onSeriesClick: (Series) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSpecialCategory = uiState.selectedCategory.categoryId in setOf("favorites", "recents")
    val isLoading = series.loadState.refresh is LoadState.Loading
    val isEmpty = series.itemCount == 0
    
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
            items(series.itemCount) { index ->
                val seriesItem = series[index] ?: return@items
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
        modifier = modifier.fillMaxWidth()
    ) {
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

            if (series.rating5Based > 0) {
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
                                String.format(l, "%.1f", series.rating5Based)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) Color(0xFFFF4444) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = series.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            lineHeight = 18.sp
        )
    }
}
