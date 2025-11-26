@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.iptv.playxy.ui.tv

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
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.components.CategoryBar
import com.iptv.playxy.ui.main.SortOrder
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.ui.player.TVMiniPlayer
import com.iptv.playxy.ui.tv.components.ChannelListView
import com.iptv.playxy.util.StreamUrlBuilder
import kotlinx.coroutines.launch

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

    var isChannelSwitching by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingChannel by remember { mutableStateOf<LiveStream?>(null) }
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

    if (isFullscreen && currentChannel != null && userProfile != null) {
        DisposableEffect(currentChannel!!.streamId) {
            playerManager.setTransportActions(
                onNext = {
                    isChannelSwitching = true
                    // navigation not kept in paging demo
                },
                onPrevious = {
                    isChannelSwitching = true
                }
            )
            onDispose { playerManager.setTransportActions(null, null) }
        }
        FullscreenPlayer(
            streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
            title = currentChannel!!.name,
            playerType = PlayerType.TV,
            playerManager = playerManager,
            onBack = { fullscreenState.value = false }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            val shouldShowMiniPlayer = currentChannel != null && userProfile != null &&
                (playbackState.streamUrl != null || isChannelSwitching)
            if (shouldShowMiniPlayer) {
                TVMiniPlayer(
                    streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
                    channelName = currentChannel!!.name,
                    playerManager = playerManager,
                    onPreviousChannel = {},
                    onNextChannel = {},
                    onClose = {
                        isChannelSwitching = false
                        playerManager.stopPlayback()
                        fullscreenState.value = false
                    },
                    onFullscreen = { fullscreenState.value = true }
                )
            }

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
}

private fun sanitizePinInput(input: String): String = input.filter { it.isDigit() }.take(4)
