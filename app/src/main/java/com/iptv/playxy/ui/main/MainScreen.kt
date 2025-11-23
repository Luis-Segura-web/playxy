package com.iptv.playxy.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.MainDestination
import com.iptv.playxy.ui.tv.TVScreen
import com.iptv.playxy.ui.movies.MoviesScreen
import com.iptv.playxy.ui.series.SeriesScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToLoading: () -> Unit,
    onNavigateToMovieDetail: (String, String) -> Unit,
    onNavigateToSeriesDetail: (String, String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value
    val playerManager = LocalPlayerManager.current
    val pipController = LocalPipController.current
    val hideUiForPip by pipController.hidePlayerUi.collectAsStateWithLifecycle()

    LaunchedEffect(state.isLoggingOut) {
        if (state.isLoggingOut) {
            onNavigateToLogin()
        }
    }
    
    val gradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    var sortMenuExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            if (!isFullscreen && !hideUiForPip) {
                // Show search bar as TopBar when searching is active
                if (state.isSearching && state.currentDestination in listOf(
                        MainDestination.TV,
                        MainDestination.MOVIES,
                        MainDestination.SERIES
                    )
                ) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = when (state.currentDestination) {
                                            MainDestination.TV -> "Buscar canales..."
                                            MainDestination.MOVIES -> "Buscar películas..."
                                            MainDestination.SERIES -> "Buscar series..."
                                            else -> "Buscar..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar búsqueda"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.onSearchActiveChange(false) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Cerrar búsqueda"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                } else {
                    // Normal TopBar
                    TopAppBar(
                    title = {
                        val subtitle = when (state.currentDestination) {
                            MainDestination.TV -> "${state.liveStreamCount} canales"
                            MainDestination.MOVIES -> "${state.vodStreamCount} películas"
                            MainDestination.SERIES -> "${state.seriesCount} series"
                            else -> ""
                        }
                        val lastUpdateTime = when (state.currentDestination) {
                            MainDestination.TV -> state.lastLiveUpdateTime
                            MainDestination.MOVIES -> state.lastVodUpdateTime
                            MainDestination.SERIES -> state.lastSeriesUpdateTime
                            else -> 0L
                        }
                        val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
                        val lastUpdateText = if (lastUpdateTime > 0) {
                            dateFormat.format(Date(lastUpdateTime))
                        } else {
                            "Sin sincronizar"
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = state.currentDestination.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    text = "$subtitle · $lastUpdateText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Show search, sort, and reload only for TV, Movies, and Series tabs
                        if (state.currentDestination in listOf(
                                MainDestination.TV,
                                MainDestination.MOVIES,
                                MainDestination.SERIES
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Search icon
                                IconButton(onClick = {
                                    viewModel.onSearchActiveChange(!state.isSearching)
                                }) {
                                    Icon(
                                        imageVector = if (state.isSearching) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = if (state.isSearching) "Cerrar búsqueda" else "Buscar",
                                        tint = if (state.isSearching) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Sort icon with dropdown menu
                                Box {
                                    IconButton(onClick = { sortMenuExpanded = true }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = "Ordenar",
                                            tint = if (state.sortOrder != SortOrder.DEFAULT) 
                                                MaterialTheme.colorScheme.secondary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Por defecto") },
                                            onClick = {
                                                viewModel.onSortOrderChange(SortOrder.DEFAULT)
                                                sortMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (state.sortOrder == SortOrder.DEFAULT) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("A-Z") },
                                            onClick = {
                                                viewModel.onSortOrderChange(SortOrder.A_TO_Z)
                                                sortMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (state.sortOrder == SortOrder.A_TO_Z) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Z-A") },
                                            onClick = {
                                                viewModel.onSortOrderChange(SortOrder.Z_TO_A)
                                                sortMenuExpanded = false
                                            },
                                            leadingIcon = {
                                                if (state.sortOrder == SortOrder.Z_TO_A) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        )
                                        
                                        // Date sorting only for Movies and Series
                                        if (state.currentDestination in listOf(
                                                MainDestination.MOVIES,
                                                MainDestination.SERIES
                                            )
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Más recientes") },
                                                onClick = {
                                                    viewModel.onSortOrderChange(SortOrder.DATE_NEWEST)
                                                    sortMenuExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (state.sortOrder == SortOrder.DATE_NEWEST) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Más antiguas") },
                                                onClick = {
                                                    viewModel.onSortOrderChange(SortOrder.DATE_OLDEST)
                                                    sortMenuExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (state.sortOrder == SortOrder.DATE_OLDEST) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.secondary
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // Reload icon
                                IconButton(
                                    onClick = { viewModel.onReload() },
                                    enabled = !state.isReloading
                                ) {
                                    if (state.isReloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Recargar",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                )
                }
            }
        },
        bottomBar = {
            if (!isFullscreen && !hideUiForPip) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp
                ) {
                    MainDestination.entries.forEach { destination ->
                        val selected = state.currentDestination == destination
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (destination) {
                                        MainDestination.HOME -> Icons.Default.Home
                                        MainDestination.TV -> Icons.Default.Tv
                                        MainDestination.MOVIES -> Icons.Default.Movie
                                        MainDestination.SERIES -> Icons.Default.VideoLibrary
                                        MainDestination.SETTINGS -> Icons.Default.Settings
                                    },
                                    contentDescription = destination.title
                                )
                            },
                            label = { Text(destination.title) },
                            selected = selected,
                            onClick = {
                                if (destination == state.currentDestination) return@NavigationBarItem
                                val leavingTv = state.currentDestination == MainDestination.TV && destination != MainDestination.TV
                                if (leavingTv) {
                                    playerManager.stopPlayback()
                                }
                                viewModel.onDestinationChange(destination)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .then(
                    if (!isFullscreen && !hideUiForPip) {
                        Modifier
                            .padding(paddingValues)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    } else if (!isFullscreen && hideUiForPip) {
                        // Evita padding del scaffold cuando se oculta la UI por PiP
                        Modifier
                    } else {
                        Modifier
                    }
                )
        ) {
            when (state.currentDestination) {
                MainDestination.HOME -> HomeContent(state)
                MainDestination.TV -> TVScreen(
                    searchQuery = state.debouncedSearchQuery,
                    sortOrder = state.sortOrder
                )
                MainDestination.MOVIES -> MoviesScreen(
                    searchQuery = state.debouncedSearchQuery,
                    sortOrder = state.sortOrder,
                    onMovieClick = { movie ->
                        onNavigateToMovieDetail(movie.streamId, movie.categoryId)
                    }
                )
                MainDestination.SERIES -> SeriesScreen(
                    searchQuery = state.debouncedSearchQuery,
                    sortOrder = state.sortOrder,
                    onSeriesClick = { series ->
                        onNavigateToSeriesDetail(series.seriesId, series.categoryId)
                    }
                )
                MainDestination.SETTINGS -> SettingsContent(
                    onLogout = viewModel::onLogout,
                    onForceReload = {
                        viewModel.onForceReload()
                        onNavigateToLoading()
                    }
                )
            }

            if (state.isReloading) {
                val reloadMessage = when (state.currentDestination) {
                    MainDestination.TV -> "Recargando canales de TV..."
                    MainDestination.MOVIES -> "Recargando películas..."
                    MainDestination.SERIES -> "Recargando series..."
                    else -> "Actualizando contenido..."
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 0.dp,
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp
                            )
                            Column {
                                Text(
                                    text = reloadMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sincronizando con el proveedor",
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
}

@Composable
fun HomeContent(state: MainState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Panel de inicio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 3.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Reproduce y navega sin ruido",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "TV en vivo, películas y series en un tablero limpio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        StatsCard(
            title = "Canales de TV",
            count = state.liveStreamCount,
            icon = Icons.Default.Tv
        )
        
        StatsCard(
            title = "Películas",
            count = state.vodStreamCount,
            icon = Icons.Default.Movie
        )
        
        StatsCard(
            title = "Series",
            count = state.seriesCount,
            icon = Icons.Default.VideoLibrary
        )
    }
}

@Composable
fun StatsCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.large
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun UnderConstructionContent(section: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "En construcción",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$section - En Construcción",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Esta sección estará disponible próximamente",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsContent(
    onLogout: () -> Unit,
    onForceReload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sincronización de contenido",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Fuerza la recarga si notas datos desactualizados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onForceReload,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recargar"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forzar recarga de contenido")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ) {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Cerrar sesión"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}
