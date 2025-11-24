package com.iptv.playxy.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iptv.playxy.domain.VodStream
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.MovieMiniPlayer
import com.iptv.playxy.ui.player.PlayerSurface
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.util.StreamUrlBuilder
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    movie: VodStream,
    onBackClick: () -> Unit,
    viewModel: MoviesViewModel = hiltViewModel()
) {
    var isPlaying by remember { mutableStateOf(false) }
    val userProfile by viewModel.userProfile.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var lastPositionMs by remember { mutableLongStateOf(0L) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var currentPlaybackPosition by remember { mutableLongStateOf(0L) }
    var currentPlaybackDuration by remember { mutableLongStateOf(0L) }

    // Load movie details when screen opens
    LaunchedEffect(movie.streamId) {
        viewModel.loadMovieInfo(movie.streamId)
    }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    val pipController = LocalPipController.current
    val isInPip by pipController.isInPip.collectAsStateWithLifecycle()

    val shouldShowHeaderPlayer = isPlaying && userProfile != null

    DisposableEffect(isPlaying, isFullscreen) {
        playerManager.setTransportActions(null, null)
        onDispose { playerManager.setTransportActions(null, null) }
    }

    LaunchedEffect(playbackState.streamUrl) {
        if (!playerManager.hasActivePlayback() && isPlaying && !isInPip) {
            // Guardar progreso cuando se detiene la reproducción
            val currentPos = playerManager.getCurrentPosition()
            val duration = playerManager.getDuration()
            if (currentPos > 0 && duration > 0) {
                viewModel.saveMovieProgress(movie.streamId, currentPos, duration)
            }
            isPlaying = false
            fullscreenState.value = false
        }
    }

    // Ensure playback continues when returning from fullscreen to mini player
    LaunchedEffect(isFullscreen, isPlaying) {
        if (!isFullscreen && isPlaying) {
            playerManager.play()
        }
    }

    // Actualizar posición de reproducción cada segundo para la barra de progreso
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                kotlinx.coroutines.delay(1000) // Actualizar cada segundo
                currentPlaybackPosition = playerManager.getCurrentPosition()
                currentPlaybackDuration = playerManager.getDuration()
            }
        }
    }

    // Guardar progreso periódicamente mientras se reproduce
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                kotlinx.coroutines.delay(10000) // Guardar cada 10 segundos
                val currentPos = playerManager.getCurrentPosition()
                val duration = playerManager.getDuration()
                if (currentPos > 0 && duration > 0) {
                    viewModel.saveMovieProgress(movie.streamId, currentPos, duration)
                }
            }
        }
    }

    // Restaurar posición cuando hay lastPositionMs y el reproductor está listo
    LaunchedEffect(lastPositionMs, isPlaying, playbackState.firstFrameRendered) {
        if (lastPositionMs > 0L && isPlaying && playbackState.firstFrameRendered) {
            val currentPos = playerManager.getCurrentPosition()
            if (abs(currentPos - lastPositionMs) > 750L) {
                kotlinx.coroutines.delay(500) // Esperar un poco para que el player esté completamente listo
                playerManager.seekTo(lastPositionMs)
            }
            lastPositionMs = 0L // Reset para evitar múltiples seeks
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Guardar progreso antes de salir de la pantalla
            if (isPlaying) {
                val currentPos = playerManager.getCurrentPosition()
                val duration = playerManager.getDuration()
                if (currentPos > 0 && duration > 0) {
                    viewModel.saveMovieProgress(movie.streamId, currentPos, duration)
                }
            }
            playerManager.stopPlayback()
            isPlaying = false
            fullscreenState.value = false
            viewModel.clearMovieInfo()
        }
    }

    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    val currentStreamUrl = remember(movie, userProfile) {
        if (userProfile != null) {
            StreamUrlBuilder.buildVodStreamUrl(userProfile!!, movie)
        } else null
    }

    if (isFullscreen && userProfile != null && currentStreamUrl != null) {
        FullscreenPlayer(
            streamUrl = currentStreamUrl,
            title = movie.name,
            playerType = PlayerType.MOVIE,
            playerManager = playerManager,
            onBack = { fullscreenState.value = false }
        )
    } else {
        var synopsisExpanded by remember { mutableStateOf(false) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    if (shouldShowHeaderPlayer && currentStreamUrl != null) {
                        MovieMiniPlayer(
                            streamUrl = currentStreamUrl,
                            movieTitle = movie.name,
                            playerManager = playerManager,
                            onClose = {
                                playerManager.stopPlayback()
                                isPlaying = false
                                fullscreenState.value = false
                            },
                            modifier = Modifier.fillMaxSize(),
                            onFullscreen = {
                                fullscreenState.value = true
                            }
                        )
                    } else {
                        AsyncImage(
                            model = movie.streamIcon,
                            contentDescription = movie.name,
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
                                fullscreenState.value = false
                                onBackClick()
                            },
                            modifier = Modifier
                                .padding(12.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título y favorito
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
                                text = movie.name,
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
                                // Estrellas
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(5) { index ->
                                        val filled = movie.rating5Based >= index + 1
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                // Duración
                                Text(
                                    text = uiState.selectedMovieInfo?.duration ?: "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                // Fecha
                                if (!uiState.selectedMovieInfo?.releaseDate.isNullOrEmpty() || !movie.added.isNullOrEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = uiState.selectedMovieInfo?.releaseDate ?: movie.added.orEmpty(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Icono de favorito
                        val isFavorite = uiState.favoriteIds.contains(movie.streamId)
                        IconButton(
                            onClick = {
                                viewModel.toggleFavorite(movie.streamId)
                                val msg = if (isFavorite) "Quitado de favoritos" else "Añadido a favoritos"
                                snackbarScope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) Color(0xFFFF0000) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Botón de reproducción y barra de progreso
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        val progress = uiState.movieProgress
                        val savedProgress = progress != null && progress.positionMs > 0 && progress.positionMs < progress.durationMs * 0.95
                        val currentProgress = isPlaying && currentPlaybackPosition > 0 && currentPlaybackDuration > 0
                        val hasProgress = savedProgress || currentProgress
                        
                        // Usar posición en tiempo real si está reproduciendo, sino usar progreso guardado
                        val displayPosition = if (isPlaying && currentPlaybackDuration > 0) currentPlaybackPosition else (progress?.positionMs ?: 0L)
                        val displayDuration = if (isPlaying && currentPlaybackDuration > 0) currentPlaybackDuration else (progress?.durationMs ?: 1L)
                        val showProgressBar = displayPosition > 0 && displayDuration > 0
                        
                        Button(
                            onClick = {
                                if (hasProgress) {
                                    // Usar progreso en tiempo real si está disponible, sino usar progreso guardado
                                    lastPositionMs = if (currentPlaybackPosition > 0) currentPlaybackPosition else (progress?.positionMs ?: 0L)
                                }
                                if (userProfile != null && currentStreamUrl != null) {
                                    playerManager.playMedia(currentStreamUrl, PlayerType.MOVIE, forcePrepare = true)
                                    isPlaying = true
                                    viewModel.onMoviePlayed(movie)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasProgress) "Reanudar" else "Ver ahora",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Barra de progreso pegada al botón
                        if (showProgressBar) {
                            val progressPercentage = (displayPosition.toFloat() / displayDuration.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressPercentage)
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.secondary,
                                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = if (progressPercentage >= 0.99f) 8.dp else 0.dp)
                                        )
                                )
                            }
                        } else {
                            // Espacio vacío cuando no hay progreso para mantener forma redondeada
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.dp)
                            )
                        }
                    }

                    val movieInfo = uiState.selectedMovieInfo
                    val description = movieInfo?.plot ?: movieInfo?.description
                    if (!description.isNullOrEmpty()) {
                        Column {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                onClick = { synopsisExpanded = !synopsisExpanded },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text(if (synopsisExpanded) "Ver menos" else "Ver más")
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoRow(label = "Género: ", value = movieInfo?.genre ?: "—")
                        InfoRow(label = "Director: ", value = movieInfo?.director ?: "—")
                    }

                    if (!movieInfo?.cast.isNullOrEmpty()) {
                        Text(
                            text = "Reparto & Equipo",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        val castList = movieInfo?.cast?.split(",")?.map { it.trim() } ?: emptyList()
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                        ) {
                            items(castList) { actorName: String ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(80.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.align(Alignment.Center),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = actorName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
