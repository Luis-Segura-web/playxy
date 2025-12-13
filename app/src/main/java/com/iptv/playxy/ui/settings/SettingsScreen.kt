package com.iptv.playxy.ui.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptv.playxy.ui.main.sanitizePin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun ModernSettingsScreen(
    onLogout: () -> Unit,
    onForceReload: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showPinSetup by remember { mutableStateOf(false) }
    var showPinPromptFor by remember { mutableStateOf<PinPromptPurpose?>(null) }
    var showChangePin by remember { mutableStateOf(false) }
    var showHiddenCategories by remember { mutableStateOf(false) }
    var enableAfterPinSave by remember { mutableStateOf(false) }
    var postPinSuccessAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinPromptError by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val settingsEvents = viewModel.events

    LaunchedEffect(settingsEvents) {
        settingsEvents.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account Card rediseñado
            MinimalistAccountCard(
                name = uiState.profileName.ifBlank { uiState.username.ifBlank { "Usuario IPTV" } },
                username = uiState.username,
                server = uiState.serverUrl,
                expiry = uiState.expiry,
                connections = uiState.connections,
                status = uiState.status
            )

            // Contenido y Sincronización
            MinimalistSection(
                title = "Contenido",
                icon = Icons.Outlined.CloudSync
            ) {
                MinimalistSettingItem(
                    title = "Datos de TMDB",
                    subtitle = "Imágenes y metadatos de películas",
                    icon = Icons.Outlined.Image,
                    trailing = {
                        MinimalistSwitch(
                            checked = uiState.tmdbEnabled,
                            onCheckedChange = { viewModel.toggleTmdb(it) }
                        )
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                MinimalistSettingItem(
                    title = "Actualizar contenido",
                    subtitle = "Recargar desde el servidor",
                    icon = Icons.Outlined.Refresh,
                    onClick = onForceReload,
                    showArrow = true
                )
            }

            // Historial
            MinimalistSection(
                title = "Historial",
                icon = Icons.Outlined.History
            ) {
                var expanded by remember { mutableStateOf(false) }
                
                MinimalistSettingItem(
                    title = "Límite de recientes",
                    subtitle = "${uiState.recentsLimit} elementos",
                    icon = Icons.Outlined.Numbers,
                    onClick = { expanded = !expanded },
                    showArrow = true
                )
                
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.recentsLimitInput,
                            onValueChange = { viewModel.onRecentsLimitInputChange(it) },
                            label = { Text("Cantidad") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.saveRecentsLimit() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Guardar",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                var cleanExpanded by remember { mutableStateOf(false) }
                
                MinimalistSettingItem(
                    title = "Limpiar historial",
                    subtitle = "Eliminar elementos recientes",
                    icon = Icons.Outlined.DeleteSweep,
                    onClick = { cleanExpanded = !cleanExpanded },
                    showArrow = true
                )
                
                AnimatedVisibility(
                    visible = cleanExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimalistButton(
                            text = "Canales",
                            icon = Icons.Outlined.Tv,
                            onClick = { viewModel.clearRecentChannels() }
                        )
                        MinimalistButton(
                            text = "Películas",
                            icon = Icons.Outlined.Movie,
                            onClick = { viewModel.clearRecentMovies() }
                        )
                        MinimalistButton(
                            text = "Series",
                            icon = Icons.Outlined.Theaters,
                            onClick = { viewModel.clearRecentSeries() }
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        MinimalistButton(
                            text = "Limpiar todo",
                            icon = Icons.Outlined.DeleteForever,
                            onClick = { viewModel.clearAllRecents() },
                            danger = true
                        )
                    }
                }
            }

            // Control Parental
            MinimalistSection(
                title = "Seguridad",
                icon = Icons.Outlined.Shield
            ) {
                MinimalistSettingItem(
                    title = "Control parental",
                    subtitle = if (uiState.parentalEnabled) "Activo con PIN" else "Desactivado",
                    icon = if (uiState.parentalEnabled) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                    trailing = {
                        MinimalistSwitch(
                            checked = uiState.parentalEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    pinPromptError = null
                                    val hasPin = viewModel.hasPinConfigured()
                                    if (checked) {
                                        // Activar control parental
                                        if (!hasPin) {
                                            // Sin PIN → crear PIN primero, luego ir a categorías
                                            enableAfterPinSave = true
                                            postPinSuccessAction = { showHiddenCategories = true }
                                            showPinSetup = true
                                        } else {
                                            // Con PIN guardado → activar directamente e ir a categorías
                                            viewModel.setParentalEnabled(true)
                                            showHiddenCategories = true
                                            snackbarHostState.showSnackbar("Control parental activado")
                                        }
                                    } else {
                                        // Desactivar control parental → siempre pedir PIN
                                        if (!hasPin) {
                                            viewModel.setParentalEnabled(false)
                                        } else {
                                            postPinSuccessAction = {
                                                coroutineScope.launch {
                                                    viewModel.setParentalEnabled(false)
                                                    snackbarHostState.showSnackbar("Control parental desactivado")
                                                }
                                            }
                                            showPinPromptFor = PinPromptPurpose.DISABLE
                                        }
                                    }
                                }
                            }
                        )
                    }
                )

                AnimatedVisibility(
                    visible = uiState.parentalEnabled || uiState.isSaving,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        MinimalistSettingItem(
                            title = "Cambiar PIN",
                            subtitle = "Actualizar código de seguridad",
                            icon = Icons.Outlined.Key,
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
                            showArrow = true
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        MinimalistSettingItem(
                            title = "Categorías ocultas",
                            subtitle = "Gestionar contenido bloqueado",
                            icon = Icons.Outlined.VisibilityOff,
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
                            showArrow = true
                        )
                    }
                }

                if (uiState.isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logout minimalista
            MinimalistLogoutButton(
                onClick = { showLogoutDialog = true }
            )
        }
    }

    // Dialogs modernizados
    if (showLogoutDialog) {
        ModernLogoutDialog(
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            }
        )
    }

    if (showPinSetup) {
        ModernPinSetupDialog(
            onDismiss = {
                showPinSetup = false
                enableAfterPinSave = false
                postPinSuccessAction = null
            },
            onSave = { pin ->
                coroutineScope.launch {
                    viewModel.configurePin(pin, enableAfterPinSave)
                    snackbarHostState.showSnackbar("PIN configurado correctamente")
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
        ModernPinPromptDialog(
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
                        pinPromptError = "PIN incorrecto"
                    }
                }
            }
        )
    }

    if (showChangePin) {
        ModernChangePinDialog(
            onDismiss = { showChangePin = false },
            onSave = { currentPin, newPin ->
                val updated = viewModel.changePin(currentPin, newPin)
                if (updated) {
                    snackbarHostState.showSnackbar("PIN actualizado correctamente")
                    showChangePin = false
                }
                updated
            }
        )
    }

    if (showHiddenCategories) {
        ModernHiddenCategoriesScreen(
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
                    snackbarHostState.showSnackbar("Categorías actualizadas")
                }
            }
        )
    }
}

// ============================================
// COMPONENTES MINIMALISTAS
// ============================================

@Composable
private fun MinimalistHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Personaliza tu experiencia",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MinimalistAccountCard(
    name: String,
    username: String,
    server: String,
    expiry: String,
    connections: String,
    status: String
) {
    val statusColor = when (status.lowercase()) {
        "active" -> MaterialTheme.colorScheme.primary
        "expired", "banned", "disabled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(status = status, color = statusColor)
                    }
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MinimalistInfoChip(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Vencimiento",
                    value = expiry,
                    modifier = Modifier.weight(1f)
                )
                MinimalistInfoChip(
                    icon = Icons.Outlined.Link,
                    label = "Conexiones",
                    value = connections,
                    modifier = Modifier.weight(1f)
                )
            }
            
            if (server.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Servidor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = server,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun MinimalistInfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MinimalistSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun MinimalistSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showArrow: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (trailing != null) {
            trailing()
        } else if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MinimalistSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun MinimalistButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val containerColor = if (danger) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (danger) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun MinimalistLogoutButton(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cerrar sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Salir de tu cuenta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
            )
        }
    }
}

// ============================================
// DIÁLOGOS MODERNIZADOS
// ============================================

@Composable
private fun ModernLogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "¿Cerrar sesión?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "Se borrarán todos los datos locales y deberás iniciar sesión nuevamente.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Cerrar sesión",
                    modifier = Modifier.padding(vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModernPinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Configurar PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Crea un PIN de 4 dígitos para proteger el contenido",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Key, contentDescription = null)
                    }
                )
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                    },
                    isError = error != null
                )
                
                if (error != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sanitizedPin = sanitizePin(pin)
                    val sanitizedConfirm = sanitizePin(confirmPin)
                    pin = sanitizedPin
                    confirmPin = sanitizedConfirm
                    if (sanitizedPin.length != 4 || sanitizedConfirm.length != 4) {
                        error = "El PIN debe tener 4 dígitos"
                        return@Button
                    }
                    if (sanitizedPin != sanitizedConfirm) {
                        error = "Los PIN no coinciden"
                        return@Button
                    }
                    error = null
                    onSave(sanitizedPin)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModernPinPromptDialog(
    purpose: PinPromptPurpose,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    
    val title = when (purpose) {
        PinPromptPurpose.DISABLE -> "Desactivar control parental"
        PinPromptPurpose.OPEN_CATEGORIES -> "Verificación requerida"
    }
    
    val description = when (purpose) {
        PinPromptPurpose.DISABLE -> "Ingresa tu PIN para desactivar la protección"
        PinPromptPurpose.OPEN_CATEGORIES -> "Ingresa tu PIN para continuar"
    }
    
    val combinedError = error ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Password,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Password, contentDescription = null)
                    },
                    isError = combinedError != null
                )
                
                if (combinedError != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = combinedError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sanitized = sanitizePin(pin)
                    pin = sanitized
                    if (sanitized.length != 4) {
                        localError = "El PIN debe tener 4 dígitos"
                        return@Button
                    }
                    localError = null
                    onConfirm(sanitized)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirmar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ModernChangePinDialog(
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
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Cambiar PIN",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Ingresa tu PIN actual y el nuevo PIN",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = sanitizePin(it) },
                    label = { Text("PIN actual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                    }
                )
                
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Key, contentDescription = null)
                    }
                )
                
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                    },
                    isError = error != null
                )
                
                if (error != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sanitizedCurrent = sanitizePin(currentPin)
                    val sanitizedNew = sanitizePin(newPin)
                    val sanitizedConfirm = sanitizePin(confirmPin)
                    currentPin = sanitizedCurrent
                    newPin = sanitizedNew
                    confirmPin = sanitizedConfirm

                    if (sanitizedCurrent.length != 4 || sanitizedNew.length != 4 || sanitizedConfirm.length != 4) {
                        error = "Todos los PIN deben tener 4 dígitos"
                        return@Button
                    }
                    if (sanitizedNew == sanitizedCurrent) {
                        error = "El nuevo PIN debe ser diferente"
                        return@Button
                    }
                    if (sanitizedNew != sanitizedConfirm) {
                        error = "Los nuevos PIN no coinciden"
                        return@Button
                    }
                    scope.launch {
                        val success = onSave(sanitizedCurrent, sanitizedNew)
                        if (!success) {
                            error = "PIN actual incorrecto"
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Actualizar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
internal fun ModernHiddenCategoriesScreen(
    liveCategories: List<com.iptv.playxy.domain.Category>,
    vodCategories: List<com.iptv.playxy.domain.Category>,
    seriesCategories: List<com.iptv.playxy.domain.Category>,
    initialLive: Set<String>,
    initialVod: Set<String>,
    initialSeries: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>, Set<String>, Set<String>) -> Unit
) {
    // Llamar a la versión pública de HiddenCategoriesScreen
    HiddenCategoriesScreen(
        liveCategories = liveCategories,
        vodCategories = vodCategories,
        seriesCategories = seriesCategories,
        initialLive = initialLive,
        initialVod = initialVod,
        initialSeries = initialSeries,
        onDismiss = onDismiss,
        onSave = onSave
    )
}

private enum class PinPromptPurpose {
    DISABLE,
    OPEN_CATEGORIES
}
