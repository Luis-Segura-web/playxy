package com.iptv.playxy.domain.player

data class PlayerEngineConfig(
    val decoderMode: DecoderMode = DecoderMode.SOFTWARE,
    val audioOutput: AudioOutput = AudioOutput.DEFAULT,
    val videoOutput: VideoOutput = VideoOutput.DEFAULT,
    val androidDisplayChroma: AndroidDisplayChroma = AndroidDisplayChroma.DEFAULT
) {
    fun toLibVlcOptions(): ArrayList<String> {
        val options = arrayListOf(
            "--no-video-title-show",
            "--audio-time-stretch",
            "--http-reconnect",
            "--clock-jitter=0",
            "--clock-synchro=0"
        )

        when (decoderMode) {
            DecoderMode.HARDWARE -> options += "--avcodec-hw=any"
            DecoderMode.SOFTWARE -> options += "--avcodec-hw=none"
        }

        when (audioOutput) {
            AudioOutput.DEFAULT -> Unit
            AudioOutput.OPENSL_ES -> options += "--aout=opensles"
        }

        when (videoOutput) {
            VideoOutput.DEFAULT -> Unit
            VideoOutput.OPENGL_GLES2 -> options += "--vout=gles2"
        }

        when (androidDisplayChroma) {
            AndroidDisplayChroma.DEFAULT -> Unit
            AndroidDisplayChroma.RV32 -> options += "--android-display-chroma=RV32"
            AndroidDisplayChroma.RGB16 -> options += "--android-display-chroma=RGB16"
        }

        return options
    }
}

enum class DecoderMode { HARDWARE, SOFTWARE }

enum class AudioOutput { DEFAULT, OPENSL_ES }

enum class VideoOutput { DEFAULT, OPENGL_GLES2 }

enum class AndroidDisplayChroma { DEFAULT, RV32, RGB16 }
