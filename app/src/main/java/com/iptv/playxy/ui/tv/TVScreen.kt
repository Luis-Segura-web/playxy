package com.iptv.playxy.ui.tv

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.iptv.playxy.ui.player.FullscreenPlayerActivity
import com.iptv.playxy.ui.tv.components.*
import com.iptv.playxy.util.StreamUrlBuilder

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
    val userProfile by viewModel.userProfile.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Mini-Player (Only shown if currentChannel is not null)
        val channel = currentChannel
        val profile = userProfile
        val streamUrl = if (channel != null && profile != null) {
            StreamUrlBuilder.buildLiveStreamUrl(profile, channel)
        } else {
            ""
        }

        MiniPlayerView(
            channel = currentChannel,
            streamUrl = streamUrl,
            state = playerState,
            onClose = { viewModel.closePlayer() },
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.playNextChannel() },
            onPrev = { viewModel.playPreviousChannel() },
            onFullscreen = {
                currentChannel?.let { channel ->
                    userProfile?.let { profile ->
                        val fullscreenUrl = StreamUrlBuilder.buildLiveStreamUrl(profile, channel)
                        val intent = FullscreenPlayerActivity.createIntent(
                            context = context,
                            streamUrl = fullscreenUrl,
                            channelName = channel.name
                        )
                        context.startActivity(intent)
                    }
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
