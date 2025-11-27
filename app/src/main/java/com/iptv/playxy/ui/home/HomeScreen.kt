package com.iptv.playxy.ui.home
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Locale
data class HomeContentItem(
    val title: String,
    val poster: String?,
    val backdrop: String?,
    val year: String?,
    val rating: Double?,
    val description: String?,
    val streamId: String? = null,
    val seriesId: String? = null,
    val categoryId: String? = null
)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToMovie: (String, String) -> Unit,
    onNavigateToSeries: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadHomeContent()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Cargando contenido destacado...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "Error al cargar contenido",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = state.error ?: "Error desconocido",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { viewModel.loadHomeContent() }) {
                        Text("Reintentar")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Section
                if (state.featuredContent.isNotEmpty()) {
                    HeroSection(
                        items = state.featuredContent,
                        onMovieClick = onNavigateToMovie,
                        onSeriesClick = onNavigateToSeries
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                // Content Rows
                Column(
                    modifier = Modifier.padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (state.trendingMovies.isNotEmpty()) {
                        ContentRow(
                            title = "Películas Populares",
                            items = state.trendingMovies,
                            onItemClick = { item ->
                                onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                    if (state.trendingSeries.isNotEmpty()) {
                        ContentRow(
                            title = "Series Destacadas",
                            items = state.trendingSeries,
                            onItemClick = { item ->
                                onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                    if (state.recentMovies.isNotEmpty()) {
                        ContentRow(
                            title = "Películas Recientes",
                            items = state.recentMovies,
                            onItemClick = { item ->
                                onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                    if (state.recentSeries.isNotEmpty()) {
                        ContentRow(
                            title = "Series Recientes",
                            items = state.recentSeries,
                            onItemClick = { item ->
                                onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                    if (state.highRatedMovies.isNotEmpty()) {
                        ContentRow(
                            title = "Películas Mejor Valoradas",
                            items = state.highRatedMovies,
                            onItemClick = { item ->
                                onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                    if (state.highRatedSeries.isNotEmpty()) {
                        ContentRow(
                            title = "Series Mejor Valoradas",
                            items = state.highRatedSeries,
                            onItemClick = { item ->
                                onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun HeroSection(
    items: List<HomeContentItem>,
    onMovieClick: (String, String) -> Unit,
    onSeriesClick: (String, String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    // Auto-scroll effect
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            while (true) {
                delay(5000)
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }
    if (items.isEmpty()) return
    val item = items[currentIndex]
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clickable {
                if (item.streamId != null && item.categoryId != null) {
                    onMovieClick(item.streamId, item.categoryId)
                } else if (item.seriesId != null && item.categoryId != null) {
                    onSeriesClick(item.seriesId, item.categoryId)
                }
            }
    ) {
        // Background Image
        AsyncImage(
            model = item.backdrop ?: item.poster,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(2.dp),
            contentScale = ContentScale.Crop
        )
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = 1200f
                    )
                )
        )
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.year?.let {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                item.rating?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", it),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = if (item.streamId != null) "Película" else "Serie",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Page Indicators
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (index == currentIndex) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { currentIndex = index }
                    )
                }
            }
        }
    }
}
@Composable
private fun ContentRow(
    title: String,
    items: List<HomeContentItem>,
    onItemClick: (HomeContentItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(items) { item ->
                ContentCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}
@Composable
private fun ContentCard(
    item: HomeContentItem,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )
    Card(
        modifier = Modifier
            .width(140.dp)
            .scale(scale)
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    AsyncImage(
                        model = item.poster,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    item.rating?.let { rating ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", rating),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    item.year?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}
