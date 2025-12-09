package com.iptv.playxy.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iptv.playxy.data.db.EpisodeProgressEntity
import com.iptv.playxy.domain.Episode
import com.iptv.playxy.domain.Series
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.components.DetailLoadingScreen
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.PlayerSurface
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.ui.player.SeriesMiniPlayer
import com.iptv.playxy.util.StreamUrlBuilder
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    categoryId: String,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToSeries: (String, String) -> Unit = { _, _ -> },
    onNavigateToActor: (com.iptv.playxy.domain.TmdbCast, Boolean) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var expandedSeason by remember { mutableIntStateOf(1) }
    var currentEpisode by remember { mutableStateOf<Episode?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value
    val snackbarHostState = remember { SnackbarHostState() }
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    val seriesVm: SeriesViewModel = hiltViewModel()
    val seriesListUi by seriesVm.uiState.collectAsState()
    val snackbarScope = rememberCoroutineScope()
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    var currentPlaybackDuration by remember { mutableLongStateOf(0L) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var accessGranted by remember { mutableStateOf(false) }
    var gatePin by remember { mutableStateOf("") }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current
    val pipController = LocalPipController.current
    val isInPip by pipController.isInPip.collectAsStateWithLifecycle()
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    var isEpisodeSwitching by remember { mutableStateOf(false) }
    val series = uiState.series
    val lastEpisodeCover = uiState.lastEpisode?.info?.cover
    val firstEpisodeCover = uiState.seasons.values.flatten()
        .firstOrNull { !it.info?.cover.isNullOrBlank() }
        ?.info?.cover
    
    // Imagen de backdrop - usa la mejor disponible sin cambiar una vez cargada
    // Prioridad: backdrops de TMDB -> covers de episodios -> poster de la serie
    val headerImageUrl = remember(series, lastEpisodeCover, firstEpisodeCover) {
        series?.backdropPath?.firstOrNull { it.isNotBlank() }
            ?: lastEpisodeCover?.takeIf { it.isNotBlank() }
            ?: firstEpisodeCover?.takeIf { it.isNotBlank() }
            ?: series?.cover?.takeIf { it.isNotBlank() }
    }
    val displayTitle = series?.name.orEmpty()
    val ratingValue = series?.rating5Based ?: 0f

    // Load series info when screen opens
    LaunchedEffect(seriesId, categoryId, seriesListUi.categories) {
        gatePin = ""
        pinError = null
        accessGranted = false
        val restricted = seriesVm.requiresPinForCategory(categoryId)
        if (restricted) {
            showPinDialog = true
        } else {
            showPinDialog = false
            accessGranted = true
        }
    }

    LaunchedEffect(seriesId, categoryId, accessGranted) {
        if (accessGranted) {
            viewModel.loadSeriesInfo(seriesId, categoryId)
        }
    }

    val shouldShowHeaderPlayer = isPlaying && currentEpisode != null && userProfile != null

    // Get all episodes in order for navigation
    val allEpisodes = remember(uiState.seasons) {
        uiState.seasons.toSortedMap().values.flatten()
    }

    val currentEpisodeIndex = remember(currentEpisode, allEpisodes) {
        currentEpisode?.let { episode ->
            allEpisodes.indexOfFirst { it.id == episode.id }
        } ?: -1
    }

    DisposableEffect(Unit) {
        onDispose {
            // Guardar progreso antes de salir de la pantalla
            if (isPlaying && currentEpisode != null && uiState.series != null) {
                val currentPos = playerManager.getCurrentPosition()
                val duration = playerManager.getDuration()
                if (currentPos > 0 && duration > 0) {
                    viewModel.saveEpisodeProgress(
                        episodeId = currentEpisode!!.id,
                        seriesId = uiState.series!!.seriesId,
                        seasonNumber = currentEpisode!!.season,
                        episodeNumber = currentEpisode!!.episodeNum,
                        positionMs = currentPos,
                        durationMs = duration
                    )
                }
                // Guardar el último episodio visto
                viewModel.saveProgress(uiState.series!!.seriesId, currentEpisode!!, currentPos)
            }
            playerManager.stopPlayback()
            isPlaying = false
            viewModel.setCurrentPlayingEpisode(null)
            currentEpisode = null
            fullscreenState.value = false
        }
    }

    LaunchedEffect(playbackState.streamUrl) {
        if (!playerManager.hasActivePlayback() && isPlaying && !isEpisodeSwitching && !isInPip) {
            isPlaying = false
            currentEpisode = null
            fullscreenState.value = false
        }
        if (playbackState.streamUrl != null) {
            isEpisodeSwitching = false
        }
    }

    // Ensure playback keeps running when exiting fullscreen back to mini player
    LaunchedEffect(isFullscreen, isPlaying) {
        if (!isFullscreen && isPlaying) {
            playerManager.play()
        }
    }

    fun playEpisode(episode: Episode) {
        if (userProfile != null && uiState.series != null) {
            isEpisodeSwitching = true
            currentEpisode = episode
            isPlaying = true
            viewModel.setCurrentPlayingEpisode(episode.id)
            
            // Cargar posición guardada si existe
            val episodeProgress = uiState.episodeProgress[episode.id]
            lastPositionMs = episodeProgress?.positionMs ?: 0L
            
            val url = StreamUrlBuilder.buildSeriesStreamUrl(
                userProfile!!,
                episode.id,
                episode.containerExtension
            )
            playerManager.playMedia(url, PlayerType.SERIES, forcePrepare = true)
            viewModel.saveProgress(uiState.series!!.seriesId, episode, 0L)
        }
    }

    // Actualizar posición de reproducción cada segundo para la barra de progreso
    LaunchedEffect(isPlaying, currentEpisode) {
        if (isPlaying && currentEpisode != null) {
            while (isPlaying) {
                kotlinx.coroutines.delay(1000) // Actualizar cada segundo
                currentPlaybackPosition = playerManager.getCurrentPosition()
                currentPlaybackDuration = playerManager.getDuration()
            }
        }
    }

    // Guardar progreso del episodio actual cada 10 segundos
    LaunchedEffect(isPlaying, currentEpisode) {
        if (isPlaying && currentEpisode != null && uiState.series != null) {
            while (isPlaying) {
                kotlinx.coroutines.delay(10000) // Guardar cada 10 segundos
                val currentPos = playerManager.getCurrentPosition()
                val duration = playerManager.getDuration()
                if (currentPos > 0 && duration > 0) {
                    // Guardar progreso del episodio específico
                    viewModel.saveEpisodeProgress(
                        episodeId = currentEpisode!!.id,
                        seriesId = uiState.series!!.seriesId,
                        seasonNumber = currentEpisode!!.season,
                        episodeNumber = currentEpisode!!.episodeNum,
                        positionMs = currentPos,
                        durationMs = duration
                    )
                    // Guardar también como último episodio visto de la serie
                    viewModel.saveProgress(uiState.series!!.seriesId, currentEpisode!!, currentPos)
                }
            }
        }
    }

    // Restaurar posición cuando hay lastPositionMs y el reproductor está listo
    LaunchedEffect(lastPositionMs, isPlaying, playbackState.firstFrameRendered, currentEpisode?.id) {
        if (lastPositionMs > 0L && isPlaying && playbackState.firstFrameRendered) {
            val currentPos = playerManager.getCurrentPosition()
            if (abs(currentPos - lastPositionMs) > 750L) {
                kotlinx.coroutines.delay(500) // Esperar un poco para que el player esté completamente listo
                playerManager.seekTo(lastPositionMs)
            }
            lastPositionMs = 0L // Reset para evitar múltiples seeks
        }
    }

    val currentStreamUrl = remember(currentEpisode, userProfile) {
        if (currentEpisode != null && userProfile != null) {
            StreamUrlBuilder.buildSeriesStreamUrl(
                userProfile!!,
                currentEpisode!!.id,
                currentEpisode!!.containerExtension
            )
        } else null
    }

    DisposableEffect(currentEpisode?.id, isPlaying, isFullscreen) {
        val hasEpisode = currentEpisode != null && isPlaying
        if (hasEpisode && uiState.series != null) {
            val canPrev = currentEpisodeIndex > 0
            val canNext = currentEpisodeIndex in 0 until (allEpisodes.size - 1)
            val prevAction = if (canPrev) {
                {
                    isEpisodeSwitching = true
                    playerManager.stopPlayback()
                    val prevEpisode = allEpisodes[currentEpisodeIndex - 1]
                    currentEpisode = prevEpisode
                    viewModel.setCurrentPlayingEpisode(prevEpisode.id)
                    viewModel.saveProgress(uiState.series!!.seriesId, prevEpisode, 0L)
                }
            } else null
            val nextAction = if (canNext) {
                {
                    isEpisodeSwitching = true
                    playerManager.stopPlayback()
                    val nextEpisode = allEpisodes[currentEpisodeIndex + 1]
                    currentEpisode = nextEpisode
                    viewModel.setCurrentPlayingEpisode(nextEpisode.id)
                    viewModel.saveProgress(uiState.series!!.seriesId, nextEpisode, 0L)
                }
            } else null
            playerManager.setTransportActions(nextAction, prevAction)
        } else {
            playerManager.setTransportActions(null, null)
        }
        onDispose { playerManager.setTransportActions(null, null) }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { onBackClick() },
            title = { Text("PIN requerido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ingresa el PIN para acceder a esta serie.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = gatePin,
                        onValueChange = { gatePin = sanitizePinInput(it) },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sanitized = sanitizePinInput(gatePin)
                    gatePin = sanitized
                    if (sanitized.length != 4) {
                        pinError = "Debes ingresar un PIN de 4 dígitos."
                        return@Button
                    }
                    snackbarScope.launch {
                        val valid = seriesVm.validateParentalPin(sanitized)
                        if (valid) {
                            accessGranted = true
                            showPinDialog = false
                            pinError = null
                        } else {
                            pinError = "PIN incorrecto."
                        }
                    }
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = { TextButton(onClick = { onBackClick() }) { Text("Cancelar") } }
        )
    }

    if (!accessGranted) {
        Box(modifier = Modifier.fillMaxSize())
    } else if (uiState.isLoading && uiState.series == null) {
        // Mostrar pantalla de carga mientras se cargan los datos
        DetailLoadingScreen(
            onBackClick = onBackClick
        )
    } else if (isFullscreen && currentEpisode != null && userProfile != null && currentStreamUrl != null) {
        // Fullscreen player in landscape mode
        FullscreenPlayer(
            streamUrl = currentStreamUrl,
            title = "${displayTitle.ifBlank { uiState.series?.name.orEmpty() }} - T${currentEpisode!!.season} E${currentEpisode!!.episodeNum}",
            playerType = PlayerType.SERIES,
            playerManager = playerManager,
            onBack = {
                fullscreenState.value = false
            },
            onPreviousItem = {
                if (currentEpisodeIndex > 0) {
                    isEpisodeSwitching = true
                    playerManager.stopPlayback()
                    currentEpisode = allEpisodes[currentEpisodeIndex - 1]
                }
            },
            onNextItem = {
                if (currentEpisodeIndex < allEpisodes.size - 1) {
                    isEpisodeSwitching = true
                    playerManager.stopPlayback()
                    currentEpisode = allEpisodes[currentEpisodeIndex + 1]
                }
            },
            hasPrevious = currentEpisodeIndex > 0,
            hasNext = currentEpisodeIndex < allEpisodes.size - 1
        )
    } else {
        val gradient = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surface
            )
        )
        val seasonNumbers = uiState.seasons.keys.sorted()
        var selectedSeason by remember { mutableIntStateOf(seasonNumbers.firstOrNull() ?: 1) }
        var synopsisExpanded by remember { mutableStateOf(false) }
        var unavailableSeriesInfo by remember { mutableStateOf<com.iptv.playxy.domain.TmdbSeriesLink?>(null) }

        LaunchedEffect(uiState.seasons) {
            if (seasonNumbers.isNotEmpty()) {
                selectedSeason = seasonNumbers.first()
                expandedSeason = selectedSeason
            }
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                ) 
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val series = uiState.series
                    if (shouldShowHeaderPlayer && currentStreamUrl != null) {
                        val ep = currentEpisode!!
                        SeriesMiniPlayer(
                            streamUrl = currentStreamUrl,
                            episodeTitle = displayEpisodeTitle(ep, series?.name),
                            seasonNumber = ep.season,
                            episodeNumber = ep.episodeNum,
                            playerManager = playerManager,
                            onPreviousEpisode = {
                                if (currentEpisodeIndex > 0) {
                                    isEpisodeSwitching = true
                                    playerManager.stopPlayback()
                                    val prevEpisode = allEpisodes[currentEpisodeIndex - 1]
                                    currentEpisode = prevEpisode
                                    viewModel.setCurrentPlayingEpisode(prevEpisode.id)
                                }
                            },
                            onNextEpisode = {
                                if (currentEpisodeIndex < allEpisodes.size - 1) {
                                    isEpisodeSwitching = true
                                    playerManager.stopPlayback()
                                    val nextEpisode = allEpisodes[currentEpisodeIndex + 1]
                                    currentEpisode = nextEpisode
                                    viewModel.setCurrentPlayingEpisode(nextEpisode.id)
                                }
                            },
                            onClose = {
                                // Guardar progreso antes de cerrar
                                if (uiState.series != null && currentEpisode != null) {
                                    val currentPos = playerManager.getCurrentPosition()
                                    val episodeToSave = currentEpisode!!
                                    if (currentPos > 0) {
                                        viewModel.saveProgress(uiState.series!!.seriesId, episodeToSave, currentPos)
                                    }
                                }
                                playerManager.stopPlayback()
                                isPlaying = false
                                // No limpiar currentPlayingEpisodeId para que el episodio siga seleccionado
                                currentEpisode = null
                            },
                            onFullscreen = {
                                fullscreenState.value = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (series != null) {
                        AsyncImage(
                            model = headerImageUrl,
                            contentDescription = displayTitle.ifBlank { series.name },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                            placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 100f
                                    )
                                )
                        )
                    }

                    if (!shouldShowHeaderPlayer) {
                        IconButton(
                            onClick = {
                                playerManager.stopPlayback()
                                isPlaying = false
                                currentEpisode = null
                                onBackClick()
                                fullscreenState.value = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver"
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.series != null) {
                    val series = uiState.series!!
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val primaryEpisode = currentEpisode
                        ?: run {
                            if (uiState.currentPlayingEpisodeId != null) {
                                allEpisodes.find { it.id == uiState.currentPlayingEpisodeId }
                            } else {
                                uiState.lastEpisode
                            }
                        }
                        ?: uiState.seasons[selectedSeason]?.firstOrNull()

                    val episodeHasProgress = primaryEpisode?.let { ep ->
                        val savedProgress = uiState.episodeProgress[ep.id]
                        val isCurrentlyPlaying = uiState.currentPlayingEpisodeId == ep.id
                        (savedProgress != null && savedProgress.positionMs > 0) || (isCurrentlyPlaying && currentPlaybackPosition > 0)
                    } ?: false
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabRow(selectedTabIndex = selectedTab) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Info") })
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Episodios") })
                        }
                        when (selectedTab) {
                            0 -> InfoTabContent(
                                series = series,
                                ratingValue = ratingValue,
                                seasonCount = seasonNumbers.size,
                                synopsisExpanded = synopsisExpanded,
                                onToggleSynopsis = { synopsisExpanded = !synopsisExpanded },
                                tmdbEnabled = uiState.tmdbEnabled,
                                catalogHasTmdb = uiState.catalogHasTmdb,
                                hasTmdbId = !series.tmdbId.isNullOrBlank(),
                                tmdbCast = uiState.tmdbCast,
                                tmdbSimilar = uiState.tmdbSimilar,
                                tmdbCollection = uiState.tmdbCollection,
                                onNavigateToActor = onNavigateToActor,
                                onNavigateToSeries = onNavigateToSeries,
                                onUnavailable = { unavailableSeriesInfo = it }
                            )
                            1 -> EpisodesTabContent(
                                series = series,
                                seasonNumbers = seasonNumbers,
                                selectedSeasonState = { selectedSeason },
                                onSeasonSelected = { selectedSeason = it; expandedSeason = it },
                                episodesBySeason = uiState.seasons,
                                resumeEpisode = primaryEpisode,
                                episodeHasProgress = episodeHasProgress,
                                onPlayResume = {
                                    primaryEpisode?.let { ep ->
                                        if (episodeHasProgress) {
                                            val savedProgress = uiState.episodeProgress[ep.id]
                                            val isCurrentlyPlaying = uiState.currentPlayingEpisodeId == ep.id
                                            lastPositionMs = if (isCurrentlyPlaying && currentPlaybackPosition > 0) {
                                                currentPlaybackPosition
                                            } else {
                                                savedProgress?.positionMs ?: 0L
                                            }
                                        }
                                        playEpisode(ep)
                                    }
                                },
                                episodeProgress = uiState.episodeProgress,
                                currentPlayingEpisodeId = uiState.currentPlayingEpisodeId,
                                currentPlaybackPosition = currentPlaybackPosition,
                                currentPlaybackDuration = currentPlaybackDuration,
                                onPlayEpisode = { ep ->
                                    val seriesDomain = uiState.series
                                    if (seriesDomain != null) {
                                        seriesVm.onSeriesOpened(seriesDomain.seriesId)
                                    }
                                    viewModel.setCurrentPlayingEpisode(ep.id)
                                    playEpisode(ep)
                                }
                            )
                        }
                    }
                    unavailableSeriesInfo?.let { info ->
                        UnavailableSeriesDialog(item = info, onDismiss = { unavailableSeriesInfo = null })
                    }
                } else {
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
fun SynopsisBlock(synopsis: String?, expanded: Boolean, onToggle: () -> Unit) {
    if (synopsis.isNullOrEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Sinopsis",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = synopsis,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        TextButton(onClick = onToggle) {
            Text(if (expanded) "Ver menos" else "Ver más")
        }
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    seriesName: String? = null,
    episodeProgress: EpisodeProgressEntity? = null,
    isCurrentlyPlaying: Boolean = false,
    currentPlaybackPosition: Long = 0L,
    currentPlaybackDuration: Long = 0L,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = if (isCurrentlyPlaying) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail más pequeño
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isCurrentlyPlaying) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    AsyncImage(
                        model = episode.info?.cover,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                        placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                    )
                    
                    // Barra de progreso dentro del thumbnail
                    // Usar posición en tiempo real si está reproduciendo este episodio, sino usar progreso guardado
                    val savedPosition = episodeProgress?.positionMs ?: 0L
                    val savedDuration = episodeProgress?.durationMs ?: 1L
                    val displayPosition = if (isCurrentlyPlaying && currentPlaybackDuration > 0) currentPlaybackPosition else savedPosition
                    val displayDuration = if (isCurrentlyPlaying && currentPlaybackDuration > 0) currentPlaybackDuration else savedDuration
                    val showProgressBar = displayPosition > 0 && displayDuration > 0
                    
                    if (showProgressBar) {
                        val progress = (displayPosition.toFloat() / displayDuration.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = if (isCurrentlyPlaying) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Título y metadata
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayEpisodeTitle(episode, seriesName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentlyPlaying) 
                            MaterialTheme.colorScheme.primary
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating
                        if ((episode.info?.rating ?: 0f) > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                repeat(5) { index ->
                                    val filled = (episode.info?.rating ?: 0f) >= index + 1
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        
                        // Duración
                        if (!episode.info?.duration.isNullOrEmpty()) {
                            Text(
                                text = episode.info?.duration ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Descripción debajo de todo (solo si existe)
            if (!episode.info?.plot.isNullOrEmpty()) {
                Text(
                    text = episode.info?.plot ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun sanitizePinInput(input: String): String = input.filter { it.isDigit() }.take(4)

private fun displayEpisodeTitle(episode: Episode, seriesName: String? = null): String {
    val seasonCode = episode.season.toString().padStart(2, '0')
    val episodeCode = episode.episodeNum.toString().padStart(2, '0')
    val base = "S${seasonCode}E${episodeCode}"
    val rawTitle = episode.title?.trim().orEmpty()
    val cleaned = cleanEpisodeTitle(rawTitle, seriesName, seasonCode, episodeCode)
    val finalTitle = cleaned.ifBlank { "Episodio $episodeCode" }
    return "$base - $finalTitle"
}

private fun cleanEpisodeTitle(title: String, seriesName: String?, seasonCode: String, episodeCode: String): String {
    var result = title
    val series = seriesName?.trim().orEmpty()
    if (series.isNotEmpty()) {
        val lowerSeries = series.lowercase()
        val lowerTitle = result.lowercase()
        if (lowerTitle.startsWith(lowerSeries)) {
            result = result.drop(series.length).trimStart(' ', '-', ':', '|', '.')
        }
        // Caso "Serie - Episodio"
        val dashParts = result.split(" - ", limit = 2)
        if (dashParts.size == 2 && dashParts[0].equals(series, ignoreCase = true)) {
            result = dashParts[1].trim()
        }
    }
    // Quitar prefijos tipo S01E06 o S1E6 para evitar duplicados
    val codeRegex = Regex("^S0?${seasonCode.toInt()}E0?${episodeCode.toInt()}\\s*[-:|]?\\s*", RegexOption.IGNORE_CASE)
    result = result.replace(codeRegex, "")
    // También remover patrones genéricos SxxExx al inicio
    result = result.replace(Regex("^S\\d{1,2}E\\d{1,2}\\s*[-:|]?\\s*", RegexOption.IGNORE_CASE), "")
    return result.trim()
}

@Composable
private fun InfoTabContent(
    series: Series,
    ratingValue: Float,
    seasonCount: Int,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    tmdbEnabled: Boolean,
    catalogHasTmdb: Boolean,
    hasTmdbId: Boolean,
    tmdbCast: List<com.iptv.playxy.domain.TmdbCast>,
    tmdbSimilar: List<com.iptv.playxy.domain.TmdbSeriesLink>,
    tmdbCollection: List<com.iptv.playxy.domain.TmdbSeriesLink>,
    onNavigateToActor: (com.iptv.playxy.domain.TmdbCast, Boolean) -> Unit,
    onNavigateToSeries: (String, String) -> Unit,
    onUnavailable: (com.iptv.playxy.domain.TmdbSeriesLink) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val initialFontSize = MaterialTheme.typography.headlineMedium.fontSize
                var fontSize by remember { mutableStateOf(initialFontSize) }
                var reductionCount by remember { mutableIntStateOf(0) }

                Text(
                    text = series.name,
                    fontSize = fontSize,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    onTextLayout = { textLayoutResult ->
                        if (textLayoutResult.hasVisualOverflow && reductionCount < 5) {
                            reductionCount++
                            fontSize = fontSize * 0.9f
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { index ->
                            val filled = ratingValue >= index + 1
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "$seasonCount Temporadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!series.releaseDate.isNullOrEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = series.releaseDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        SynopsisBlock(
            synopsis = series.plot,
            expanded = synopsisExpanded,
            onToggle = onToggleSynopsis
        )

        Text(
            text = "Género: ${series.genre ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Dirigido por: ${series.director ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Caso 1 y 2: TMDB habilitado Y la serie tiene tmdb_id -> mostrar reparto con imágenes
        // Caso 1: tmdbEnabled + !catalogHasTmdb + hasTmdbId -> sin buscar disponibilidad
        // Caso 2: tmdbEnabled + catalogHasTmdb + hasTmdbId -> con disponibilidad
        if (tmdbEnabled && hasTmdbId && tmdbCast.isNotEmpty()) {
            Text(
                text = "Reparto & Equipo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(tmdbCast) { cast ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(90.dp)
                            .clickable { onNavigateToActor(cast, catalogHasTmdb) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = cast.profile,
                                contentDescription = cast.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = cast.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!cast.character.isNullOrBlank()) {
                            Text(
                                text = cast.character,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else if (!tmdbEnabled && !series.cast.isNullOrBlank()) {
            // Caso 3: TMDB deshabilitado -> mostrar reparto como texto del proveedor
            Text(
                text = "Reparto & Equipo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = series.cast,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Colección relacionada (solo si TMDB habilitado Y el catálogo soporta TMDB Y hay rastreo)
        if (tmdbEnabled && catalogHasTmdb && hasTmdbId && tmdbCollection.size > 1) {
            Text(
                text = "Colección relacionada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tmdbCollection) { item ->
                    SeriesSimilarCard(
                        item = item,
                        onClick = {
                            if (item.availableSeriesId != null && item.availableCategoryId != null) {
                                onNavigateToSeries(item.availableSeriesId, item.availableCategoryId)
                            } else {
                                onUnavailable(item)
                            }
                        }
                    )
                }
            }
        }

        // Series similares (solo si TMDB habilitado Y el catálogo soporta TMDB)
        if (tmdbEnabled && catalogHasTmdb && hasTmdbId && tmdbSimilar.isNotEmpty()) {
            Text(
                text = "Series similares",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tmdbSimilar) { item ->
                    SeriesSimilarCard(
                        item = item,
                        onClick = {
                            if (item.availableSeriesId != null && item.availableCategoryId != null) {
                                onNavigateToSeries(item.availableSeriesId, item.availableCategoryId)
                            } else {
                                onUnavailable(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodesTabContent(
    series: Series,
    seasonNumbers: List<Int>,
    selectedSeasonState: () -> Int,
    onSeasonSelected: (Int) -> Unit,
    episodesBySeason: Map<Int, List<Episode>>,
    resumeEpisode: Episode?,
    episodeHasProgress: Boolean,
    onPlayResume: () -> Unit,
    episodeProgress: Map<String, EpisodeProgressEntity>,
    currentPlayingEpisodeId: String?,
    currentPlaybackPosition: Long,
    currentPlaybackDuration: Long,
    onPlayEpisode: (Episode) -> Unit
) {
    val selectedSeason = selectedSeasonState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Temporadas: ${seasonNumbers.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        resumeEpisode?.let { ep ->
            Button(
                onClick = onPlayResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (episodeHasProgress) "Reanudar" else "Ver ahora",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displayEpisodeTitle(ep, series.name),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var seasonMenuExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier.clickable { seasonMenuExpanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temporada $selectedSeason",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(
                    expanded = seasonMenuExpanded,
                    onDismissRequest = { seasonMenuExpanded = false }
                ) {
                    seasonNumbers.forEach { seasonNum ->
                        DropdownMenuItem(
                            text = { Text("Temporada $seasonNum") },
                            onClick = {
                                onSeasonSelected(seasonNum)
                                seasonMenuExpanded = false
                            }
                        )
                    }
                }
            }

            val episodes = episodesBySeason[selectedSeason] ?: emptyList()
            Text(
                text = "Episodios (${episodes.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val episodes = episodesBySeason[selectedSeason] ?: emptyList()
        episodes.forEach { ep ->
            EpisodeCard(
                episode = ep,
                seriesName = series.name,
                episodeProgress = episodeProgress[ep.id],
                isCurrentlyPlaying = currentPlayingEpisodeId == ep.id,
                currentPlaybackPosition = if (currentPlayingEpisodeId == ep.id) currentPlaybackPosition else 0L,
                currentPlaybackDuration = if (currentPlayingEpisodeId == ep.id) currentPlaybackDuration else 0L,
                onPlay = { onPlayEpisode(ep) }
            )
        }
    }
}

@Composable
private fun SeriesSimilarCard(
    item: com.iptv.playxy.domain.TmdbSeriesLink,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
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
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            item.firstAirDate?.takeIf { it.isNotBlank() }?.let { date ->
                Text(
                    text = date.take(4),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.rating != null) {
                val rating5 = item.rating / 2
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = String.format("%.1f", rating5),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailableSeriesDialog(
    item: com.iptv.playxy.domain.TmdbSeriesLink,
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
                        text = item.tmdbTitle ?: item.title,
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
