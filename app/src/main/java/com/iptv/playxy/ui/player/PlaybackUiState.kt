package com.iptv.playxy.ui.player

data class PlaybackUiState(
    val streamUrl: String? = null,
    val playerType: PlayerType = PlayerType.TV,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val firstFrameRendered: Boolean = false,
    val tracks: PlaybackTracks = PlaybackTracks(),
    val selectedAudioTrackId: String? = null,
    val selectedSubtitleTrackId: String? = null
)

data class PlaybackTracks(
    val audio: List<TrackOption> = emptyList(),
    val text: List<TrackOption> = emptyList()
) {
    val hasDialogOptions: Boolean
        get() = audio.size > 1 || text.size > 1

    companion object {
        fun empty() = PlaybackTracks()
    }
}

data class TrackOption(
    val id: String,
    val label: String,
    val language: String?,
    val trackType: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val selected: Boolean,
    val isDisableOption: Boolean = false
) {
    companion object {
        const val SUBTITLE_OFF_ID = "subtitle-off"
    }
}

fun PlaybackTracks.subtitleOptions(): List<TrackOption> = text.filter { !it.isDisableOption }

fun PlaybackTracks.hasSelectableTracks(): Boolean = audio.size > 1 || text.size > 1
