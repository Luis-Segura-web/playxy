package com.iptv.playxy.ui.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.iptv.playxy.ui.theme.PlayxyTheme

class FullscreenPlayerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_STREAM_URL = "stream_url"
        private const val EXTRA_CHANNEL_NAME = "channel_name"

        fun createIntent(context: Context, streamUrl: String, channelName: String): Intent {
            return Intent(context, FullscreenPlayerActivity::class.java).apply {
                putExtra(EXTRA_STREAM_URL, streamUrl)
                putExtra(EXTRA_CHANNEL_NAME, channelName)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable immersive mode
        setupImmersiveMode()

        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: ""
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""

        setContent {
            PlayxyTheme {
                FullscreenPlayer(
                    streamUrl = streamUrl,
                    channelName = channelName,
                    onClose = { finish() }
                )
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Player will be paused when activity goes to background
    }
    
    override fun onResume() {
        super.onResume()
        // Player will resume when activity comes to foreground
        setupImmersiveMode() // Re-apply immersive mode after returning
    }

    private fun setupImmersiveMode() {
        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun FullscreenPlayer(
    streamUrl: String,
    channelName: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Set up player listener
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> isBuffering = true
                        Player.STATE_READY -> {
                            isBuffering = false
                            isPlaying = true
                            showError = false
                        }
                        Player.STATE_IDLE, Player.STATE_ENDED -> {}
                    }
                }
                
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    showError = true
                    errorMessage = error.message ?: "Error al reproducir el stream"
                    isBuffering = false
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

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                showControls = !showControls
            }
    ) {
        // Video player
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
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay (shown on tap)
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Top bar with channel name and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channelName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )

                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Center play/pause button
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        // Buffering indicator
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp),
                color = Color.White,
                strokeWidth = 6.dp
            )
        }

        // Error message
        if (showError) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = onClose) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Auto-hide controls after 3 seconds
    LaunchedEffect(showControls) {
        if (showControls && !isBuffering && !showError) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
}
