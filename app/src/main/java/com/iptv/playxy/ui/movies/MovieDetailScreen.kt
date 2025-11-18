package com.iptv.playxy.ui.movies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.MovieMiniPlayer
import com.iptv.playxy.ui.player.PlayerManager
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.util.StreamUrlBuilder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movie: VodStream,
    onBackClick: () -> Unit,
    onPlayClick: (VodStream) -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val userProfile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    var showResumeDialog by remember { mutableStateOf(false) }

    // Load movie details when screen opens
    LaunchedEffect(movie.streamId) {
        viewModel.loadMovieInfo(movie.streamId)
    }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    val pipController = LocalPipController.current
    val isInPip by pipController.isInPip.collectAsStateWithLifecycle()

    DisposableEffect(isPlaying, isFullscreen) {
        playerManager.setTransportActions(null, null)
        onDispose { playerManager.setTransportActions(null, null) }
    }

    LaunchedEffect(playbackState.streamUrl) {
        if (playbackState.streamUrl == null && isPlaying) {
            isPlaying = false
            fullscreenState.value = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            playerManager.stopPlayback()
            isPlaying = false
            fullscreenState.value = false
            viewModel.clearMovieInfo()
        }
    }

    if (isFullscreen && userProfile != null) {
        // Fullscreen player in landscape mode
        FullscreenPlayer(
            streamUrl = StreamUrlBuilder.buildVodStreamUrl(userProfile!!, movie),
            title = movie.name,
            playerType = PlayerType.MOVIE,
            playerManager = playerManager,
            onBack = {
                // guardar posición antes de salir
                lastPositionMs = playerManager.getCurrentPosition()
                fullscreenState.value = false
                // Mantener isPlaying = true
            }
        )
    } else {
        Scaffold(
            topBar = {
                if (!isInPip) {
                    TopAppBar(
                        title = {
                            Text(
                                text = movie.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                playerManager.stopPlayback()
                                isPlaying = false
                                fullscreenState.value = false
                                onBackClick()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                        },
                        actions = {
                            val isFavorite = uiState.favoriteIds.contains(movie.streamId)
                            IconButton(onClick = {
                                viewModel.toggleFavorite(movie.streamId)
                                val message = if (isFavorite) "Quitado de favoritos" else "Añadido a favoritos"
                                snackbarScope.launch { snackbarHostState.showSnackbar(message) }
                            }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos"
                                )
                            }
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Mini player when movie is playing
                if (isPlaying && userProfile != null) {
                    MovieMiniPlayer(
                        streamUrl = StreamUrlBuilder.buildVodStreamUrl(userProfile!!, movie),
                        movieTitle = movie.name,
                        playerManager = playerManager,
                        onClose = {
                            playerManager.stopPlayback()
                            // No liberar singleton
                            isPlaying = false
                            fullscreenState.value = false
                        },
                        onFullscreen = { fullscreenState.value = true }
                    )

                    // Guardar progreso periódicamente cada 10 segundos
                    LaunchedEffect(isPlaying) {
                        while (isPlaying) {
                            delay(10000) // 10 segundos
                            val currentPos = playerManager.getCurrentPosition()
                            val totalDuration = playerManager.getDuration()
                            if (currentPos > 0 && totalDuration > 0) {
                                viewModel.saveMovieProgress(movie.streamId, currentPos, totalDuration)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Poster Image
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        }
                    }

                    // Movie Info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Loading indicator
                        if (uiState.isLoadingMovieInfo) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        }

                        val movieInfo = uiState.selectedMovieInfo

                        // Title
                        Text(
                            text = movieInfo?.name ?: movie.name,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Original title (if different)
                        if (movieInfo?.originalName != null && movieInfo.originalName != movieInfo.name) {
                            Text(
                                text = movieInfo.originalName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Rating
                        val rating = movieInfo?.rating5Based ?: movie.rating5Based.toDouble()
                        if (rating > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⭐",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = String.format("%.1f / 5.0", rating),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Release Date
                        if (movieInfo?.releaseDate != null) {
                            InfoRow(label = "Estreno", value = movieInfo.releaseDate)
                        } else if (!movie.added.isNullOrEmpty()) {
                            InfoRow(label = "Agregada", value = movie.added)
                        }

                        // Duration
                        if (movieInfo?.duration != null) {
                            InfoRow(label = "Duración", value = movieInfo.duration)
                        }

                        // Genre
                        if (movieInfo?.genre != null) {
                            InfoRow(label = "Género", value = movieInfo.genre)
                        }

                        // Country
                        if (movieInfo?.country != null) {
                            InfoRow(label = "País", value = movieInfo.country)
                        }

                        // Director
                        if (movieInfo?.director != null) {
                            InfoRow(label = "Director", value = movieInfo.director)
                        }

                        // Cast/Actors
                        val cast = movieInfo?.cast ?: movieInfo?.actors
                        if (cast != null) {
                            InfoRow(label = "Reparto", value = cast)
                        }

                        // Age Rating
                        if (movieInfo?.mpaaRating != null) {
                            InfoRow(label = "Clasificación", value = movieInfo.mpaaRating)
                        } else if (movieInfo?.age != null) {
                            InfoRow(label = "Edad", value = "${movieInfo.age}+")
                        }

                        // Plot/Description
                        val description = movieInfo?.plot ?: movieInfo?.description
                        if (description != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Sinopsis",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Container Extension
                        InfoRow(label = "Formato", value = movie.containerExtension.uppercase())

                        // Video/Audio info
                        if (movieInfo?.video != null || movieInfo?.audio != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (movieInfo.video != null) {
                                    InfoRow(label = "Video", value = movieInfo.video)
                                }
                                if (movieInfo.audio != null) {
                                    InfoRow(label = "Audio", value = movieInfo.audio)
                                }
                            }
                        }

                        // TMDB ID
                        if (!movie.tmdbId.isNullOrEmpty()) {
                            InfoRow(label = "TMDB ID", value = movie.tmdbId)
                        }

                        // Play Button
                        Button(
                            onClick = {
                                // Verificar si hay progreso guardado
                                val progress = uiState.movieProgress
                                if (progress != null && progress.positionMs > 0 && progress.positionMs < progress.durationMs * 0.95) {
                                    // Mostrar diálogo para continuar o empezar desde el inicio
                                    showResumeDialog = true
                                } else {
                                    // Reproducir desde el inicio
                                    playerManager.stopPlayback()
                                    onPlayClick(movie)
                                    isPlaying = true
                                    viewModel.onMoviePlayed(movie)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reproducir",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo para continuar o empezar desde el inicio
    if (showResumeDialog) {
        AlertDialog(
            onDismissRequest = { showResumeDialog = false },
            title = { Text("Continuar reproducción") },
            text = {
                Text("¿Deseas continuar donde lo dejaste o empezar desde el inicio?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        // Continuar donde se quedó
                        val progress = uiState.movieProgress
                        if (progress != null) {
                            lastPositionMs = progress.positionMs
                        }
                        playerManager.stopPlayback()
                        onPlayClick(movie)
                        isPlaying = true
                        viewModel.onMoviePlayed(movie)
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResumeDialog = false
                        // Empezar desde el inicio
                        viewModel.deleteMovieProgress(movie.streamId)
                        lastPositionMs = 0L
                        playerManager.stopPlayback()
                        onPlayClick(movie)
                        isPlaying = true
                        viewModel.onMoviePlayed(movie)
                    }
                ) {
                    Text("Desde el inicio")
                }
            }
        )
    }

    // Al volver de fullscreen, restaurar posición si corresponde
    LaunchedEffect(lastPositionMs, isPlaying) {
        if (lastPositionMs > 0L && isPlaying) {
            playerManager.seekTo(lastPositionMs)
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
