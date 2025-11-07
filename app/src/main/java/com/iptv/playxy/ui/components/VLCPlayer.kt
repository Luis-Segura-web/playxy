package com.iptv.playxy.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * VLC Player Component
 * 
 * This component is used to play IPTV streams using VLC SDK
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
    
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var libVLC by remember { mutableStateOf<LibVLC?>(null) }
    
    DisposableEffect(streamUrl) {
        try {
            // Initialize LibVLC with options optimized for IPTV streaming
            val vlc = LibVLC(context, arrayListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--rtsp-tcp",
                "--network-caching=1500",
                "--live-caching=1500"
            ))
            libVLC = vlc
            
            // Initialize MediaPlayer
            val player = MediaPlayer(vlc)
            mediaPlayer = player
            
            // Setup event listeners
            player.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
                        if (event.buffering < 100f) {
                            onBuffering()
                        } else {
                            onPlaying()
                        }
                    }
                    MediaPlayer.Event.Playing -> onPlaying()
                    MediaPlayer.Event.EncounteredError -> onError("Error al reproducir el stream")
                    else -> {}
                }
            }
            
            // Start playback
            if (streamUrl.isNotEmpty()) {
                val media = Media(vlc, Uri.parse(streamUrl))
                player.media = media
                media.release()
                player.play()
            }
        } catch (e: Exception) {
            onError(e.message ?: "Error desconocido")
        }
        
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            libVLC?.release()
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
                VLCVideoLayout(ctx).apply {
                    mediaPlayer?.attachViews(this, null, false, false)
                }
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
