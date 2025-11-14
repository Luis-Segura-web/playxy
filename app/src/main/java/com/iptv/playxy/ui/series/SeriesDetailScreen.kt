package com.iptv.playxy.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.iptv.playxy.domain.Episode
import com.iptv.playxy.domain.Series
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.PlayerManager
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.ui.player.SeriesMiniPlayer
import com.iptv.playxy.util.StreamUrlBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    categoryId: String,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var expandedSeason by remember { mutableIntStateOf(1) }
    var currentEpisode by remember { mutableStateOf<Episode?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isFullscreenLocal by remember { mutableStateOf(false) }
    val globalFullscreenState = LocalFullscreenState.current
    val snackbarHostState = remember { SnackbarHostState() }
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    val seriesVm: SeriesViewModel = hiltViewModel()
    val seriesListUi by seriesVm.uiState.collectAsState()
    val snackbarScope = rememberCoroutineScope()

    // Sync local fullscreen state with global state
    LaunchedEffect(isFullscreenLocal) {
        globalFullscreenState.value = isFullscreenLocal
    }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current

    // Load series info when screen opens
    LaunchedEffect(seriesId, categoryId) {
        viewModel.loadSeriesInfo(seriesId, categoryId)
    }

    // Get all episodes in order for navigation
    val allEpisodes = remember(uiState.seasons) {
        uiState.seasons.toSortedMap().values.flatten()
    }

    val currentEpisodeIndex = remember(currentEpisode, allEpisodes) {
        currentEpisode?.let { episode ->
            allEpisodes.indexOfFirst { it.id == episode.id }
        } ?: -1
    }

    if (isFullscreenLocal && currentEpisode != null && userProfile != null) {
        // Fullscreen player in landscape mode
        FullscreenPlayer(
            streamUrl = StreamUrlBuilder.buildSeriesStreamUrl(
                userProfile!!,
                currentEpisode!!.id,
                currentEpisode!!.containerExtension
            ),
            title = "${uiState.series?.name} - T${currentEpisode!!.season} E${currentEpisode!!.episodeNum}",
            playerType = PlayerType.SERIES,
            playerManager = playerManager,
            onBack = {
                // guardar posición y salir
                lastPositionMs = playerManager.getCurrentPosition()
                isFullscreenLocal = false
            },
            onPreviousItem = {
                if (currentEpisodeIndex > 0) {
                    currentEpisode = allEpisodes[currentEpisodeIndex - 1]
                }
            },
            onNextItem = {
                if (currentEpisodeIndex < allEpisodes.size - 1) {
                    currentEpisode = allEpisodes[currentEpisodeIndex + 1]
                }
            },
            hasPrevious = currentEpisodeIndex > 0,
            hasNext = currentEpisodeIndex < allEpisodes.size - 1
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.series?.name ?: "Detalles de Serie",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    },
                    actions = {
                        val s = uiState.series
                        if (s != null) {
                            val isFavorite = seriesListUi.favoriteIds.contains(s.seriesId)
                            IconButton(onClick = {
                                seriesVm.toggleFavorite(s.seriesId)
                                val msg = if (isFavorite) "Quitado de favoritos" else "Añadido a favoritos"
                                snackbarScope.launch { snackbarHostState.showSnackbar(msg) }
                            }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Mini player when episode is playing
                if (isPlaying && currentEpisode != null && userProfile != null) {
                    SeriesMiniPlayer(
                        streamUrl = StreamUrlBuilder.buildSeriesStreamUrl(
                            userProfile!!,
                            currentEpisode!!.id,
                            currentEpisode!!.containerExtension
                        ),
                        episodeTitle = currentEpisode!!.title,
                        seasonNumber = currentEpisode!!.season,
                        episodeNumber = currentEpisode!!.episodeNum,
                        playerManager = playerManager,
                        onPreviousEpisode = {
                            if (currentEpisodeIndex > 0) {
                                currentEpisode = allEpisodes[currentEpisodeIndex - 1]
                                // Guardar progreso al cambiar de episodio
                                viewModel.saveProgress(uiState.series!!.seriesId, currentEpisode!!, 0L)
                            }
                        },
                        onNextEpisode = {
                            if (currentEpisodeIndex < allEpisodes.size - 1) {
                                currentEpisode = allEpisodes[currentEpisodeIndex + 1]
                                // Guardar progreso al cambiar de episodio
                                viewModel.saveProgress(uiState.series!!.seriesId, currentEpisode!!, 0L)
                            }
                        },
                        onClose = {
                            playerManager.pause()
                            // No liberar singleton
                            isPlaying = false
                            currentEpisode = null
                        },
                        onFullscreen = { isFullscreenLocal = true },
                        hasPrevious = currentEpisodeIndex > 0,
                        hasNext = currentEpisodeIndex < allEpisodes.size - 1
                    )

                    // Guardar progreso periódicamente cada 10 segundos
                    LaunchedEffect(isPlaying, currentEpisode) {
                        while (isPlaying && currentEpisode != null) {
                            delay(10000) // 10 segundos
                            val currentPos = playerManager.getCurrentPosition()
                            if (currentPos > 0) {
                                viewModel.saveProgress(
                                    uiState.series!!.seriesId,
                                    currentEpisode!!,
                                    currentPos
                                )
                            }
                        }
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.series == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "Serie no encontrada",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Cover Image and Basic Info
                            item {
                                SeriesHeader(uiState.series!!)
                            }

                            // Series Details
                            item {
                                SeriesDetails(uiState.series!!)
                            }

                            // Botón Continuar si hay progreso guardado
                            if (uiState.lastEpisode != null) {
                                item {
                                    // Usar variable local para evitar smart cast sobre propiedad delegada
                                    val lastEp = uiState.lastEpisode!!
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "Último episodio visto",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "T${lastEp.season}E${lastEp.episodeNum}",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = lastEp.title,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Button(
                                                    onClick = {
                                                        currentEpisode = lastEp
                                                        isPlaying = true
                                                        // Registrar reciente
                                                        seriesVm.onSeriesOpened(uiState.series!!.seriesId)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Continuar"
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Continuar")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Error message if seasons couldn't load
                            if (uiState.error != null && uiState.seasons.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = uiState.error!!,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }

                            // Seasons and Episodes Section
                            if (uiState.seasons.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Temporadas y Episodios",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                // Season List
                                val sortedSeasons = uiState.seasons.keys.sorted()
                                items(sortedSeasons) { seasonNumber ->
                                    val episodes = uiState.seasons[seasonNumber] ?: emptyList()
                                    SeasonCard(
                                        seasonNumber = seasonNumber,
                                        episodes = episodes,
                                        isExpanded = expandedSeason == seasonNumber,
                                        onExpandClick = {
                                            expandedSeason = if (expandedSeason == seasonNumber) -1 else seasonNumber
                                        },
                                        onEpisodeClick = { episode ->
                                            currentEpisode = episode
                                            isPlaying = true
                                            // Registrar reciente
                                            seriesVm.onSeriesOpened(uiState.series!!.seriesId)
                                            // Guardar progreso (último episodio visto)
                                            viewModel.saveProgress(uiState.series!!.seriesId, episode, positionMs = 0L)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Restaurar posición al volver de fullscreen
    LaunchedEffect(lastPositionMs, isPlaying) {
        if (lastPositionMs > 0L && isPlaying) {
            playerManager.seekTo(lastPositionMs)
        }
    }
}

@Composable
fun SeriesHeader(series: Series) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Backdrop or Cover Image
        AsyncImage(
            model = series.backdropPath.firstOrNull() ?: series.cover,
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

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        startY = 100f
                    )
                )
        )
    }
}

@Composable
fun SeriesDetails(series: Series) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            text = series.name,
            style = MaterialTheme.typography.headlineMedium
        )

        // Rating
        if (series.rating5Based > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "⭐", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = String.format("%.1f / 5.0", series.rating5Based),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Genre and Release Date
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!series.genre.isNullOrEmpty()) {
                Chip(label = series.genre)
            }
            if (!series.releaseDate.isNullOrEmpty()) {
                Chip(label = series.releaseDate)
            }
        }

        // Plot
        if (!series.plot.isNullOrEmpty()) {
            Text(
                text = "Sinopsis",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = series.plot,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Cast
        if (!series.cast.isNullOrEmpty()) {
            InfoRow(label = "Reparto", value = series.cast)
        }

        // Director
        if (!series.director.isNullOrEmpty()) {
            InfoRow(label = "Director", value = series.director)
        }

        // Episode Run Time
        if (!series.episodeRunTime.isNullOrEmpty()) {
            InfoRow(label = "Duración", value = "${series.episodeRunTime} min")
        }
    }
}

@Composable
fun SeasonCard(
    seasonNumber: Int,
    episodes: List<Episode>,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onEpisodeClick: (Episode) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Season Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandClick)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Temporada $seasonNumber",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${episodes.size} episodios",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir"
                )
            }

            // Episodes List
            if (isExpanded) {
                Divider()
                episodes.forEach { episode ->
                    EpisodeItem(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode Number Badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = episode.episodeNum.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Episode Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = episode.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            episode.info?.let { info ->
                if (!info.duration.isNullOrEmpty()) {
                    Text(
                        text = info.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Play Icon
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Reproducir",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun Chip(label: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
