package com.iptv.playxy.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.iptv.playxy.domain.TmdbMovieLink
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.delay
import com.iptv.playxy.domain.TmdbSeriesLink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActorDetailScreen(
    actorId: Int,
    fallbackName: String,
    fallbackProfile: String,
    catalogHasTmdb: Boolean = false,
    onBack: () -> Unit,
    onNavigateToMovie: (String, String) -> Unit,
    onNavigateToSeries: (String, String) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
){
    val uiState by viewModel.uiState.collectAsState()
    val actor = uiState.selectedActor
    val isLoading = uiState.isLoadingActor
    var selectedUnavailableMovie by remember { mutableStateOf<TmdbMovieLink?>(null) }
    var selectedUnavailableSeries by remember { mutableStateOf<TmdbSeriesLink?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(actorId, catalogHasTmdb) {
        viewModel.loadActorDetails(actorId, fallbackName, fallbackProfile.takeIf { it.isNotBlank() }, catalogHasTmdb)
    }

    var bioExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filmografía") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearActorDetails()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(padding)
        ) {
            when {
                isLoading || actor == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando ficha…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    AsyncImage(
                                        model = actor.profile,
                                        contentDescription = actor.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = actor.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!actor.birthday.isNullOrBlank()) {
                                        Text(
                                            text = "Nacimiento: ${actor.birthday}" + (actor.placeOfBirth?.let { " · $it" } ?: ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (!actor.biography.isNullOrBlank()) {
                            item {
                                Surface(
                                    tonalElevation = 2.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = actor.biography ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = if (bioExpanded) Int.MAX_VALUE else 6,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (bioExpanded) "Mostrar menos" else "Mostrar más",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { bioExpanded = !bioExpanded }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            TabRow(selectedTabIndex = selectedTab) {
                                Tab(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    text = { Text("Películas") }
                                )
                                Tab(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    text = { Text("Series") }
                                )
                            }
                        }

                        when (selectedTab) {
                            0 -> {
                                // Si hay rastreo TMDB, mostrar secciones separadas
                                // Si no hay rastreo, mostrar todo como filmografía sin buscar disponibilidad
                                if (catalogHasTmdb && actor.availableMovies.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Disponibles en el servicio",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    items(actor.availableMovies) { movie ->
                                        FilmographyCard(
                                            item = movie,
                                            showAvailability = false,
                                            enabled = movie.availableStreamId != null,
                                            onClick = {
                                                if (movie.availableStreamId != null && movie.availableCategoryId != null) {
                                                    viewModel.clearActorDetails()
                                                    onBack()
                                                    onNavigateToMovie(movie.availableStreamId, movie.availableCategoryId)
                                                }
                                            }
                                        )
                                    }
                                }

                                if (actor.unavailableMovies.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = if (catalogHasTmdb) "Fuera del servicio" else "Filmografía",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    items(actor.unavailableMovies) { movie ->
                                        FilmographyCard(
                                            item = movie,
                                            showAvailability = catalogHasTmdb,  // Solo mostrar chip cuando hay rastreo
                                            enabled = false,
                                            onClick = { }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                if (catalogHasTmdb && actor.availableSeries.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Disponibles en el servicio",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    items(actor.availableSeries) { serie ->
                                        SeriesFilmographyCard(
                                            item = serie,
                                            showAvailability = false,
                                            enabled = serie.availableSeriesId != null,
                                            onClick = {
                                                if (serie.availableSeriesId != null && serie.availableCategoryId != null) {
                                                    viewModel.clearActorDetails()
                                                    onBack()
                                                    onNavigateToSeries(serie.availableSeriesId, serie.availableCategoryId)
                                                }
                                            }
                                        )
                                    }
                                }
                                if (actor.unavailableSeries.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = if (catalogHasTmdb) "Fuera del servicio" else "Filmografía",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    items(actor.unavailableSeries) { serie ->
                                        SeriesFilmographyCard(
                                            item = serie,
                                            showAvailability = catalogHasTmdb,  // Solo mostrar chip cuando hay rastreo
                                            enabled = true,
                                            onClick = { selectedUnavailableSeries = serie }
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                }
            }
        }
    }

    selectedUnavailableMovie?.let {
        ActorUnavailableDialog(item = it, onDismiss = { selectedUnavailableMovie = null })
    }
    selectedUnavailableSeries?.let {
        ActorUnavailableSeriesDialog(item = it, onDismiss = { selectedUnavailableSeries = null })
    }
}

@Composable
private fun FilmographyCard(
    item: TmdbMovieLink,
    onClick: () -> Unit,
    showAvailability: Boolean,
    enabled: Boolean
) {
    var overviewExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = item.poster ?: item.backdrop,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val genreText = remember(item.genreIds) { formatGenres(item.genreIds, isTv = false) }
                    genreText?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item.releaseDate?.takeIf { it.isNotBlank() }?.let { date ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = date.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val rating5 = (item.rating ?: 0.0) / 2.0
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            val filled = rating5 >= index + 1
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = String.format("%.1f/5", rating5),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!item.character.isNullOrBlank()) {
                        Text(
                            text = "Personaje: ${item.character}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showAvailability && item.availableStreamId == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "No disponible",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (!item.overview.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (overviewExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (overviewExpanded) "Mostrar menos" else "Mostrar más",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { overviewExpanded = !overviewExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun ActorUnavailableDialog(
    item: TmdbMovieLink,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = item.poster ?: item.backdrop,
                            contentDescription = item.tmdbTitle ?: item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!item.character.isNullOrBlank()) {
                            Text(
                                text = item.character,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!item.releaseDate.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "No disponible · ${item.releaseDate.take(4)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (!item.overview.isNullOrBlank()) {
                            Text(
                                text = item.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesFilmographyCard(
    item: TmdbSeriesLink,
    onClick: () -> Unit,
    showAvailability: Boolean,
    enabled: Boolean
) {
    var overviewExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = item.poster ?: item.backdrop,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val genreText = remember(item.genreIds) { formatGenres(item.genreIds, isTv = true) }
                    genreText?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    item.firstAirDate?.takeIf { it.isNotBlank() }?.let { date ->
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = date.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    val rating5 = (item.rating ?: 0.0) / 2.0
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { index ->
                            val filled = rating5 >= index + 1
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = String.format("%.1f/5", rating5),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!item.character.isNullOrBlank()) {
                        Text(
                            text = "Personaje: ${item.character}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showAvailability && item.availableSeriesId == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "No disponible",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            if (!item.overview.isNullOrBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (overviewExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (overviewExpanded) "Mostrar menos" else "Mostrar más",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { overviewExpanded = !overviewExpanded }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun ActorUnavailableSeriesDialog(
    item: TmdbSeriesLink,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = item.poster ?: item.backdrop,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!item.firstAirDate.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "No disponible · ${item.firstAirDate.take(4)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (!item.overview.isNullOrBlank()) {
                            Text(
                                text = item.overview,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private val tmdbMovieGenreMap = mapOf(
    28 to "Accion",
    12 to "Aventura",
    16 to "Animacion",
    35 to "Comedia",
    80 to "Crimen",
    99 to "Documental",
    18 to "Drama",
    10751 to "Familia",
    14 to "Fantasia",
    36 to "Historia",
    27 to "Terror",
    10402 to "Musica",
    9648 to "Misterio",
    10749 to "Romance",
    878 to "Ciencia ficcion",
    10770 to "Pelicula de TV",
    53 to "Suspenso",
    10752 to "Guerra",
    37 to "Western"
)

private val tmdbTvGenreMap = mapOf(
    10759 to "Accion y aventura",
    16 to "Animacion",
    35 to "Comedia",
    80 to "Crimen",
    99 to "Documental",
    18 to "Drama",
    10751 to "Familia",
    10762 to "Infantil",
    9648 to "Misterio",
    10763 to "Noticias",
    10764 to "Reality",
    10765 to "Ciencia ficcion y fantasia",
    10766 to "Telenovela",
    10767 to "Charlas",
    10768 to "Guerra y politica",
    37 to "Western"
)

private fun formatGenres(genreIds: List<Int>, isTv: Boolean): String? {
    val lookup = if (isTv) tmdbTvGenreMap + tmdbMovieGenreMap else tmdbMovieGenreMap + tmdbTvGenreMap
    val names = genreIds.mapNotNull { lookup[it] }.distinct()
    return names.takeIf { it.isNotEmpty() }?.joinToString(" / ")
}

