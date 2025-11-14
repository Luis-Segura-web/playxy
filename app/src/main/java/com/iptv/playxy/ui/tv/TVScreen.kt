@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.iptv.playxy.ui.tv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
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
    val context = LocalContext.current
    val currentChannel by viewModel.currentChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var isFullscreenLocal by remember { mutableStateOf(false) }
    val globalFullscreenState = LocalFullscreenState.current

    // Sync local fullscreen state with global state
    LaunchedEffect(isFullscreenLocal) {
        globalFullscreenState.value = isFullscreenLocal
    }

    // Shared PlayerManager instance - survives composition changes
    val playerManager = LocalPlayerManager.current

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    if (isFullscreenLocal && currentChannel != null && userProfile != null) {
        // Fullscreen player in landscape mode
        FullscreenPlayer(
            streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
            title = currentChannel!!.name,
            playerType = PlayerType.TV,
            playerManager = playerManager,
            onBack = { isFullscreenLocal = false },
            onPreviousItem = { viewModel.playPreviousChannel() },
            onNextItem = { viewModel.playNextChannel() },
            hasPrevious = viewModel.hasPreviousChannel(),
            hasNext = viewModel.hasNextChannel()
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // Mini player when channel is playing
            if (currentChannel != null && userProfile != null) {
                TVMiniPlayer(
                    streamUrl = StreamUrlBuilder.buildLiveStreamUrl(userProfile!!, currentChannel!!),
                    channelName = currentChannel!!.name,
                    playerManager = playerManager,
                    onPreviousChannel = { viewModel.playPreviousChannel() },
                    onNextChannel = { viewModel.playNextChannel() },
                    onClose = {
                        playerManager.pause()
                        viewModel.stopPlayback()
                    },
                    onFullscreen = { isFullscreenLocal = true }
                )
            }

            // Category Chip Bar
            CategoryChipBar(
                categories = categories,
                selected = selectedCategory,
                onCategorySelected = { category ->
                    viewModel.selectCategory(category, context)
                }
            )

            // Channel List (Scrollable, takes remaining space)
            ChannelListView(
                channels = filteredChannels,
                favoriteChannelIds = favoriteChannelIds,
                currentChannelId = currentChannel?.streamId,
                onChannelClick = { channel ->
                    viewModel.playChannel(context, channel)
                },
                onFavoriteClick = { channel ->
                    viewModel.toggleFavorite(channel)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
