package com.iptv.playxy.ui.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(@param:ApplicationContext private val context: Context) {

    private var player: ExoPlayer? = null
    private var currentUrl: String? = null
    private var currentType: PlayerType = PlayerType.TV
    private val tag = "PlayerManager"

    // Retry logic
    private var retryCount = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastStartTimestamp: Long = 0L
    private var lastFrameTimestamp: Long = 0L
    private val firstFrameRendered = AtomicBoolean(false)
    private val frameListeners = CopyOnWriteArrayList<(Boolean) -> Unit>()
    private var watchdogPosted = false
    private val frameWatchdogTimeoutMs = 7_000L // Dar más margen antes de reiniciar

    init { registerManager(this) }

    fun addFrameListener(listener: (Boolean) -> Unit) { frameListeners.addIfAbsent(listener) }
    fun removeFrameListener(listener: (Boolean) -> Unit) { frameListeners.remove(listener) }
    fun hasRenderedFirstFrame(): Boolean = firstFrameRendered.get()
    @OptIn(UnstableApi::class)
    fun initializePlayer(): ExoPlayer {
        if (player == null) {
            Log.d(tag, "Inicializando ExoPlayer")
            val (connectTimeout, readTimeout) = when (currentType) {
                PlayerType.TV -> 15_000 to 20_000
                PlayerType.MOVIE, PlayerType.SERIES -> 30_000 to 30_000
            }
            val httpFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("PlayXY/1.0 (Android)")
                .setConnectTimeoutMs(connectTimeout)
                .setReadTimeoutMs(readTimeout)
                .setAllowCrossProtocolRedirects(true)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

            player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(DefaultLoadControl())
                .build()
                .apply {
                    setAudioAttributes(
                        androidx.media3.common.AudioAttributes.Builder()
                            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                            .build(), true
                    )
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) { Log.d(tag, "onIsPlayingChanged = $isPlaying") }
                        override fun onPlaybackStateChanged(state: Int) {
                            val stateName = when (state) {
                                Player.STATE_IDLE -> "IDLE"
                                Player.STATE_BUFFERING -> "BUFFERING"
                                Player.STATE_READY -> "READY"
                                Player.STATE_ENDED -> "ENDED"
                                else -> state.toString()
                            }
                            Log.d(tag, "onPlaybackStateChanged = $stateName")
                            if (state == Player.STATE_BUFFERING) {
                                // Reiniciar indicadores para nueva preparación y notificar listeners (no listo)
                                frameListeners.forEach { it(false) }
                                firstFrameRendered.set(false)
                                lastStartTimestamp = System.currentTimeMillis()
                                lastFrameTimestamp = 0L
                                scheduleFrameWatchdog()
                            }
                            if (state == Player.STATE_READY) {
                                retryCount = 0
                                // READY sin primer frame todavía: watchdog seguirá activo
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) { Log.e(tag, "PlayerError: ${error.errorCodeName} - ${error.message}"); handleRetryOnError(error) }
                        override fun onRenderedFirstFrame() { markFrameRendered() }
                        override fun onVideoSizeChanged(videoSize: VideoSize) { if (!firstFrameRendered.get() && videoSize.width > 0 && videoSize.height > 0) markFrameRendered() }
                    })
                }
        }
        return player!!
    }

    private fun markFrameRendered() {
        if (firstFrameRendered.compareAndSet(false, true)) {
            lastFrameTimestamp = System.currentTimeMillis()
            Log.d(tag, "Primer frame de video renderizado tras ${lastFrameTimestamp - lastStartTimestamp}ms")
            frameListeners.forEach { it(true) }
            watchdogPosted = false
        }
    }

    private fun scheduleFrameWatchdog() {
        if (watchdogPosted) return
        watchdogPosted = true
        mainHandler.postDelayed({
            watchdogPosted = false
            if (!firstFrameRendered.get()) {
                val elapsed = System.currentTimeMillis() - lastStartTimestamp
                Log.w(tag, "Watchdog: sin primer frame tras ${elapsed}ms. Reiniciando pipeline rápido.")
                forceSoftReset()
            }
        }, frameWatchdogTimeoutMs)
    }

    private fun forceSoftReset() {
        val url = currentUrl ?: return
        val p = player ?: return
        Log.d(tag, "Soft reset (clearSurface + stop + prepare + play) para intentar obtener frames")
        p.playWhenReady = false
        p.stop()
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
    }

    private fun maxRetriesFor(type: PlayerType): Int = when (type) {
        PlayerType.TV -> 1
        PlayerType.MOVIE, PlayerType.SERIES -> 3
    }

    private fun handleRetryOnError(error: PlaybackException) {
        val shouldRetry = (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        val url = currentUrl
        if (shouldRetry && url != null && retryCount < maxRetriesFor(currentType)) {
            val delayMs = if (currentType == PlayerType.TV) 1500L else (2_000L * (retryCount + 1))
            Log.d(tag, "Retry ${retryCount + 1}/${maxRetriesFor(currentType)} en ${delayMs}ms por ${error.errorCodeName}")
            retryCount++
            mainHandler.postDelayed({ playMedia(url, currentType) }, delayMs)
        } else {
            Log.d(tag, "No se reintentará. shouldRetry=$shouldRetry url=${url != null} retryCount=$retryCount tipo=$currentType")
        }
    }

    fun playMedia(url: String, type: PlayerType = PlayerType.TV) {
        currentType = type
        initializePlayer()
        pauseAllExcept(this)
        val p = player ?: return
        if (currentUrl != url) {
            Log.d(tag, "Nueva URL, preparando media: $url ($type)")
            currentUrl = url
            retryCount = 0
            firstFrameRendered.set(false)
            frameListeners.forEach { it(false) }
            lastStartTimestamp = System.currentTimeMillis()
            lastFrameTimestamp = 0L
            p.stop()
            p.setMediaItem(MediaItem.fromUri(url))
            p.prepare()
            p.playWhenReady = true
            scheduleFrameWatchdog()
        } else {
            Log.d(tag, "Misma URL, reanudando si estaba pausado ($type)")
            if (p.playbackState == Player.STATE_IDLE) p.prepare()
            p.playWhenReady = true
            if (!firstFrameRendered.get()) scheduleFrameWatchdog()
        }
    }

    fun play() {
        initializePlayer()
        pauseAllExcept(this)
        player?.apply {
            Log.d(tag, "play() llamado")
            if (playbackState == Player.STATE_IDLE) prepare()
            playWhenReady = true
        }
    }

    fun pause() { player?.apply { Log.d(tag, "pause() llamado"); pause() } }

    fun release() { player?.clearVideoSurface(); player?.release(); player = null; currentUrl = null; firstFrameRendered.set(false); watchdogPosted = false }

    fun seekTo(positionMs: Long) { player?.seekTo(positionMs) }
    fun seekForward(incrementMs: Long = 10000) { player?.seekTo((player?.currentPosition ?: 0L) + incrementMs) }
    fun seekBackward(decrementMs: Long = 10000) { player?.seekTo(((player?.currentPosition ?: 0L) - decrementMs).coerceAtLeast(0)) }
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0L
    fun getDuration(): Long = player?.duration ?: 0L
    fun isPlaying(): Boolean = player?.isPlaying ?: false
    fun getPlayer(): Player? = player

    companion object {
        private val managers = CopyOnWriteArrayList<PlayerManager>()
        private fun registerManager(manager: PlayerManager) { managers.addIfAbsent(manager) }
        private fun pauseAllExcept(self: PlayerManager) { managers.forEach { m -> if (m !== self) m.pause() } }
    }
}
