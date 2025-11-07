package com.iptv.playxy.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
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
import com.iptv.playxy.ui.theme.PlayxyTheme
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class FullscreenPlayerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_STREAM_URL = "stream_url"
        private const val EXTRA_CHANNEL_NAME = "channel_name"
        private const val CONTROLS_AUTO_HIDE_DELAY = 3000L

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

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var libVLC by remember { mutableStateOf<LibVLC?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showControls by remember { mutableStateOf(true) }

    DisposableEffect(streamUrl) {
        try {
            // Initialize LibVLC with options optimized for IPTV streaming
            val vlc = LibVLC(context, arrayListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--rtsp-tcp",
                "--network-caching=1500",
                "--live-caching=1500",
                "--clock-jitter=0",
                "--clock-synchro=0"
            ))
            libVLC = vlc

            // Initialize MediaPlayer
            val player = MediaPlayer(vlc)
            mediaPlayer = player

            // Setup event listeners
            player.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
                        isBuffering = event.buffering < 100f
                        if (event.buffering >= 100f) {
                            isPlaying = true
                        }
                    }
                    MediaPlayer.Event.Playing -> {
                        isPlaying = true
                        isBuffering = false
                        showError = false
                    }
                    MediaPlayer.Event.Paused -> {
                        isPlaying = false
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        showError = true
                        errorMessage = "Error al reproducir el stream"
                        isBuffering = false
                    }
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
            showError = true
            errorMessage = e.message ?: "Error desconocido"
        }

        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            libVLC?.release()
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
                VLCVideoLayout(ctx).apply {
                    mediaPlayer?.attachViews(this, null, false, false)
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
                        mediaPlayer?.let { player ->
                            if (isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
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

    // Auto-hide controls after delay
    LaunchedEffect(showControls) {
        if (showControls && !isBuffering && !showError) {
            kotlinx.coroutines.delay(3000L)
            showControls = false
        }
    }
}
