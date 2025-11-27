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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    onNavigateToMovie: (String, String) -> Unit = { _, _ -> },
    onNavigateToActor: (com.iptv.playxy.domain.TmdbCast) -> Unit = {},
    showHomeButton: Boolean = false,
    onNavigateHome: () -> Unit = {},
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
    var showPinDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var accessGranted by remember { mutableStateOf(false) }
    var gatePin by remember { mutableStateOf("") }
    var recentRegistered by remember { mutableStateOf(false) }
    var unavailableMovieInfo by remember { mutableStateOf<com.iptv.playxy.domain.TmdbMovieLink?>(null) }
    val tmdbEnabled = uiState.tmdbEnabled

    // Load movie details when screen opens
    LaunchedEffect(movie.streamId) {
        gatePin = ""
        pinError = null
        accessGranted = false
        recentRegistered = false
        val restricted = viewModel.requiresPinForCategory(movie.categoryId)
        if (restricted) {
            showPinDialog = true
        } else {
            showPinDialog = false
            accessGranted = true
        }
    }

    LaunchedEffect(movie.streamId, accessGranted) {
        if (accessGranted) {
            viewModel.loadMovieInfo(movie.streamId)
        }
    }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    val pipController = LocalPipController.current
    val isInPip by pipController.isInPip.collectAsStateWithLifecycle()

    val shouldShowHeaderPlayer = isPlaying && userProfile != null
    val movieInfo = uiState.selectedMovieInfo
    val headerImageUrl = movieInfo?.backdropPath?.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: movieInfo?.movieImage
        ?: movie.streamIcon
    val displayTitle = movieInfo?.name?.takeIf { it.isNotBlank() } ?: movie.name
    val ratingValue = movieInfo?.rating5Based?.toFloat()?.takeIf { it > 0f } ?: movie.rating5Based

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

    LaunchedEffect(playbackState.streamUrl) {
        if (playbackState.streamUrl != null && !recentRegistered && accessGranted) {
            viewModel.onMoviePlayed(movie)
            recentRegistered = true
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

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { onBackClick() },
            title = { Text("PIN requerido") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ingresa el PIN para acceder a este contenido.",
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
                        val valid = viewModel.validateParentalPin(sanitized)
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
            dismissButton = {
                TextButton(onClick = { onBackClick() }) { Text("Cancelar") }
            }
        )
    }

    if (!accessGranted) {
        Box(modifier = Modifier.fillMaxSize())
    } else if (isFullscreen && userProfile != null && currentStreamUrl != null) {
        FullscreenPlayer(
            streamUrl = currentStreamUrl,
            title = displayTitle,
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
                            movieTitle = displayTitle,
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
                            model = headerImageUrl,
                            contentDescription = displayTitle,
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
                        if (showHomeButton) {
                            IconButton(
                                onClick = onNavigateHome,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Inicio",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
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
                                text = displayTitle,
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
                                        val filled = ratingValue >= index + 1
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

                    val tmdbEnabled = uiState.tmdbEnabled
                    if (tmdbEnabled && !movieInfo?.tmdbCast.isNullOrEmpty()) {
                        Text(
                            text = "Reparto & Equipo",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                        ) {
                            items(movieInfo?.tmdbCast ?: emptyList()) { cast ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(90.dp)
                                        .clickable {
                                            onNavigateToActor(cast)
                                        }
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
                                    Spacer(modifier = Modifier.height(8.dp))
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
                    } else if (!movieInfo?.cast.isNullOrEmpty()) {
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

                    val collectionItems = movieInfo?.tmdbCollection ?: emptyList()
                    if (tmdbEnabled && collectionItems.size > 1) {
                        Text(
                            text = "Colección relacionada",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(collectionItems) { item ->
                                CollectionCard(
                                    item = item,
                                    showUnavailableChip = item.availableStreamId == null,
                                    isCurrent = item.availableStreamId == movie.streamId,
                                    onClick = { link ->
                                        if (link.availableStreamId != null && link.availableCategoryId != null) {
                                            onNavigateToMovie(link.availableStreamId, link.availableCategoryId)
                                        } else {
                                            unavailableMovieInfo = link
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (tmdbEnabled && !movieInfo?.tmdbSimilar.isNullOrEmpty()) {
                        Text(
                            text = "Contenido similar",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(movieInfo?.tmdbSimilar ?: emptyList()) { item ->
                                CollectionCard(
                                    item = item,
                                    showUnavailableChip = false,
                                    onClick = { link ->
                                        if (link.availableStreamId != null && link.availableCategoryId != null) {
                                            onNavigateToMovie(link.availableStreamId, link.availableCategoryId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    unavailableMovieInfo?.let { info ->
        UnavailableMovieDialog(
            item = info,
            onDismiss = { unavailableMovieInfo = null }
        )
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

private fun sanitizePinInput(input: String): String = input.filter { it.isDigit() }.take(4)

@Composable
private fun CollectionCard(
    item: com.iptv.playxy.domain.TmdbMovieLink,
    showUnavailableChip: Boolean,
    isCurrent: Boolean = false,
    onClick: (com.iptv.playxy.domain.TmdbMovieLink) -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(item) },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.poster,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
            )
            if (isCurrent) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "Actual",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (showUnavailableChip && item.availableStreamId == null) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "No disponible",
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun UnavailableMovieDialog(
    item: com.iptv.playxy.domain.TmdbMovieLink,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = item.poster ?: item.backdrop,
                            contentDescription = item.tmdbTitle ?: item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_report_image),
                            placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "No disponible",
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
                                maxLines = 12,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!item.tmdbTitle.isNullOrBlank() && item.tmdbTitle != item.title) {
                            Text(
                                text = "Título TMDB: ${item.tmdbTitle}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
