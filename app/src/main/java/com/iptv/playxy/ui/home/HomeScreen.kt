package com.iptv.playxy.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
@Suppress("DEPRECATION")
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
        when {
            state.isLoading -> {
                ModernLoadingState()
            }
            state.error != null -> {
                ModernErrorState(
                    error = state.error ?: "Error desconocido",
                    onRetry = { viewModel.loadHomeContent() }
                )
            }
            else -> {
                ModernHomeContent(
                    state = state,
                    onNavigateToMovie = onNavigateToMovie,
                    onNavigateToSeries = onNavigateToSeries
                )
            }
        }
    }
}

@Composable
private fun ModernLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(56.dp),
                strokeWidth = 5.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Cargando contenido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Preparando lo mejor para ti...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Algo salió mal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Reintentar",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ModernHomeContent(
    state: HomeState,
    onNavigateToMovie: (String, String) -> Unit,
    onNavigateToSeries: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Section moderno
        if (state.featuredContent.isNotEmpty()) {
            ModernHeroSection(
                items = state.featuredContent,
                onMovieClick = onNavigateToMovie,
                onSeriesClick = onNavigateToSeries
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        // Content Rows minimalistas
        Column(
            modifier = Modifier.padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (state.trendingMovies.isNotEmpty()) {
                ModernContentRow(
                    title = "Películas Populares",
                    icon = Icons.Outlined.LocalFireDepartment,
                    items = state.trendingMovies,
                    onItemClick = { item ->
                        onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                    }
                )
            }
            
            if (state.trendingSeries.isNotEmpty()) {
                ModernContentRow(
                    title = "Series Destacadas",
                    icon = Icons.Outlined.AutoAwesome,
                    items = state.trendingSeries,
                    onItemClick = { item ->
                        onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                    }
                )
            }
            
            if (state.recentMovies.isNotEmpty()) {
                ModernContentRow(
                    title = "Agregadas Recientemente",
                    icon = Icons.Outlined.NewReleases,
                    items = state.recentMovies,
                    onItemClick = { item ->
                        onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                    }
                )
            }
            
            if (state.recentSeries.isNotEmpty()) {
                ModernContentRow(
                    title = "Series Nuevas",
                    icon = Icons.Outlined.FiberNew,
                    items = state.recentSeries,
                    onItemClick = { item ->
                        onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                    }
                )
            }
            
            if (state.highRatedMovies.isNotEmpty()) {
                ModernContentRow(
                    title = "Mejor Valoradas",
                    icon = Icons.Outlined.Star,
                    items = state.highRatedMovies,
                    onItemClick = { item ->
                        onNavigateToMovie(item.streamId ?: "", item.categoryId ?: "")
                    }
                )
            }
            
            if (state.highRatedSeries.isNotEmpty()) {
                ModernContentRow(
                    title = "Series Aclamadas",
                    icon = Icons.Outlined.EmojiEvents,
                    items = state.highRatedSeries,
                    onItemClick = { item ->
                        onNavigateToSeries(item.seriesId ?: "", item.categoryId ?: "")
                    }
                )
            }
        }
    }
}
@Composable
private fun ModernHeroSection(
    items: List<HomeContentItem>,
    onMovieClick: (String, String) -> Unit,
    onSeriesClick: (String, String) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    
    // Auto-scroll effect
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            while (true) {
                delay(6000)
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }
    
    if (items.isEmpty()) return
    
    val item = items[currentIndex]
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .clickable {
                if (item.streamId != null && item.categoryId != null) {
                    onMovieClick(item.streamId, item.categoryId)
                } else if (item.seriesId != null && item.categoryId != null) {
                    onSeriesClick(item.seriesId, item.categoryId)
                }
            }
    ) {
        // Background Image con efecto blur sutil
        AsyncImage(
            model = item.backdrop ?: item.poster,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(1.5.dp),
            contentScale = ContentScale.Crop
        )
        
        // Gradient Overlay moderno
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = 1400f
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badge tipo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ) {
                Text(
                    text = if (item.streamId != null) "PELÍCULA" else "SERIE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    letterSpacing = 1.2.sp
                )
            }
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 40.sp
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.year?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                item.rating?.let {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", it),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )
            }
        }
        
        // Page Indicators minimalistas
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.indices.forEach { index ->
                    val isSelected = index == currentIndex
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.4f,
                        animationSpec = tween(300)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = alpha))
                            .clickable { currentIndex = index }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernContentRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<HomeContentItem>,
    onItemClick: (HomeContentItem) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(items) { item ->
                ModernContentCard(
                    item = item,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun ModernContentCard(
    item: HomeContentItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = tween(100)
    )
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column {
            // Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Rating overlay mejorado
                item.rating?.let { rating ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", rating),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Gradient en la parte inferior
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
            
            // Info section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    
                    item.year?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(150)
            isPressed = false
        }
    }
}
