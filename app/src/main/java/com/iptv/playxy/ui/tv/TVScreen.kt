
package com.iptv.playxy.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPipController
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.components.CategoryBar
import com.iptv.playxy.ui.main.SortOrder
import com.iptv.playxy.ui.player.FullscreenOverlay
import com.iptv.playxy.ui.player.FullscreenOrientationMode
import com.iptv.playxy.ui.player.ImmersiveMode
import com.iptv.playxy.ui.player.LocalPlayerContainerHost
import com.iptv.playxy.ui.player.PlayerContainerConfig
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.ui.player.TVMiniOverlay
import com.iptv.playxy.ui.player.TrackSelectionTab
import com.iptv.playxy.ui.player.TrackSelectionDialog
import com.iptv.playxy.ui.tv.components.ChannelListView
import com.iptv.playxy.util.StreamUrlBuilder
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@Composable
fun TVScreen(
    viewModel: TVViewModel = hiltViewModel(),
    searchQuery: String = "",
    sortOrder: SortOrder = SortOrder.DEFAULT
) {
    LaunchedEffect(searchQuery, sortOrder) { viewModel.updateFilters(searchQuery, sortOrder) }
    val currentChannel by viewModel.currentChannel.collectAsState()
    val categories by viewModel.uiState.collectAsState()
    val pagingFlow by viewModel.pagingFlow.collectAsState()
    val channelsPaging = pagingFlow.collectAsLazyPagingItems()
    val userProfile by viewModel.userProfile.collectAsState()
    val playerManager = LocalPlayerManager.current
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value
    val pipController = LocalPipController.current
    val playerContainer = LocalPlayerContainerHost.current
    
    // Navegación de canales
    val hasPrevious = viewModel.hasPreviousChannel()
    val hasNext = viewModel.hasNextChannel()

    // Actualizar la lista de canales cuando cambia el paging
    LaunchedEffect(channelsPaging.itemSnapshotList) {
        val items = channelsPaging.itemSnapshotList.items
        if (items.isNotEmpty()) {
            viewModel.updateChannelList(items)
        }
    }

    var isChannelSwitching by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingChannel by remember { mutableStateOf<LiveStream?>(null) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var trackDialogTab by remember { mutableStateOf<TrackSelectionTab?>(null) }
    var showFitDialog by remember { mutableStateOf(false) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var orientationMode by rememberSaveable { mutableStateOf(FullscreenOrientationMode.Auto) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(playbackState.streamUrl) {
        if (playbackState.streamUrl == null && currentChannel != null && !isChannelSwitching) {
            fullscreenState.value = false
        }
        if (playbackState.streamUrl != null) {
            isChannelSwitching = false
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pendingChannel = null
                pinInput = ""
                pinError = null
            },
            title = { Text("PIN requerido") },
            text = {
                Column {
                    Text("Ingresa el PIN para abrir esta categoría oculta.")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = sanitizePinInput(it) },
                        label = { Text("PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    if (pinError != null) {
                        Text(text = pinError!!, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val sanitized = sanitizePinInput(pinInput)
                    if (sanitized.length != 4) {
                        pinError = "Debes ingresar un PIN de 4 dígitos."
                        return@Button
                    }
                    coroutineScope.launch {
                        val valid = viewModel.validateParentalPin(sanitized)
                        if (valid) {
                            val channel = pendingChannel
                            showPinDialog = false
                            pinError = null
                            pendingChannel = null
                            if (channel != null) {
                                isChannelSwitching = true
                                viewModel.playChannel(channel)
                            }
                        } else {
                            pinError = "PIN incorrecto."
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false; pendingChannel = null; pinInput = "" }) { Text("Cancelar") } }
        )
    }

    val hasChannelContext = currentChannel != null && userProfile != null
    val streamUrl = if (hasChannelContext) {
        StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!)
    } else {
        null
    }

    val stopAndClose: () -> Unit = {
        isChannelSwitching = false
        playerManager.stopPlayback()
        fullscreenState.value = false
    }

    BackHandler(enabled = isFullscreen && hasChannelContext) { fullscreenState.value = false }
    ImmersiveMode(enabled = isFullscreen && hasChannelContext, orientationMode = orientationMode)
    DisposableEffect(isFullscreen, currentChannel?.streamId) {
        if (!(isFullscreen && hasChannelContext)) return@DisposableEffect onDispose {}
        playerManager.setTransportActions(
            onNext = {
                isChannelSwitching = true
                viewModel.playNextChannel()
            },
            onPrevious = {
                isChannelSwitching = true
                viewModel.playPreviousChannel()
            }
        )
        onDispose { playerManager.setTransportActions(null, null) }
    }

    val shouldShowPlayer =
        hasChannelContext && (isFullscreen || playbackState.streamUrl != null || isChannelSwitching)

    if (shouldShowPlayer && streamUrl != null) {
        LaunchedEffect(streamUrl) {
            if (playbackState.streamUrl != streamUrl) {
                playerManager.playMedia(streamUrl, PlayerType.TV)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (shouldShowPlayer && streamUrl != null) {
            playerContainer(
                PlayerContainerConfig(
                    state = playbackState,
                    modifier = if (isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    },
                    controlsLocked = showTrackDialog || showFitDialog || showEngineDialog,
                    overlay = { state, _, setControlsVisible ->
                        if (isFullscreen) {
                            FullscreenOverlay(
                                state = state,
                                title = currentChannel!!.name,
                                playerType = PlayerType.TV,
                                hasProgress = false,
                                hasPrevious = hasPrevious,
                                hasNext = hasNext,
                                orientationMode = orientationMode,
                                onBack = {
                                    fullscreenState.value = false
                                    setControlsVisible(true)
                                },
                                onShowAudioTracks = {
                                    setControlsVisible(true)
                                    trackDialogTab = TrackSelectionTab.Audio
                                    showTrackDialog = true
                                },
                                onShowSubtitleTracks = {
                                    setControlsVisible(true)
                                    trackDialogTab = TrackSelectionTab.Subtitles
                                    showTrackDialog = true
                                },
                                onShowFit = {
                                    setControlsVisible(true)
                                    showFitDialog = true
                                },
                                onShowEngineSettings = {
                                    setControlsVisible(true)
                                    showEngineDialog = true
                                },
                                onOrientationModeChange = { newMode ->
                                    setControlsVisible(true)
                                    orientationMode = newMode
                                },
                                onTogglePlay = {
                                    if (state.isPlaying) playerManager.pause() else playerManager.play()
                                },
                                onSeekBack = { playerManager.seekBackward() },
                                onSeekForward = { playerManager.seekForward() },
                                onRetry = { playerManager.playMedia(streamUrl, PlayerType.TV, forcePrepare = true) },
                                onSeek = { position -> playerManager.seekTo(position) },
                                onPrevious = { viewModel.playPreviousChannel() },
                                onNext = { viewModel.playNextChannel() },
                                enablePrevious = hasPrevious,
                                enableNext = hasNext,
                                onPip = { pipController.requestPip(onClose = stopAndClose) }
                            )
                        } else {
                            TVMiniOverlay(
                                state = state,
                                channelName = currentChannel!!.name,
                                hasTrackOptions = state.tracks.hasDialogOptions,
                                hasPrevious = hasPrevious,
                                hasNext = hasNext,
                                onClose = {
                                    stopAndClose()
                                    setControlsVisible(true)
                                },
                                onReplay = { playerManager.playMedia(streamUrl, PlayerType.TV, forcePrepare = true) },
                                onTogglePlay = {
                                    if (state.isPlaying) playerManager.pause() else playerManager.play()
                                },
                                onPrevious = { viewModel.playPreviousChannel() },
                                onNext = { viewModel.playNextChannel() },
                                onShowTracks = {
                                    setControlsVisible(true)
                                    showTrackDialog = true
                                },
                                onShowEngineSettings = {
                                    setControlsVisible(true)
                                    showEngineDialog = true
                                },
                                onFullscreen = { fullscreenState.value = true },
                                onPip = { pipController.requestPip(onClose = stopAndClose) }
                            )
                        }
                    }
                )
            )
        }

        if (!isFullscreen) {
            CategoryBar(
                categories = categories.categories,
                selectedCategoryId = categories.selectedCategory?.categoryId,
                highlightedCategoryIds = emptySet(),
                onCategorySelected = { category ->
                    viewModel.selectCategory(category)
                },
                modifier = Modifier.fillMaxWidth()
            )

            ChannelListView(
                channels = channelsPaging,
                favoriteChannelIds = categories.favoriteChannelIds,
                currentChannelId = currentChannel?.streamId,
                onChannelClick = { channel ->
                    coroutineScope.launch {
                        val restricted = viewModel.requiresPinForCategory(channel.categoryId)
                        if (restricted) {
                            pinInput = ""
                            pinError = null
                            pendingChannel = channel
                            showPinDialog = true
                        } else {
                            isChannelSwitching = true
                            viewModel.playChannel(channel)
                        }
                    }
                },
                onFavoriteClick = { channel ->
                    viewModel.toggleFavorite(channel)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showTrackDialog && playbackState.tracks.hasDialogOptions) {
        TrackSelectionDialog(
            tracks = playbackState.tracks,
            onDismiss = {
                showTrackDialog = false
                trackDialogTab = null
            },
            onAudioSelected = { option -> playerManager.selectAudioTrack(option.id) },
            onSubtitleSelected = { option ->
                if (option == null) playerManager.disableSubtitles() else playerManager.selectSubtitleTrack(option.id)
            },
            initialTab = trackDialogTab
        )
    }

    if (showFitDialog) {
        com.iptv.playxy.ui.player.VideoFitDialog(
            selectedScale = playerManager.getVideoScaleType(),
            onDismiss = { showFitDialog = false },
            onScaleSelected = { scaleType -> playerManager.setVideoScaleType(scaleType) },
            immersive = isFullscreen && hasChannelContext
        )
    }

    if (showEngineDialog) {
        com.iptv.playxy.ui.player.PlayerEngineSettingsDialog(
            initialConfig = playerManager.getEngineConfig(),
            onDismiss = { showEngineDialog = false },
            onApply = { config -> playerManager.setEngineConfig(config) },
            immersive = isFullscreen && hasChannelContext
        )
    }
}

private fun sanitizePinInput(input: String): String = input.filter { it.isDigit() }.take(4)
