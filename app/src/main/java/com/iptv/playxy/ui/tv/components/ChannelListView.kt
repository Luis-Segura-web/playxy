package com.iptv.playxy.ui.tv.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptv.playxy.domain.LiveStream

@Composable
fun ChannelListView(
    channels: List<LiveStream>,
    favoriteChannelIds: Set<String>,
    currentChannelId: String? = null,
    onChannelClick: (LiveStream) -> Unit,
    onFavoriteClick: (LiveStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Scroll to current channel when it changes or category changes
    LaunchedEffect(currentChannelId, channels) {
        if (currentChannelId != null && channels.isNotEmpty()) {
            val index = channels.indexOfFirst { it.streamId == currentChannelId }
            if (index >= 0) {
                // Canal encontrado en la lista - hacer scroll a él
                listState.animateScrollToItem(index)
            } else {
                // Canal no está en esta categoría - volver al inicio
                listState.scrollToItem(0)
            }
        } else if (channels.isNotEmpty()) {
            // No hay canal actual - volver al inicio
            listState.scrollToItem(0)
        }
    }

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
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            items(channels, key = { "${it.streamId}_${it.categoryId}" }) { channel ->
                ChannelRow(
                    channel = channel,
                    isFavorite = favoriteChannelIds.contains(channel.streamId),
                    isPlaying = channel.streamId == currentChannelId,
                    onChannelClick = onChannelClick,
                    onFavoriteClick = onFavoriteClick
                )
            }
        }
    }
}
