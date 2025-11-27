package com.iptv.playxy.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptv.playxy.ui.main.sanitizePin
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountCard(
                name = uiState.profileName.ifBlank { uiState.username.ifBlank { "Usuario IPTV" } },
                username = uiState.username,
                server = uiState.serverUrl,
                expiry = uiState.expiry,
                connections = uiState.connections,
                status = uiState.status
            )

            // Content & Sync Section
            SettingsSection(
                title = "Contenido y Sincronización",
                icon = Icons.Default.CloudSync
            ) {
                SettingCard(
                    title = "Datos de TMDB",
                    description = "Obtener imágenes y metadatos de películas y series desde TMDB",
                    icon = Icons.Default.Image,
                    trailing = {
                        Switch(
                            checked = uiState.tmdbEnabled,
                            onCheckedChange = { viewModel.toggleTmdb(it) }
                        )
                    }
                )

                SettingCard(
                    title = "Forzar recarga de contenido",
                    description = "Actualizar todo el contenido desde el servidor",
                    icon = Icons.Default.Refresh,
                    onClick = onForceReload
                )
            }

            // Recent History Section
            SettingsSection(
                title = "Historial Reciente",
                icon = Icons.Default.History
            ) {
                SettingCard(
                    title = "Límite de elementos recientes",
                    description = "Cantidad máxima de elementos en el historial",
                    icon = Icons.Default.Numbers
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.recentsLimitInput,
                            onValueChange = { viewModel.onRecentsLimitInputChange(it) },
                            label = { Text("Límite") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.saveRecentsLimit() }) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Guardar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }

                SettingCard(
                    title = "Limpiar historial",
                    description = "Eliminar elementos del historial reciente",
                    icon = Icons.Default.DeleteSweep
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        ClearHistoryButton(
                            text = "Limpiar canales recientes",
                            onClick = { viewModel.clearRecentChannels() }
                        )
                        ClearHistoryButton(
                            text = "Limpiar películas recientes",
                            onClick = { viewModel.clearRecentMovies() }
                        )
                        ClearHistoryButton(
                            text = "Limpiar series recientes",
                            onClick = { viewModel.clearRecentSeries() }
                        )
                        OutlinedButton(
                            onClick = { viewModel.clearAllRecents() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                            )
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Limpiar todo el historial")
                        }
                    }
                }
            }

            // Parental Control Section
            SettingsSection(
                title = "Control Parental",
                icon = Icons.Default.ChildCare
            ) {
                SettingCard(
                    title = "Control parental",
                    description = if (uiState.parentalEnabled) {
                        "Activo - Protegido con PIN de 4 dígitos"
                    } else {
                        "Inactivo - Proteger contenido con PIN"
                    },
                    icon = if (uiState.parentalEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                    trailing = {
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
                                                    snackbarHostState.showSnackbar("Control parental activado")
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

                AnimatedVisibility(visible = uiState.parentalEnabled || uiState.isSaving) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingCard(
                            title = "Cambiar PIN",
                            description = "Actualizar el código de seguridad",
                            icon = Icons.Default.Key,
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
                            }
                        )

                        SettingCard(
                            title = "Categorías ocultas",
                            description = "Gestionar categorías bloqueadas por el control parental",
                            icon = Icons.Default.VisibilityOff,
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
                            }
                        )
                    }
                }

                if (uiState.isSaving) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cerrar sesión",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Salir de tu cuenta actual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Dialogs
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se borrarán todos los datos locales y deberás iniciar sesión nuevamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
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
                        pinPromptError = "PIN incorrecto"
                    }
                }
            }
        )
    }

    if (showChangePin) {
        ChangePinDialog(
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
        HiddenCategoriesScreen(
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

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

@Composable
private fun AccountCard(
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
        else -> MaterialTheme.colorScheme.secondary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    label = { Text(status.replaceFirstChar { it.uppercase() }) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (status.lowercase() == "active") Icons.Default.Verified else Icons.Default.Info,
                            contentDescription = null
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = statusColor.copy(alpha = 0.16f),
                        labelColor = statusColor,
                        leadingIconContentColor = statusColor
                    )
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoPill(
                    icon = Icons.Default.CalendarMonth,
                    label = "Vence",
                    value = expiry,
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    icon = Icons.Default.Lan,
                    label = "Conexiones",
                    value = connections,
                    modifier = Modifier.weight(1f)
                )
            }
            if (server.isNotBlank()) {
                SettingCard(
                    title = "Servidor",
                    description = server,
                    icon = Icons.Default.CloudQueue
                )
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (trailing != null) {
                    trailing()
                } else if (onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (content != null) {
                content()
            }
        }
    }
}

@Composable
private fun ClearHistoryButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
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
        icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null) },
        title = { Text("Configurar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Crea un PIN de 4 dígitos para proteger el contenido",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    error = "El PIN debe tener 4 dígitos"
                    return@Button
                }
                if (sanitizedPin != sanitizedConfirm) {
                    error = "Los PIN no coinciden"
                    return@Button
                }
                error = null
                onSave(sanitizedPin)
            }) {
                Text("Guardar")
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
        PinPromptPurpose.ENABLE -> "Activar control parental"
        PinPromptPurpose.DISABLE -> "Desactivar control parental"
        PinPromptPurpose.OPEN_CATEGORIES -> "Acceder a categorías"
    }
    val description = when (purpose) {
        PinPromptPurpose.ENABLE -> "Ingresa tu PIN para activar el control parental"
        PinPromptPurpose.DISABLE -> "Ingresa tu PIN para desactivar el control parental"
        PinPromptPurpose.OPEN_CATEGORIES -> "Ingresa tu PIN para gestionar las categorías ocultas"
    }
    val combinedError = error ?: localError

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Password, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = sanitizePin(it) },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    localError = "El PIN debe tener 4 dígitos"
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
        icon = { Icon(imageVector = Icons.Default.Key, contentDescription = null) },
        title = { Text("Cambiar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ingresa tu PIN actual y el nuevo PIN",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { currentPin = sanitizePin(it) },
                    label = { Text("PIN actual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = sanitizePin(it) },
                    label = { Text("Nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = sanitizePin(it) },
                    label = { Text("Confirmar nuevo PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
            }) {
                Text("Actualizar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private enum class PinPromptPurpose {
    ENABLE,
    DISABLE,
    OPEN_CATEGORIES
}
