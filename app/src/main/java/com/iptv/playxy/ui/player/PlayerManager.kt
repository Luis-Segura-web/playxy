package com.iptv.playxy.ui.player

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.ui.PlayerView
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

private const val TAG = "PlayerManager"
private const val POSITION_UPDATE_INTERVAL_MS = 500L

@Singleton
class PlayerManager @Inject constructor(@ApplicationContext context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(25_000)
        .setReadTimeoutMs(25_000)
        .setAllowCrossProtocolRedirects(true)
        .setUserAgent("PlayXY/Media3-1.8.0")

    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

    private val trackSelector = DefaultTrackSelector(context).apply {
        // Algunos streams no informan frame rate; limitamos a 30 fps para evitar configuraciones de 1 fps en ciertos decodificadores.
        setParameters(buildUponParameters().setMaxVideoFrameRate(30))
    }

    private val dataSourceFactory = DefaultDataSource.Factory(
        context,
        httpDataSourceFactory.setTransferListener(bandwidthMeter)
    )

    private val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        .setLiveTargetOffsetMs(9_000)

    private val renderersFactory = DefaultRenderersFactory(context)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        .setEnableDecoderFallback(true)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 40_000,
            /* maxBufferMs = */ 120_000,
            /* bufferForPlaybackMs = */ 4_000,
            /* bufferForPlaybackAfterRebufferMs = */ 8_000
        )
        .setBackBuffer(0, false)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val liveSpeedControl = DefaultLivePlaybackSpeedControl.Builder()
        .setFallbackMinPlaybackSpeed(0.97f)
        .setFallbackMaxPlaybackSpeed(1.02f)
        .setMaxLiveOffsetErrorMsForUnitSpeed(1_500L)
        .build()

    private var foregroundServiceActive = false
    private var nextAction: (() -> Unit)? = null
    private var previousAction: (() -> Unit)? = null

    private val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setLivePlaybackSpeedControl(liveSpeedControl)
        .setBandwidthMeter(bandwidthMeter)
        .setMediaSourceFactory(mediaSourceFactory)
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
            setWakeMode(C.WAKE_MODE_NETWORK)
        }

    private val mediaSession = MediaSession.Builder(appContext, player)
        .setId("PlayxyMediaSession")
        .build()

    private var currentPlayerView: PlayerView? = null
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var currentRequest: PlaybackRequest? = null
    private var progressJob: Job? = null
    private var trackLookup: Map<String, TrackOption> = emptyMap()
    private var userStopped = false
    
    // Auto-retry configuration
    private var retryCount = 0
    private var retryJob: Job? = null
    private val maxRetries = 3
    private val retryDelayMs = 2000L

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _uiState.update { state ->
                    state.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        isPlaying = player.isPlaying,
                        hasError = if (playbackState == Player.STATE_IDLE) state.hasError else false
                    )
                }
                if (playbackState == Player.STATE_READY) {
                    // Reset retry count on successful playback
                    retryCount = 0
                    updateProgress()
                    userStopped = false
                } else if ((playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) && currentRequest != null && !userStopped) {
                    handleAutoRestart("Playback stopped (state=$playbackState)")
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying, isBuffering = if (isPlaying) false else it.isBuffering) }
                updateForegroundPlayback(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Playback error (retry $retryCount/$maxRetries)", error)
                
                // Attempt auto-retry
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
                    _uiState.update {
                        it.copy(
                            hasError = true,
                            errorMessage = error.localizedMessage ?: "Error de reproducción",
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                    updateForegroundPlayback(false)
                }
            }

            override fun onRenderedFirstFrame() {
                _uiState.update { it.copy(firstFrameRendered = true, isBuffering = false) }
                // Reset retry count when first frame is rendered
                retryCount = 0
            }

            override fun onTracksChanged(tracks: Tracks) {
                captureTrackInfo(tracks)
            }
        })

        startProgressUpdates()
    }
    
    private fun scheduleRetry() {
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(retryDelayMs)
            currentRequest?.let { request ->
                Log.d(TAG, "Auto-retrying playback: ${request.url}")
                playMedia(request.url, request.type, forcePrepare = true)
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
            _uiState.update {
                it.copy(
                    hasError = true,
                    errorMessage = "Reproduccion detenida",
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
        if (player.playbackState == Player.STATE_IDLE) return
        val safeDuration = player.duration.takeIf { it != C.TIME_UNSET && it >= 0 } ?: _uiState.value.durationMs
        val safePosition = player.currentPosition.coerceAtLeast(0L)
        _uiState.update {
            it.copy(
                positionMs = safePosition,
                durationMs = safeDuration,
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(safePosition).coerceAtMost(safeDuration)
            )
        }
    }

    fun playMedia(url: String, type: PlayerType = PlayerType.TV, forcePrepare: Boolean = false) {
        if (url.isBlank()) return
        
        // Cancel any pending retry when starting new playback
        retryJob?.cancel()
        userStopped = false
        
        val lastRequest = currentRequest
        if (!forcePrepare && lastRequest?.url == url && lastRequest.type == type) {
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.playWhenReady = true
            _uiState.update { it.copy(playerType = type, streamUrl = url, hasError = false) }
            return
        }

        // Reset retry count only when starting a completely new URL
        if (lastRequest?.url != url) {
            retryCount = 0
        }
        
        currentRequest = PlaybackRequest(url, type)
        player.setMediaItem(buildMediaItem(url, type))
        player.prepare()
        player.playWhenReady = true

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
    }

    private fun buildMediaItem(url: String, type: PlayerType): MediaItem {
        val builder = MediaItem.Builder().setUri(url)
        if (type == PlayerType.TV) {
            builder
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(9_000)
                        .setMinPlaybackSpeed(0.97f)
                        .setMaxPlaybackSpeed(1.03f)
                        .build()
                )
        }
        return builder.build()
    }

    fun play() {
        userStopped = false
        if (player.playbackState == Player.STATE_IDLE && currentRequest != null) {
            player.prepare()
        }
        player.playWhenReady = true
    }

    fun pause() {
        userStopped = true
        player.playWhenReady = false
    }

    fun stopPlayback() {
        // Cancel any pending retry
        retryJob?.cancel()
        retryCount = 0
        userStopped = true
        
        player.stop()
        player.clearMediaItems()
        currentRequest = null
        trackLookup = emptyMap()
        currentPlayerView?.let {
            PlayerView.switchTargetView(player, it, null)
            currentPlayerView = null
        }
        _uiState.value = PlaybackUiState()
        updateForegroundPlayback(false)
        nextAction = null
        previousAction = null
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        updateProgress()
    }

    fun seekForward(incrementMs: Long = 10_000L) {
        seekTo(player.currentPosition + incrementMs)
    }

    fun seekBackward(decrementMs: Long = 10_000L) {
        seekTo(player.currentPosition - decrementMs)
    }

    fun getCurrentPosition(): Long = player.currentPosition.coerceAtLeast(0L)

    fun getDuration(): Long = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    fun isPlaying(): Boolean = player.isPlaying

    fun hasActivePlayback(): Boolean {
        val state = _uiState.value
        return state.streamUrl != null && player.playbackState != Player.STATE_IDLE
    }

    fun setTransportActions(onNext: (() -> Unit)?, onPrevious: (() -> Unit)?) {
        nextAction = onNext
        previousAction = onPrevious
    }

    fun getPlayer(): Player = player

    fun attachPlayerView(view: PlayerView) {
        if (currentPlayerView === view) return
        PlayerView.switchTargetView(player, currentPlayerView, view)
        currentPlayerView = view
    }

    fun detachPlayerView(view: PlayerView) {
        if (currentPlayerView === view) {
            PlayerView.switchTargetView(player, currentPlayerView, null)
            currentPlayerView = null
        }
    }

    fun release() {
        progressJob?.cancel()
        retryJob?.cancel()
        currentPlayerView?.let { PlayerView.switchTargetView(player, it, null) }
        currentPlayerView = null
        player.release()
        scope.cancel()
        updateForegroundPlayback(false)
        nextAction = null
        previousAction = null
        mediaSession.release()
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
        if (option.trackType != C.TRACK_TYPE_AUDIO) return
        applyTrackOverride(option)
    }

    fun selectSubtitleTrack(optionId: String?) {
        if (optionId == null || optionId == TrackOption.SUBTITLE_OFF_ID) {
            disableSubtitles()
            return
        }
        val option = trackLookup[optionId] ?: return
        if (option.trackType != C.TRACK_TYPE_TEXT) return
        applyTrackOverride(option)
    }

    fun disableSubtitles() {
        val builder = trackSelector.parameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        trackSelector.parameters = builder.build()
        captureTrackInfo(player.currentTracks)
    }

    private fun applyTrackOverride(option: TrackOption) {
        val tracks = player.currentTracks
        if (option.groupIndex < 0 || option.trackIndex < 0) return
        val group = tracks.groups.getOrNull(option.groupIndex) ?: return
        val override = TrackSelectionOverride(group.mediaTrackGroup, listOf(option.trackIndex))
        val builder = trackSelector.parameters.buildUpon()
            .setTrackTypeDisabled(option.trackType, false)
            .clearOverridesOfType(option.trackType)
            .addOverride(override)
        trackSelector.parameters = builder.build()
        captureTrackInfo(tracks)
    }

    private fun captureTrackInfo(tracks: Tracks) {
        val audioTracks = mutableListOf<TrackOption>()
        val textTracks = mutableListOf<TrackOption>()

        tracks.groups.forEachIndexed { groupIndex, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        audioTracks.add(
                            TrackOption(
                                id = "audio-$groupIndex-$trackIndex",
                                label = format.label ?: format.language ?: "Audio ${audioTracks.size + 1}",
                                language = format.language,
                                trackType = C.TRACK_TYPE_AUDIO,
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                selected = group.isTrackSelected(trackIndex)
                            )
                        )
                    }
                }

                C.TRACK_TYPE_TEXT -> {
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        textTracks.add(
                            TrackOption(
                                id = "text-$groupIndex-$trackIndex",
                                label = format.label ?: format.language ?: "Subtítulo ${textTracks.size + 1}",
                                language = format.language,
                                trackType = C.TRACK_TYPE_TEXT,
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                selected = group.isTrackSelected(trackIndex)
                            )
                        )
                    }
                }
            }
        }

        if (textTracks.isNotEmpty()) {
            val disableSelected = trackSelector.parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
            textTracks.add(
                0,
                TrackOption(
                    id = TrackOption.SUBTITLE_OFF_ID,
                    label = "Subtítulos desactivados",
                    language = null,
                    trackType = C.TRACK_TYPE_TEXT,
                    groupIndex = -1,
                    trackIndex = -1,
                    selected = disableSelected,
                    isDisableOption = true
                )
            )
        }

        trackLookup = (audioTracks + textTracks.filter { !it.isDisableOption }).associateBy { it.id }

        val selectedAudio = audioTracks.firstOrNull { it.selected }?.id
        val selectedText = when {
            trackSelector.parameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) -> TrackOption.SUBTITLE_OFF_ID
            else -> textTracks.firstOrNull { it.selected && !it.isDisableOption }?.id
        }

        _uiState.update {
            it.copy(
                tracks = PlaybackTracks(audio = audioTracks, text = textTracks),
                selectedAudioTrackId = selectedAudio,
                selectedSubtitleTrackId = selectedText
            )
        }
    }

    private data class PlaybackRequest(val url: String, val type: PlayerType)

    private fun <T> List<T>.getOrNull(index: Int): T? = if (index in indices) this[index] else null
}
