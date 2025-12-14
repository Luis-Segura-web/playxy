package com.iptv.playxy.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
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
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

private const val TAG = "PlayerManager"
private const val POSITION_UPDATE_INTERVAL_MS = 500L

@Singleton
class PlayerManager @Inject constructor(@ApplicationContext context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext

    private var foregroundServiceActive = false
    private var nextAction: (() -> Unit)? = null
    private var previousAction: (() -> Unit)? = null

    private val libVlc = LibVLC(
        appContext,
        arrayListOf(
            "--no-video-title-show",
            "--audio-time-stretch",
            "--http-reconnect",
            "--network-caching=3500",
            "--live-caching=3000",
            "--clock-jitter=0",
            "--clock-synchro=0"
        )
    )

    private val mediaPlayer = MediaPlayer(libVlc)
    private var currentVideoLayout: VLCVideoLayout? = null

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
    private var videoReattachJob: Job? = null
    private var videoReattachAttempts: Int = 0
    private var detachViewsJob: Job? = null
    private val stopEventsToIgnore = AtomicInteger(0)
    private val maxRetries = 3
    private val retryDelayMs = 2000L
    private val detachViewsDelayMs = 900L

    init {
        mediaPlayer.setEventListener { event ->
            scope.launch {
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
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
                    }

                    MediaPlayer.Event.Paused -> {
                        _uiState.update { it.copy(isPlaying = false) }
                        updateForegroundPlayback(false)
                    }

                    MediaPlayer.Event.Vout -> {
                        if (event.voutCount > 0) {
                            videoReattachJob?.cancel()
                            videoReattachAttempts = 0
                            _uiState.update { it.copy(firstFrameRendered = true, isBuffering = false) }
                            retryCount = 0
                        } else if (mediaPlayer.isPlaying && currentRequest != null) {
                            scheduleVideoReattach("Vout=0")
                        }
                    }

                    MediaPlayer.Event.EncounteredError -> {
                        Log.e(TAG, "Playback error (retry $retryCount/$maxRetries)")
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

        startProgressUpdates()
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
        if (videoReattachAttempts >= 3) return
        if (!layoutSnapshot.isAttachedToWindow) return

        videoReattachAttempts++
        videoReattachJob?.cancel()
        videoReattachJob = scope.launch {
            delay(150L * videoReattachAttempts)
            if (currentVideoLayout !== layoutSnapshot) return@launch
            if (!layoutSnapshot.isAttachedToWindow) return@launch
            if (!mediaPlayer.isPlaying || currentRequest == null) return@launch
            Log.w(TAG, "Reatachando video layout ($reason), intento $videoReattachAttempts/3")
            runCatching { mediaPlayer.updateVideoSurfaces() }

            // Evitar reiniciar el decoder en intentos tempranos: eso provoca pantalla negra hasta el siguiente keyframe.
            // Si sigue sin vout tras varios intentos, como último recurso recreamos las views.
            if (videoReattachAttempts >= 3) {
                runCatching { mediaPlayer.detachViews() }
                runCatching { mediaPlayer.attachViews(layoutSnapshot, null, false, true) }
                runCatching { mediaPlayer.updateVideoSurfaces() }
            }
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

    fun playMedia(url: String, type: PlayerType = PlayerType.TV, forcePrepare: Boolean = false) {
        if (url.isBlank()) return

        retryJob?.cancel()
        prepareJob?.cancel()
        userStopped = false

        val lastRequest = currentRequest
        if (!forcePrepare && lastRequest?.url == url && lastRequest.type == type) {
            if (!mediaPlayer.isPlaying) {
                isIdle = false
                scope.launch(Dispatchers.IO) { runCatching { mediaPlayer.play() } }
            }
            _uiState.update { it.copy(playerType = type, streamUrl = url, hasError = false) }
            return
        }

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
                mediaPlayer.play()
            }.onFailure { throwable ->
                Log.e(TAG, "Error al preparar reproducción", throwable)
                withContext(Dispatchers.Main.immediate) {
                    handlePlaybackError("No se pudo iniciar la reproducción")
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
            addOption(":network-caching=${if (type == PlayerType.TV) 3000 else 4500}")
            addOption(":live-caching=${if (type == PlayerType.TV) 2500 else 4500}")
            addOption(":clock-jitter=0")
            addOption(":clock-synchro=0")
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
        retryCount = 0
        userStopped = true

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
            runCatching { mediaPlayer.updateVideoSurfaces() }
            return
        }
        runCatching { mediaPlayer.detachViews() }
        currentVideoLayout = layout
        videoReattachJob?.cancel()
        videoReattachAttempts = 0
        mediaPlayer.attachViews(layout, null, false, true)
        runCatching { mediaPlayer.updateVideoSurfaces() }
    }

    fun refreshVideoSurfaces() {
        runCatching { mediaPlayer.updateVideoSurfaces() }
    }

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
