package com.iptv.playxy.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptv.playxy.ui.MainDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToLoading: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(state.isLoggingOut) {
        if (state.isLoggingOut) {
            onNavigateToLogin()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.currentDestination.title) }
            )
        },
        bottomBar = {
            NavigationBar {
                MainDestination.values().forEach { destination ->
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
                        selected = state.currentDestination == destination,
                        onClick = { viewModel.onDestinationChange(destination) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state.currentDestination) {
                MainDestination.HOME -> HomeContent(state)
                MainDestination.TV -> UnderConstructionContent("TV")
                MainDestination.MOVIES -> UnderConstructionContent("Películas")
                MainDestination.SERIES -> UnderConstructionContent("Series")
                MainDestination.SETTINGS -> SettingsContent(
                    onLogout = viewModel::onLogout,
                    onForceReload = {
                        viewModel.onForceReload()
                        onNavigateToLoading()
                    }
                )
            }
        }
    }
}

@Composable
fun HomeContent(state: MainState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Estadísticas de Contenido",
            style = MaterialTheme.typography.headlineMedium
        )
        
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium
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
            Text("Forzar Recarga de Contenido")
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Cerrar sesión"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Cerrar Sesión")
        }
    }
}
