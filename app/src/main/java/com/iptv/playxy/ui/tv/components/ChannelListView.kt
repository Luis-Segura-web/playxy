package com.iptv.playxy.ui.tv.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.playxy.domain.LiveStream

@Composable
fun ChannelListView(
    channels: List<LiveStream>,
    favoriteChannelIds: Set<String>,
    onChannelClick: (LiveStream) -> Unit,
    onFavoriteClick: (LiveStream) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay canales disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(channels, key = { "${it.streamId}_${it.categoryId}" }) { channel ->
                ChannelRow(
                    channel = channel,
                    isFavorite = favoriteChannelIds.contains(channel.streamId),
                    onChannelClick = onChannelClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}
