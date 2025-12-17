package com.iptv.playxy.ui.player

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.iptv.playxy.data.repository.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import com.iptv.playxy.domain.player.PlayerEngineConfig
import com.iptv.playxy.domain.player.DecoderMode
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private const val TAG = "PlayerManager"
private const val POSITION_UPDATE_INTERVAL_MS = 500L
private const val HEALTH_MONITOR_INTERVAL_MS = 1000L
private const val HW_STALL_GRACE_MS = 6000L
private const val HW_STALL_TIMEOUT_MS = 4500L
private const val HW_STALL_MIN_PROGRESS_DELTA_MS = 250L

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext context: Context,
    private val preferencesManager: PreferencesManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext

    private var foregroundServiceActive = false
    private var nextAction: (() -> Unit)? = null
    private var previousAction: (() -> Unit)? = null

    private var engineConfig: PlayerEngineConfig = preferencesManager.getPlayerEngineConfig()
    private var libVlc: LibVLC = LibVLC(appContext, engineConfig.toLibVlcOptions())
    private var mediaPlayer: MediaPlayer = MediaPlayer(libVlc)
    private var currentVideoLayout: VLCVideoLayout? = null
    private var videoScaleType: MediaPlayer.ScaleType = MediaPlayer.ScaleType.SURFACE_BEST_FIT

    private var pendingSeekAfterPlayMs: Long? = null
    private var playbackSessionId: Int = 0
    private var hwFallbackSessionId: Int? = null

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var currentRequest: PlaybackRequest? = null
    private var progressJob: Job? = null
    private var trackLookup: Map<String, TrackOption> = emptyMap()
    private var userStopped = false
    private var isIdle = true
    private var bufferingPercent: Float = 0f

    private var retryCount = 0
    private var retryJob: Job? = null
    private var prepareJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var videoReattachJob: Job? = null
    private var videoReattachAttempts: Int = 0
    private var detachViewsJob: Job? = null
    private var pendingPlayUntilSurface = false
    private var healthJob: Job? = null
    private var healthIgnoreUntilMs: Long = 0L
    private var lastHealthPositionMs: Long = 0L
    private var lastHealthProgressMs: Long = 0L
    private val stopEventsToIgnore = AtomicInteger(0)
    private val maxRetries = 3
    private val maxVideoReattachAttempts = 6
    private val retryDelayMs = 2000L
    private val detachViewsDelayMs = 900L
    private val connectionTimeoutMs = 15000L
    
    // Timeout inicial más corto para detectar fallos de conexión rápidos (HTTP connection failure)
    // Si VLC no emite evento Buffering dentro de este tiempo, probablemente falló silenciosamente
    private val earlyConnectionTimeoutMs = 5000L
    private var earlyConnectionTimeoutJob: Job? = null
    private var hasReceivedBuffering = false

    init {
        bindMediaPlayerEvents(mediaPlayer)
        startProgressUpdates()
        startHealthMonitor()
    }

    fun setEngineConfig(config: PlayerEngineConfig) {
        if (engineConfig == config) return
        engineConfig = config
        preferencesManager.setPlayerEngineConfig(config)
        recreateEngine()
    }

    fun getEngineConfig(): PlayerEngineConfig = engineConfig

    private fun setEngineConfigTemporary(config: PlayerEngineConfig) {
        if (engineConfig == config) return
        engineConfig = config
        recreateEngine(resumeIfPlaying = false)
    }

    private fun recreateEngine(resumeIfPlaying: Boolean = true) {
        val requestSnapshot = currentRequest
        val resume = resumeIfPlaying && requestSnapshot != null && mediaPlayer.isPlaying && !userStopped
        val resumePositionMs =
            if (resume && requestSnapshot?.type != PlayerType.TV) {
                safePositionMs().takeIf { it > 0L }
            } else {
                null
            }

        runCatching { mediaPlayer.detachViews() }
        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.release() }
        runCatching { libVlc.release() }

        libVlc = LibVLC(appContext, engineConfig.toLibVlcOptions())
        mediaPlayer = MediaPlayer(libVlc)
        bindMediaPlayerEvents(mediaPlayer)

        currentVideoLayout?.let { layout ->
            if (layout.isAttachedToWindow) {
                runCatching { mediaPlayer.attachViews(layout, null, true, true) }
                runCatching { mediaPlayer.videoScale = videoScaleType }
                runCatching { mediaPlayer.updateVideoSurfaces() }
            }
        }

        if (resume) {
            playMedia(
                requestSnapshot!!.url,
                requestSnapshot.type,
                forcePrepare = true,
                startPositionMs = resumePositionMs
            )
        }
    }

    private fun maybeFallbackToSoftware(reason: String): Boolean {
        val request = currentRequest ?: return false
        if (engineConfig.decoderMode != DecoderMode.HARDWARE) return false
        if (hwFallbackSessionId == playbackSessionId) return false

        hwFallbackSessionId = playbackSessionId

        val resumePositionMs =
            when (request.type) {
                PlayerType.TV -> null
                PlayerType.MOVIE, PlayerType.SERIES -> safePositionMs().takeIf { it > 0L }
            }

        Log.w(TAG, "Falling back to software decoder ($reason). Resume=$resumePositionMs")
        pendingSeekAfterPlayMs = resumePositionMs
        setEngineConfigTemporary(engineConfig.copy(decoderMode = DecoderMode.SOFTWARE))
        playMedia(request.url, request.type, forcePrepare = true, startPositionMs = resumePositionMs)
        return true
    }

    private fun applyPendingSeekIfNeeded() {
        val seekMs = pendingSeekAfterPlayMs ?: return
        val request = currentRequest
        if (request == null || request.type == PlayerType.TV) {
            pendingSeekAfterPlayMs = null
            return
        }

        pendingSeekAfterPlayMs = null
        scope.launch {
            delay(200L)
            markHealthGracePeriod("PendingSeek", graceMs = 5000L)
            runCatching { mediaPlayer.time = seekMs }
        }
    }

    private fun markHealthGracePeriod(reason: String, graceMs: Long = HW_STALL_GRACE_MS) {
        val now = SystemClock.elapsedRealtime()
        healthIgnoreUntilMs = maxOf(healthIgnoreUntilMs, now + graceMs)
        lastHealthPositionMs = safePositionMs()
        lastHealthProgressMs = now
        Log.d(TAG, "Health grace period: $reason for ${graceMs}ms")
    }

    private fun startHealthMonitor() {
        healthJob?.cancel()
        healthJob = scope.launch {
            while (isActive) {
                delay(HEALTH_MONITOR_INTERVAL_MS)
                val request = currentRequest ?: continue
                if (engineConfig.decoderMode != DecoderMode.HARDWARE) continue
                if (hwFallbackSessionId == playbackSessionId) continue
                if (pendingPlayUntilSurface || currentVideoLayout == null) continue
                if (userStopped || isIdle) continue
                if (!mediaPlayer.isPlaying) continue

                val state = _uiState.value
                if (state.isBuffering || !state.firstFrameRendered) {
                    val now = SystemClock.elapsedRealtime()
                    lastHealthProgressMs = now
                    lastHealthPositionMs = safePositionMs()
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                if (now < healthIgnoreUntilMs) {
                    lastHealthProgressMs = now
                    lastHealthPositionMs = safePositionMs()
                    continue
                }

                val positionMs = safePositionMs()
                val deltaMs = kotlin.math.abs(positionMs - lastHealthPositionMs)
                if (deltaMs >= HW_STALL_MIN_PROGRESS_DELTA_MS) {
                    lastHealthPositionMs = positionMs
                    lastHealthProgressMs = now
                    continue
                }

                val stalledMs = now - lastHealthProgressMs
                if (stalledMs >= HW_STALL_TIMEOUT_MS) {
                    Log.w(
                        TAG,
                        "HW stall detected (pos=$positionMs, stalledMs=$stalledMs). Triggering fallback."
                    )
                    if (maybeFallbackToSoftware("Stall")) {
                        markHealthGracePeriod("FallbackAfterStall")
                    } else {
                        lastHealthProgressMs = now
                        lastHealthPositionMs = positionMs
                    }
                }
            }
        }
    }

    private fun bindMediaPlayerEvents(player: MediaPlayer) {
        player.setEventListener { event ->
            scope.launch {
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
                        // Marcar que VLC empezó a conectar exitosamente
                        if (!hasReceivedBuffering) {
                            hasReceivedBuffering = true
                            earlyConnectionTimeoutJob?.cancel()
                            Log.d(TAG, "First buffering event received - connection established")
                        }
                        bufferingPercent = event.buffering.coerceIn(0f, 100f)
                        _uiState.update {
                            val durationMs = it.durationMs.takeIf { d -> d > 0L } ?: safeDurationMs()
                            val bufferedMs = if (durationMs > 0L) {
                                (durationMs * (bufferingPercent / 100f)).toLong()
                            } else {
                                it.bufferedPositionMs
                            }
                            it.copy(isBuffering = bufferingPercent < 100f, bufferedPositionMs = bufferedMs)
                        }
                    }

                    MediaPlayer.Event.Playing -> {
                        retryJob?.cancel()
                        connectionTimeoutJob?.cancel()
                        earlyConnectionTimeoutJob?.cancel()
                        hasReceivedBuffering = true
                        retryCount = 0
                        userStopped = false
                        bufferingPercent = 100f
                        _uiState.update {
                            it.copy(
                                isPlaying = true,
                                isBuffering = false,
                                hasError = false,
                                errorMessage = null
                            )
                        }
                        updateForegroundPlayback(true)
                        updateTracksFromPlayer()
                        markHealthGracePeriod("Playing")
                        applyPendingSeekIfNeeded()
                    }

                    MediaPlayer.Event.Paused -> {
                        _uiState.update { it.copy(isPlaying = false) }
                        updateForegroundPlayback(false)
                    }

                    MediaPlayer.Event.Vout -> {
                        if (event.voutCount > 0) {
                            videoReattachJob?.cancel()
                            connectionTimeoutJob?.cancel()
                            earlyConnectionTimeoutJob?.cancel()
                            videoReattachAttempts = 0
                            _uiState.update { it.copy(firstFrameRendered = true, isBuffering = false) }
                            retryCount = 0
                            markHealthGracePeriod("Vout>0", graceMs = 3000L)
                        } else if (player.isPlaying && currentRequest != null) {
                            scheduleVideoReattach("Vout=0")
                        }
                    }

                    MediaPlayer.Event.EncounteredError -> {
                        Log.e(TAG, "Playback error (retry $retryCount/$maxRetries)")
                        if (maybeFallbackToSoftware("EncounteredError")) return@launch
                        handlePlaybackError("Error de reproducción")
                    }

                    MediaPlayer.Event.EndReached -> {
                        if (currentRequest != null && !userStopped) {
                            handleAutoRestart("Playback ended")
                        } else {
                            isIdle = true
                            updateForegroundPlayback(false)
                        }
                    }

                    MediaPlayer.Event.Stopped -> {
                        if (stopEventsToIgnore.get() > 0) {
                            stopEventsToIgnore.decrementAndGet()
                            return@launch
                        }
                        if (currentRequest != null && !userStopped) {
                            if (maybeFallbackToSoftware("Stopped")) return@launch
                            handleAutoRestart("Playback stopped")
                        } else {
                            isIdle = true
                            updateForegroundPlayback(false)
                        }
                    }

                    MediaPlayer.Event.LengthChanged,
                    MediaPlayer.Event.TimeChanged,
                    MediaPlayer.Event.PositionChanged -> updateProgress()
                }
            }
        }
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        val requestSnapshot = currentRequest
        retryJob = scope.launch {
            delay(retryDelayMs)
            val request = currentRequest ?: return@launch
            if (currentRequest !== requestSnapshot) return@launch
            if (mediaPlayer.isPlaying) return@launch
            Log.d(TAG, "Auto-retrying playback: ${request.url}")
            playMedia(request.url, request.type, forcePrepare = true)
        }
    }

    private fun scheduleVideoReattach(reason: String) {
        val layoutSnapshot = currentVideoLayout ?: return
        if (videoReattachAttempts >= maxVideoReattachAttempts) {
            maybeFallbackToSoftware("VoutLost/$reason")
            return
        }
        if (!layoutSnapshot.isAttachedToWindow) return

        videoReattachAttempts++
        val attempt = videoReattachAttempts
        videoReattachJob?.cancel()
        videoReattachJob = scope.launch {
            delay(200L * attempt)
            if (currentVideoLayout !== layoutSnapshot) return@launch
            if (!layoutSnapshot.isAttachedToWindow) return@launch
            if (!mediaPlayer.isPlaying || currentRequest == null) return@launch
            Log.w(TAG, "Reatachando video layout ($reason), intento $attempt/$maxVideoReattachAttempts")
            runCatching { mediaPlayer.updateVideoSurfaces() }
        }
    }

    private fun handleAutoRestart(reason: String) {
        if (retryCount < maxRetries && currentRequest != null) {
            retryCount++
            _uiState.update {
                it.copy(
                    isBuffering = true,
                    hasError = false,
                    errorMessage = "Reintentando... ($retryCount/$maxRetries)"
                )
            }
            Log.w(TAG, "Auto-restart triggered ($reason). Retry $retryCount/$maxRetries")
            scheduleRetry()
        } else {
            isIdle = true
            _uiState.update {
                it.copy(
                    hasError = true,
                    errorMessage = "Reproducción detenida",
                    isPlaying = false,
                    isBuffering = false
                )
            }
            updateForegroundPlayback(false)
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateProgress() {
        if (isIdle) return
        val safeDuration = safeDurationMs().takeIf { it > 0L } ?: _uiState.value.durationMs
        val safePosition = safePositionMs()
        val bufferedPosition = if (safeDuration > 0L) {
            (safeDuration * (bufferingPercent / 100f))
                .toLong()
                .coerceAtLeast(safePosition)
                .coerceAtMost(safeDuration)
        } else {
            _uiState.value.bufferedPositionMs
        }
        _uiState.update {
            it.copy(
                positionMs = safePosition,
                durationMs = safeDuration,
                bufferedPositionMs = bufferedPosition
            )
        }
    }

    fun playMedia(
        url: String,
        type: PlayerType = PlayerType.TV,
        forcePrepare: Boolean = false,
        startPositionMs: Long? = null
    ) {
        if (url.isBlank()) return

        retryJob?.cancel()
        prepareJob?.cancel()
        connectionTimeoutJob?.cancel()
        earlyConnectionTimeoutJob?.cancel()
        hasReceivedBuffering = false
        userStopped = false
        markHealthGracePeriod("playMedia", graceMs = 6000L)

        val lastRequest = currentRequest
        if (!forcePrepare && lastRequest?.url == url && lastRequest.type == type) {
            if (!mediaPlayer.isPlaying) {
                if (currentVideoLayout == null) {
                    pendingPlayUntilSurface = true
                    Log.d(TAG, "Esperando surface para reanudar reproducciA3n")
                    _uiState.update { it.copy(playerType = type, streamUrl = url, hasError = false) }
                    return
                }
                isIdle = false
                scope.launch(Dispatchers.IO) { runCatching { mediaPlayer.play() } }
            }
            _uiState.update { it.copy(playerType = type, streamUrl = url, hasError = false) }
            return
        }

        playbackSessionId++
        hwFallbackSessionId = null
        pendingSeekAfterPlayMs = startPositionMs?.takeIf { it > 0L && type != PlayerType.TV }

        if (lastRequest?.url != url) {
            retryCount = 0
        }

        currentRequest = PlaybackRequest(url, type)
        isIdle = false
        bufferingPercent = 0f

        _uiState.update {
            it.copy(
                streamUrl = url,
                playerType = type,
                isBuffering = true,
                hasError = false,
                errorMessage = null,
                firstFrameRendered = false,
                positionMs = 0L,
                durationMs = 0L,
                tracks = PlaybackTracks.empty(),
                selectedAudioTrackId = null,
                selectedSubtitleTrackId = null
            )
        }

        val requestSnapshot = currentRequest
        prepareJob = scope.launch(Dispatchers.IO) {
            runCatching {
                if (currentRequest !== requestSnapshot) return@launch
                stopEventsToIgnore.incrementAndGet()
                runCatching { mediaPlayer.stop() }
                    .onFailure { stopEventsToIgnore.decrementAndGet() }
                setMedia(url, type)
                if (currentRequest !== requestSnapshot) return@launch
                if (currentVideoLayout == null) {
                    pendingPlayUntilSurface = true
                    Log.d(TAG, "Esperando surface para iniciar reproducciA3n")
                    return@launch
                }
                pendingPlayUntilSurface = false
                mediaPlayer.play()
                // Start connection timeout to detect silent VLC failures (e.g., HTTP connection failure)
                startConnectionTimeout(requestSnapshot)
            }.onFailure { throwable ->
                Log.e(TAG, "Error al preparar reproducción", throwable)
                withContext(Dispatchers.Main.immediate) {
                    handlePlaybackError("No se pudo iniciar la reproducción")
                }
            }
        }
    }
    
    private fun startConnectionTimeout(requestSnapshot: PlaybackRequest?) {
        connectionTimeoutJob?.cancel()
        earlyConnectionTimeoutJob?.cancel()
        
        // Early timeout: detecta fallos de conexión rápidos (HTTP connection failure)
        // Si VLC no emite ningún evento Buffering en 5 segundos, probablemente falló silenciosamente
        earlyConnectionTimeoutJob = scope.launch {
            delay(earlyConnectionTimeoutMs)
            if (currentRequest === requestSnapshot && !hasReceivedBuffering && !mediaPlayer.isPlaying && !userStopped) {
                val state = _uiState.value
                if (!state.hasError && !state.firstFrameRendered) {
                    Log.w(TAG, "Early connection timeout - No buffering received (retry $retryCount/$maxRetries)")
                    handlePlaybackError("Error de conexión")
                }
            }
        }
        
        // Timeout largo: para casos donde VLC conecta pero nunca empieza a reproducir
        connectionTimeoutJob = scope.launch {
            delay(connectionTimeoutMs)
            // Si después del timeout no estamos reproduciendo y no hubo error, es un fallo silencioso
            if (currentRequest === requestSnapshot && !mediaPlayer.isPlaying && !userStopped) {
                val state = _uiState.value
                if (!state.hasError && !state.firstFrameRendered) {
                    Log.w(TAG, "Connection timeout - VLC failed silently (retry $retryCount/$maxRetries)")
                    handlePlaybackError("Error de conexión")
                }
            }
        }
    }

    private fun setMedia(url: String, type: PlayerType) {
        val media = Media(libVlc, Uri.parse(url)).apply {
            val userAgent = "PlayXY/VLC"
            addOption(":http-user-agent=$userAgent")
            addOption(":user-agent=$userAgent")
            addOption(":http-reconnect=true")
            val cachingMs =
                when (type) {
                    PlayerType.TV -> 4000
                    PlayerType.SERIES -> 4500
                    PlayerType.MOVIE -> 6500
                }
            addOption(":network-caching=$cachingMs")
            addOption(":live-caching=$cachingMs")
            addOption(":clock-jitter=0")
            addOption(":clock-synchro=0")

            when (engineConfig.decoderMode) {
                DecoderMode.HARDWARE -> setHWDecoderEnabled(true, true)
                DecoderMode.SOFTWARE -> setHWDecoderEnabled(false, true)
            }
        }
        mediaPlayer.media = media
        media.release()
    }

    fun play() {
        userStopped = false
        if (currentRequest != null && !mediaPlayer.isPlaying) {
            isIdle = false
            mediaPlayer.play()
        }
    }

    fun pause() {
        userStopped = true
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun stopPlayback() {
        retryJob?.cancel()
        prepareJob?.cancel()
        connectionTimeoutJob?.cancel()
        earlyConnectionTimeoutJob?.cancel()
        retryCount = 0
        userStopped = true
        pendingPlayUntilSurface = false

        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.media = null }
        runCatching { mediaPlayer.detachViews() }
        currentRequest = null
        isIdle = true
        trackLookup = emptyMap()
        currentVideoLayout = null
        _uiState.value = PlaybackUiState()
        updateForegroundPlayback(false)
        nextAction = null
        previousAction = null
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer.time = positionMs.coerceAtLeast(0L)
        markHealthGracePeriod("seekTo", graceMs = 4000L)
        updateProgress()
    }

    fun seekForward(incrementMs: Long = 10_000L) {
        seekTo(safePositionMs() + incrementMs)
    }

    fun seekBackward(decrementMs: Long = 10_000L) {
        seekTo(safePositionMs() - decrementMs)
    }

    fun getCurrentPosition(): Long = safePositionMs()

    fun getDuration(): Long = safeDurationMs()

    fun isPlaying(): Boolean = mediaPlayer.isPlaying

    fun hasActivePlayback(): Boolean = currentRequest != null && !isIdle

    fun setTransportActions(onNext: (() -> Unit)?, onPrevious: (() -> Unit)?) {
        nextAction = onNext
        previousAction = onPrevious
    }

    fun attachVideoLayout(layout: VLCVideoLayout) {
        detachViewsJob?.cancel()
        if (currentVideoLayout === layout) {
            runCatching { mediaPlayer.videoScale = videoScaleType }
            runCatching { mediaPlayer.updateVideoSurfaces() }
            if (pendingPlayUntilSurface && currentRequest != null && !mediaPlayer.isPlaying) {
                pendingPlayUntilSurface = false
                scope.launch(Dispatchers.IO) { runCatching { mediaPlayer.play() } }
            }
            return
        }
        runCatching { mediaPlayer.detachViews() }
        currentVideoLayout = layout
        videoReattachJob?.cancel()
        videoReattachAttempts = 0
        // Habilita la Surface de subtítulos para evitar "can't get Subtitles Surface" en algunos dispositivos (MIUI),
        // incluso si el usuario mantiene los subtítulos desactivados (spuTrack = -1).
        mediaPlayer.attachViews(layout, null, true, true)
        runCatching { mediaPlayer.videoScale = videoScaleType }
        runCatching { mediaPlayer.updateVideoSurfaces() }
        if (pendingPlayUntilSurface && currentRequest != null && !mediaPlayer.isPlaying) {
            pendingPlayUntilSurface = false
            scope.launch(Dispatchers.IO) { runCatching { mediaPlayer.play() } }
        }
    }

    fun refreshVideoSurfaces() {
        runCatching { mediaPlayer.updateVideoSurfaces() }
    }

    fun setVideoScaleType(scaleType: MediaPlayer.ScaleType) {
        videoScaleType = scaleType
        runCatching { mediaPlayer.videoScale = scaleType }
        runCatching { mediaPlayer.updateVideoSurfaces() }
    }

    fun getVideoScaleType(): MediaPlayer.ScaleType = videoScaleType

    fun detachVideoLayout(layout: VLCVideoLayout) {
        if (currentVideoLayout === layout) {
            detachViewsJob?.cancel()
            val layoutSnapshot = layout
            detachViewsJob = scope.launch {
                delay(detachViewsDelayMs)
                if (currentVideoLayout !== layoutSnapshot) return@launch
                if (layoutSnapshot.isAttachedToWindow) return@launch
                runCatching { mediaPlayer.detachViews() }
                currentVideoLayout = null
            }
        }
    }

    fun release() {
        progressJob?.cancel()
        retryJob?.cancel()
        prepareJob?.cancel()
        videoReattachJob?.cancel()
        detachViewsJob?.cancel()
        earlyConnectionTimeoutJob?.cancel()
        connectionTimeoutJob?.cancel()
        pendingPlayUntilSurface = false
        runCatching { mediaPlayer.detachViews() }
        currentVideoLayout = null
        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.release() }
        runCatching { libVlc.release() }
        scope.cancel()
        updateForegroundPlayback(false)
        nextAction = null
        previousAction = null
    }

    private fun updateForegroundPlayback(isPlaying: Boolean) {
        if (isPlaying && !foregroundServiceActive) {
            val started = PlaybackForegroundService.start(appContext)
            foregroundServiceActive = started
        } else if (!isPlaying && foregroundServiceActive) {
            PlaybackForegroundService.stop(appContext)
            foregroundServiceActive = false
        }
    }

    fun retryLastRequest() {
        currentRequest?.let { playMedia(it.url, it.type, forcePrepare = true) }
    }

    fun selectAudioTrack(optionId: String) {
        val option = trackLookup[optionId] ?: return
        if (option.trackType != TrackTypes.AUDIO) return
        mediaPlayer.audioTrack = option.trackIndex
        updateTracksFromPlayer()
    }

    fun selectSubtitleTrack(optionId: String?) {
        if (optionId == null || optionId == TrackOption.SUBTITLE_OFF_ID) {
            disableSubtitles()
            return
        }
        val option = trackLookup[optionId] ?: return
        if (option.trackType != TrackTypes.TEXT) return
        mediaPlayer.spuTrack = option.trackIndex
        updateTracksFromPlayer()
    }

    fun disableSubtitles() {
        mediaPlayer.spuTrack = -1
        updateTracksFromPlayer()
    }

    private data class PlaybackRequest(val url: String, val type: PlayerType)

    private fun safePositionMs(): Long = mediaPlayer.time.coerceAtLeast(0L)

    private fun safeDurationMs(): Long = mediaPlayer.length.coerceAtLeast(0L)

    private fun updateTracksFromPlayer() {
        val audioTracks = mediaPlayer.audioTracks
            ?.filter { it.id >= 0 }
            ?.mapIndexed { index, desc ->
                TrackOption(
                    id = "audio-${desc.id}",
                    label = desc.name?.takeIf { it.isNotBlank() } ?: "Audio ${index + 1}",
                    language = null,
                    trackType = TrackTypes.AUDIO,
                    groupIndex = 0,
                    trackIndex = desc.id,
                    selected = desc.id == mediaPlayer.audioTrack
                )
            }
            .orEmpty()

        val rawText = mediaPlayer.spuTracks
            ?.filter { it.id >= 0 }
            ?.mapIndexed { index, desc ->
                TrackOption(
                    id = "text-${desc.id}",
                    label = desc.name?.takeIf { it.isNotBlank() } ?: "Subtítulo ${index + 1}",
                    language = null,
                    trackType = TrackTypes.TEXT,
                    groupIndex = 0,
                    trackIndex = desc.id,
                    selected = desc.id == mediaPlayer.spuTrack
                )
            }
            .orEmpty()

        val textTracks = if (rawText.isNotEmpty()) {
            listOf(
                TrackOption(
                    id = TrackOption.SUBTITLE_OFF_ID,
                    label = "Subtítulos desactivados",
                    language = null,
                    trackType = TrackTypes.TEXT,
                    groupIndex = -1,
                    trackIndex = -1,
                    selected = mediaPlayer.spuTrack == -1,
                    isDisableOption = true
                )
            ) + rawText
        } else {
            emptyList()
        }

        trackLookup = (audioTracks + textTracks.filter { !it.isDisableOption }).associateBy { it.id }

        val selectedAudio = audioTracks.firstOrNull { it.selected }?.id
        val selectedText = when {
            rawText.isNotEmpty() && mediaPlayer.spuTrack == -1 -> TrackOption.SUBTITLE_OFF_ID
            else -> rawText.firstOrNull { it.selected }?.id
        }

        _uiState.update {
            it.copy(
                tracks = PlaybackTracks(audio = audioTracks, text = textTracks),
                selectedAudioTrackId = selectedAudio,
                selectedSubtitleTrackId = selectedText
            )
        }
    }

    private fun handlePlaybackError(message: String) {
        if (retryCount < maxRetries && currentRequest != null) {
            retryCount++
            _uiState.update {
                it.copy(
                    isBuffering = true,
                    hasError = false,
                    errorMessage = "Reintentando... ($retryCount/$maxRetries)"
                )
            }
            scheduleRetry()
        } else {
            isIdle = true
            _uiState.update {
                it.copy(
                    hasError = true,
                    errorMessage = message,
                    isPlaying = false,
                    isBuffering = false
                )
            }
            updateForegroundPlayback(false)
        }
    }
}
