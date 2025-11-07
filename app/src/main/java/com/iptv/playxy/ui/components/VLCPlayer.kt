package com.iptv.playxy.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Video Player Component
 * 
 * This component is used to play IPTV streams using ExoPlayer
 * 
 * @param streamUrl The URL of the stream to play
 * @param modifier Modifier for the composable
 * @param onBuffering Callback when buffering
 * @param onPlaying Callback when playing
 * @param onError Callback when error occurs
 */
@Composable
fun VLCPlayer(
    streamUrl: String,
    modifier: Modifier = Modifier,
    onBuffering: () -> Unit = {},
    onPlaying: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            // Set up player listener
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> onBuffering()
                        Player.STATE_READY -> onPlaying()
                        Player.STATE_IDLE, Player.STATE_ENDED -> {}
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    onError(error.message ?: "Error de reproducción")
                }
            })
            
            // Prepare and play the media
            if (streamUrl.isNotEmpty()) {
                setMediaItem(MediaItem.fromUri(streamUrl))
                prepare()
                playWhenReady = true
            }
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We'll use our own controls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
