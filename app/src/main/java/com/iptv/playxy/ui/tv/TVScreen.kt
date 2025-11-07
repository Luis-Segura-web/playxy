package com.iptv.playxy.ui.tv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.iptv.playxy.ui.player.FullscreenPlayerActivity
import com.iptv.playxy.ui.tv.components.*

@Composable
fun TVScreen(
    viewModel: TVViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Mini-Player (Only shown if currentChannel is not null)
        MiniPlayerView(
            channel = currentChannel,
            state = playerState,
            onClose = { viewModel.closePlayer() },
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.playNextChannel() },
            onPrev = { viewModel.playPreviousChannel() },
            onFullscreen = {
                currentChannel?.let { channel ->
                    val intent = FullscreenPlayerActivity.createIntent(
                        context = context,
                        streamUrl = channel.directSource ?: "",
                        channelName = channel.name
                    )
                    context.startActivity(intent)
                }
            }
        )

        // 2. Current Channel Info (Only shown if currentChannel is not null)
        CurrentChannelInfoView(channel = currentChannel)

        // 3. Category Chip Bar
        CategoryChipBar(
            categories = categories,
            selected = selectedCategory,
            onCategorySelected = { category ->
                viewModel.selectCategory(category)
            }
        )

        // 4. Channel List (Scrollable, takes remaining space)
        ChannelListView(
            channels = filteredChannels,
            favoriteChannelIds = favoriteChannelIds,
            onChannelClick = { channel ->
                viewModel.playChannel(channel)
            },
            onFavoriteClick = { channel ->
                viewModel.toggleFavorite(channel)
            },
            modifier = Modifier.weight(1f)
        )
    }
}
