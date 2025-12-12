package com.iptv.playxy.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.iptv.playxy.domain.Category
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToProfiles: () -> Unit,
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
            onNavigateToProfiles()
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
                            when (state.currentDestination) {
                                MainDestination.HOME -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Descubre",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Contenido destacado de TMDB",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                MainDestination.SETTINGS -> {
                                    Text(
                                        text = "Ajustes",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> {
                                    val contentCount = when (state.currentDestination) {
                                        MainDestination.TV -> state.liveStreamCount
                                        MainDestination.MOVIES -> state.vodStreamCount
                                        MainDestination.SERIES -> state.seriesCount
                                        else -> 0
                                    }
                                    val contentLabel = when (state.currentDestination) {
                                        MainDestination.TV -> "Canales"
                                        MainDestination.MOVIES -> "Películas"
                                        MainDestination.SERIES -> "Series"
                                        else -> ""
                                    }
                                    val lastUpdateTime = when (state.currentDestination) {
                                        MainDestination.TV -> state.lastLiveUpdateTime
                                        MainDestination.MOVIES -> state.lastVodUpdateTime
                                        MainDestination.SERIES -> state.lastSeriesUpdateTime
                                        else -> 0L
                                    }
                                    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
                                    val lastUpdateText = if (lastUpdateTime > 0) {
                                        "Actualizado: ${dateFormat.format(Date(lastUpdateTime))}"
                                    } else {
                                        "Sin sincronizar"
                                    }
                                    val subtitle = if (contentCount > 0) {
                                        "$lastUpdateText • $contentCount $contentLabel"
                                    } else {
                                        lastUpdateText
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = state.currentDestination.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (subtitle.isNotEmpty()) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
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
                        val horizontal = if (state.currentDestination == MainDestination.TV) 0.dp else 14.dp
                        Modifier
                            .padding(paddingValues)
                            .padding(horizontal = horizontal, vertical = 10.dp)
                    } else if (!isFullscreen && hideUiForPip) {
                        // Evita padding del scaffold cuando se oculta la UI por PiP
                        Modifier
                    } else {
                        Modifier
                    }
                )
        ) {
            when (state.currentDestination) {
                MainDestination.HOME -> com.iptv.playxy.ui.home.HomeScreen(
                    onNavigateToMovie = onNavigateToMovieDetail,
                    onNavigateToSeries = onNavigateToSeriesDetail
                )
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
                MainDestination.SETTINGS -> com.iptv.playxy.ui.settings.ModernSettingsScreen(
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
fun HomeContent(
    state: MainState,
    viewModel: MainViewModel,
    onNavigateToMovie: (String, String) -> Unit,
    onNavigateToSeries: (String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var highlights by remember { mutableStateOf<HomeHighlights?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            highlights = viewModel.fetchHomeHighlights()
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Inicio",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        highlights?.let { data ->
            HighlightCarousel(
                title = "Películas destacadas",
                items = data.movies,
                onClick = { link ->
                    val streamId = link.availableStreamId
                    val categoryId = link.availableCategoryId
                    if (streamId != null && categoryId != null) {
                        onNavigateToMovie(streamId, categoryId)
                    }
                }
            )

            HighlightCarousel(
                title = "Series destacadas",
                items = data.series,
                onClick = { link ->
                    val seriesId = link.availableSeriesId
                    val categoryId = link.availableCategoryId
                    if (seriesId != null && categoryId != null) {
                        onNavigateToSeries(seriesId, categoryId)
                    }
                }
            )
        }
    }
}

@Composable
private fun HighlightCarousel(
    title: String,
    items: List<HomeLink>,
    onClick: (HomeLink) -> Unit
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                Surface(
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onClick(item) },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp
                ) {
                    Column {
                        AsyncImage(
                            model = item.poster,
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.year?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class HomeHighlights(
    val movies: List<HomeLink>,
    val series: List<HomeLink>
)

data class HomeLink(
    val title: String,
    val poster: String?,
    val year: String?,
    val availableStreamId: String? = null,
    val availableSeriesId: String? = null,
    val availableCategoryId: String? = null
)

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

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun SettingsContent(
    onLogout: () -> Unit,
    onForceReload: () -> Unit,
    viewModel: com.iptv.playxy.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val limits = listOf(3, 6, 9, 12, 18, 24)
    val scrollState = rememberScrollState()
    var limitsExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinPromptFor by remember { mutableStateOf<PinPromptPurpose?>(null) }
    var showChangePin by remember { mutableStateOf(false) }
    var showHiddenCategories by remember { mutableStateOf(false) }
    var enableAfterPinSave by remember { mutableStateOf(false) }
    var postPinSuccessAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinPromptError by remember { mutableStateOf<String?>(null) }
    val settingsEvents = viewModel.events

    Box(modifier = Modifier.fillMaxSize()) {
        LaunchedEffect(settingsEvents) {
            settingsEvents.collect { event ->
                when (event) {
                    is com.iptv.playxy.ui.settings.SettingsEvent.ShowMessage -> {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.profileName.ifBlank { "Perfil sin nombre" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Usuario: ${uiState.username.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Servidor: ${uiState.serverUrl.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Vence: ${uiState.expiry}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Conexiones: ${uiState.connections}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.reloadProfile() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recargar datos de cuenta")
                    }
                }
            }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Datos de TMDB", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Usa TMDB para completar portadas, backdrops y metadatos cuando haya ID TMDB.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.tmdbEnabled,
                            onCheckedChange = { enabled -> viewModel.toggleTmdb(enabled) }
                        )
                    }
                    Text(
                        text = "No se buscará por nombre; solo se usa cuando el proveedor envía el ID TMDB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
                        text = "Historial de recientes",
                        style = MaterialTheme.typography.titleMedium
                    )
                Text(
                    text = "Límite de elementos recientes (por defecto 15)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = uiState.recentsLimitInput,
                    onValueChange = { viewModel.onRecentsLimitInputChange(it) },
                    label = { Text("Límite de recientes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Button(onClick = { viewModel.saveRecentsLimit() }) {
                            Text("Guardar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                    Text(
                        text = "Limpiar recientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.clearRecentChannels() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Limpiar canales recientes")
                        }
                        Button(onClick = { viewModel.clearRecentMovies() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Limpiar películas recientes")
                        }
                        Button(onClick = { viewModel.clearRecentSeries() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Limpiar series recientes")
                        }
                        Button(onClick = { viewModel.clearAllRecents() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Limpiar todos los recientes")
                        }
                    }
                }
            }

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
                        text = "Control parental",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Activar/Desactivar Control Parental")
                            Text(
                                text = "El acceso se protege con PIN de 4 dígitos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.parentalEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    pinPromptError = null
                                    val hasPin = viewModel.hasPinConfigured()
                                    if (checked) {
                                        if (!hasPin) {
                                            enableAfterPinSave = true
                                            postPinSuccessAction = null
                                            showPinSetup = true
                                        } else {
                                            postPinSuccessAction = {
                                                coroutineScope.launch {
                                                    viewModel.setParentalEnabled(true)
                                                    snackbarHostState.showSnackbar("Control parental activado.")
                                                }
                                            }
                                            showPinPromptFor = PinPromptPurpose.ENABLE
                                        }
                                    } else {
                                        if (!hasPin) {
                                            viewModel.setParentalEnabled(false)
                                        } else {
                                            postPinSuccessAction = {
                                                coroutineScope.launch {
                                                    viewModel.setParentalEnabled(false)
                                                    snackbarHostState.showSnackbar("Control parental desactivado.")
                                                }
                                            }
                                            showPinPromptFor = PinPromptPurpose.DISABLE
                                        }
                                    }
                                }
                            }
                        )
                    }
                    Text(
                        text = if (uiState.parentalEnabled) "Control Parental: Activado" else "Control Parental: Desactivado",
                        color = if (uiState.parentalEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (viewModel.hasPinConfigured()) {
                                        showChangePin = true
                                    } else {
                                        enableAfterPinSave = uiState.parentalEnabled
                                        postPinSuccessAction = null
                                        showPinSetup = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cambiar PIN")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pinPromptError = null
                                    val hasPin = viewModel.hasPinConfigured()
                                    if (!hasPin) {
                                        enableAfterPinSave = uiState.parentalEnabled
                                        postPinSuccessAction = { showHiddenCategories = true }
                                        showPinSetup = true
                                    } else if (uiState.parentalEnabled) {
                                        postPinSuccessAction = { showHiddenCategories = true }
                                        showPinPromptFor = PinPromptPurpose.OPEN_CATEGORIES
                                    } else {
                                        showHiddenCategories = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Configurar Categorías Ocultas")
                        }
                    }
                    Text(
                        text = "El control parental restringe categorías de canales, películas y series.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.isSaving) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = {
                showPinSetup = false
                enableAfterPinSave = false
                postPinSuccessAction = null
            },
            onSave = { pin ->
                coroutineScope.launch {
                    viewModel.configurePin(pin, enableAfterPinSave)
                    snackbarHostState.showSnackbar("PIN configurado correctamente.")
                    val action = postPinSuccessAction
                    showPinSetup = false
                    enableAfterPinSave = false
                    postPinSuccessAction = null
                    action?.invoke()
                }
            }
        )
    }

    if (showPinPromptFor != null) {
        PinPromptDialog(
            purpose = showPinPromptFor!!,
            error = pinPromptError,
            onDismiss = {
                showPinPromptFor = null
                pinPromptError = null
                postPinSuccessAction = null
            },
            onConfirm = { pin ->
                coroutineScope.launch {
                    val isValid = viewModel.isPinValid(pin)
                    if (isValid) {
                        val action = postPinSuccessAction
                        pinPromptError = null
                        showPinPromptFor = null
                        postPinSuccessAction = null
                        action?.invoke()
                    } else {
                        pinPromptError = "PIN incorrecto."
                    }
                }
            }
        )
    }

    if (showChangePin) {
        ChangePinDialog(
            onDismiss = {
                showChangePin = false
            },
            onSave = { currentPin, newPin ->
                val updated = viewModel.changePin(currentPin, newPin)
                if (updated) {
                    snackbarHostState.showSnackbar("PIN actualizado correctamente.")
                    showChangePin = false
                }
                updated
            }
        )
    }

    if (showHiddenCategories) {
        com.iptv.playxy.ui.settings.HiddenCategoriesScreen(
            liveCategories = uiState.liveCategories,
            vodCategories = uiState.vodCategories,
            seriesCategories = uiState.seriesCategories,
            initialLive = uiState.blockedLive,
            initialVod = uiState.blockedVod,
            initialSeries = uiState.blockedSeries,
            onDismiss = { showHiddenCategories = false },
            onSave = { live, vod, series ->
                coroutineScope.launch {
                    viewModel.saveBlockedCategories(live, vod, series)
                    showHiddenCategories = false
                    snackbarHostState.showSnackbar("Categorías ocultas actualizadas.")
                }
            }
        )
    }
}

private enum class PinPromptPurpose {
    ENABLE,
    DISABLE,
    OPEN_CATEGORIES
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Define un PIN numérico de 4 dígitos y confírmalo para activar el control parental.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sanitizedPin = sanitizePin(pin)
                val sanitizedConfirm = sanitizePin(confirmPin)
                pin = sanitizedPin
                confirmPin = sanitizedConfirm
                if (sanitizedPin.length != 4 || sanitizedConfirm.length != 4) {
                    error = "Debes ingresar un PIN de 4 dígitos."
                    return@Button
                }
                if (sanitizedPin != sanitizedConfirm) {
                    error = "Los PIN ingresados no coinciden."
                    return@Button
                }
                error = null
                onSave(sanitizedPin)
            }) {
                Text("Guardar PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun PinPromptDialog(
    purpose: PinPromptPurpose,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val title = when (purpose) {
        PinPromptPurpose.ENABLE -> "Ingresar PIN"
        PinPromptPurpose.DISABLE -> "Confirmar desactivación"
        PinPromptPurpose.OPEN_CATEGORIES -> "Validar PIN"
    }
    val description = when (purpose) {
        PinPromptPurpose.ENABLE -> "Ingresa el PIN para activar el control parental."
        PinPromptPurpose.DISABLE -> "Ingresa el PIN para desactivar el control parental."
        PinPromptPurpose.OPEN_CATEGORIES -> "Ingresa el PIN para administrar las categorías ocultas."
    }
    val combinedError = error ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (combinedError != null) {
                    Text(
                        text = combinedError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sanitized = sanitizePin(pin)
                pin = sanitized
                if (sanitized.length != 4) {
                    localError = "Debes ingresar un PIN de 4 dígitos."
                    return@Button
                }
                localError = null
                onConfirm(sanitized)
            }) {
                Text("Confirmar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSave: suspend (String, String) -> Boolean
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ingresa tu PIN actual y define uno nuevo.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = sanitizePin(it) },
                    label = { Text("PIN actual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val sanitizedCurrent = sanitizePin(currentPin)
                val sanitizedNew = sanitizePin(newPin)
                val sanitizedConfirm = sanitizePin(confirmPin)
                currentPin = sanitizedCurrent
                newPin = sanitizedNew
                confirmPin = sanitizedConfirm

                if (sanitizedCurrent.length != 4 || sanitizedNew.length != 4 || sanitizedConfirm.length != 4) {
                    error = "Debes ingresar un PIN de 4 dígitos."
                    return@Button
                }
                if (sanitizedNew == sanitizedCurrent) {
                    error = "El nuevo PIN no puede ser igual al actual."
                    return@Button
                }
                if (sanitizedNew != sanitizedConfirm) {
                    error = "Los PIN ingresados no coinciden."
                    return@Button
                }

                scope.launch {
                    val updated = onSave(sanitizedCurrent, sanitizedNew)
                    if (!updated) {
                        error = "PIN incorrecto."
                    }
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

internal fun sanitizePin(input: String): String = input.filter { it.isDigit() }.take(4)
