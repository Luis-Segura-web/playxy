@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.iptv.playxy.ui.tv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iptv.playxy.ui.LocalFullscreenState
import com.iptv.playxy.ui.LocalPlayerManager
import com.iptv.playxy.ui.player.FullscreenPlayer
import com.iptv.playxy.ui.player.PlayerType
import com.iptv.playxy.ui.player.TVMiniPlayer
import com.iptv.playxy.ui.tv.components.CategoryChipBar
import com.iptv.playxy.ui.tv.components.ChannelListView
import com.iptv.playxy.util.StreamUrlBuilder

@Composable
fun TVScreen(
    viewModel: TVViewModel = hiltViewModel()
) {
    val currentChannel by viewModel.currentChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val playerManager = LocalPlayerManager.current
    val playbackState by playerManager.uiState.collectAsStateWithLifecycle()
    var isChannelSwitching by remember { mutableStateOf(false) }
    val fullscreenState = LocalFullscreenState.current
    val isFullscreen = fullscreenState.value

    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    LaunchedEffect(playbackState.streamUrl) {
        if (playbackState.streamUrl == null && currentChannel != null && !isChannelSwitching) {
            viewModel.stopPlayback()
            fullscreenState.value = false
        }
        if (playbackState.streamUrl != null) {
            isChannelSwitching = false
        }
    }

    if (isFullscreen && currentChannel != null && userProfile != null) {
        DisposableEffect(currentChannel!!.streamId) {
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
        // Fullscreen player in landscape mode
        FullscreenPlayer(
            streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
            title = currentChannel!!.name,
            playerType = PlayerType.TV,
            playerManager = playerManager,
            onBack = { fullscreenState.value = false },
            onPreviousItem = {
                isChannelSwitching = true
                viewModel.playPreviousChannel()
            },
            onNextItem = {
                isChannelSwitching = true
                viewModel.playNextChannel()
            },
            hasPrevious = viewModel.hasPreviousChannel(),
            hasNext = viewModel.hasNextChannel()
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            val shouldShowMiniPlayer = currentChannel != null && userProfile != null &&
                (playbackState.streamUrl != null || isChannelSwitching)
            if (shouldShowMiniPlayer) {
                DisposableEffect(currentChannel!!.streamId) {
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
                TVMiniPlayer(
                    streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
                    channelName = currentChannel!!.name,
                    playerManager = playerManager,
                    onPreviousChannel = {
                        isChannelSwitching = true
                        viewModel.playPreviousChannel()
                    },
                    onNextChannel = {
                        isChannelSwitching = true
                        viewModel.playNextChannel()
                    },
                    onClose = {
                        isChannelSwitching = false
                        viewModel.stopPlayback()
                        fullscreenState.value = false
                    },
                    onFullscreen = { fullscreenState.value = true }
                )
            }

            // Category Chip Bar
            CategoryChipBar(
                categories = categories,
                selected = selectedCategory,
                onCategorySelected = { category ->
                    viewModel.selectCategory(category)
                }
            )

            // Channel List (Scrollable, takes remaining space)
            ChannelListView(
                channels = filteredChannels,
                favoriteChannelIds = favoriteChannelIds,
                currentChannelId = currentChannel?.streamId,
                onChannelClick = { channel ->
                    isChannelSwitching = true
                    viewModel.playChannel(channel)
                },
                onFavoriteClick = { channel ->
                    viewModel.toggleFavorite(channel)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
