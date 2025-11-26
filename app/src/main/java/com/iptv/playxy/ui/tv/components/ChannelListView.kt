package com.iptv.playxy.ui.tv.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.iptv.playxy.domain.LiveStream

@Composable
fun ChannelListView(
    channels: LazyPagingItems<LiveStream>,
    favoriteChannelIds: Set<String>,
    currentChannelId: String? = null,
    onChannelClick: (LiveStream) -> Unit,
    onFavoriteClick: (LiveStream) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentChannelId) {
        val index = currentChannelId?.let { id ->
            (0 until channels.itemCount).firstOrNull { idx -> channels.peek(idx)?.streamId == id }
        }
        if (index != null) {
            listState.animateScrollToItem(index)
        } else if (channels.itemCount > 0) {
            listState.scrollToItem(0)
        }
    }

    if (channels.itemCount == 0) {
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
            items(channels.itemCount) { idx ->
                val channel = channels[idx] ?: return@items
                ChannelRow(
                    channel = channel,
                    isFavorite = favoriteChannelIds.contains(channel.streamId),
                    isPlaying = channel.streamId == currentChannelId,
                    onChannelClick = onChannelClick,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
