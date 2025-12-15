package com.iptv.playxy.data.repository

import android.content.Context
import com.iptv.playxy.domain.player.AndroidDisplayChroma
import com.iptv.playxy.domain.player.AudioOutput
import com.iptv.playxy.domain.player.DecoderMode
import com.iptv.playxy.domain.player.PlayerEngineConfig
import com.iptv.playxy.domain.player.VideoOutput
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("playxy_prefs", Context.MODE_PRIVATE)

    fun getRecentsLimit(): Int = prefs.getInt(KEY_RECENTS_LIMIT, DEFAULT_RECENTS_LIMIT)

    fun setRecentsLimit(limit: Int) {
        prefs.edit().putInt(KEY_RECENTS_LIMIT, limit).apply()
    }

    fun isParentalControlEnabled(): Boolean = prefs.getBoolean(KEY_PARENTAL_ENABLED, false)

    fun setParentalControlEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PARENTAL_ENABLED, enabled).apply()
    }

    fun getParentalPin(): String? = prefs.getString(KEY_PARENTAL_PIN, null)

    fun setParentalPin(pin: String?) {
        prefs.edit().putString(KEY_PARENTAL_PIN, pin).apply()
    }

    fun isTmdbEnabled(): Boolean = prefs.getBoolean(KEY_TMDB_ENABLED, false)

    fun setTmdbEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TMDB_ENABLED, enabled).apply()
    }

    fun getPlayerEngineConfig(): PlayerEngineConfig {
        fun <T : Enum<T>> readEnum(key: String, fallback: T, values: Array<T>): T {
            val raw = prefs.getString(key, null) ?: return fallback
            return values.firstOrNull { it.name == raw } ?: fallback
        }

        fun readDecoderMode(): DecoderMode {
            val raw = prefs.getString(KEY_PLAYER_DECODER_MODE, null) ?: return DecoderMode.SOFTWARE
            if (raw == "AUTO") return DecoderMode.SOFTWARE
            return DecoderMode.values().firstOrNull { it.name == raw } ?: DecoderMode.SOFTWARE
        }

        return PlayerEngineConfig(
            decoderMode = readDecoderMode(),
            audioOutput = readEnum(KEY_PLAYER_AUDIO_OUTPUT, AudioOutput.DEFAULT, AudioOutput.values()),
            videoOutput = readEnum(KEY_PLAYER_VIDEO_OUTPUT, VideoOutput.DEFAULT, VideoOutput.values()),
            androidDisplayChroma = readEnum(
                KEY_PLAYER_ANDROID_DISPLAY_CHROMA,
                AndroidDisplayChroma.DEFAULT,
                AndroidDisplayChroma.values()
            )
        )
    }

    fun setPlayerEngineConfig(config: PlayerEngineConfig) {
        prefs.edit()
            .putString(KEY_PLAYER_DECODER_MODE, config.decoderMode.name)
            .putString(KEY_PLAYER_AUDIO_OUTPUT, config.audioOutput.name)
            .putString(KEY_PLAYER_VIDEO_OUTPUT, config.videoOutput.name)
            .putString(KEY_PLAYER_ANDROID_DISPLAY_CHROMA, config.androidDisplayChroma.name)
            .apply()
    }

    fun getBlockedCategories(type: String): Set<String> {
        val key = when (type) {
            "live" -> KEY_BLOCKED_LIVE
            "vod" -> KEY_BLOCKED_VOD
            "series" -> KEY_BLOCKED_SERIES
            else -> KEY_BLOCKED_GENERIC
        }
        return (prefs.getStringSet(key, emptySet()) ?: emptySet()).toSet()
    }

    fun setBlockedCategories(type: String, ids: Set<String>) {
        val key = when (type) {
            "live" -> KEY_BLOCKED_LIVE
            "vod" -> KEY_BLOCKED_VOD
            "series" -> KEY_BLOCKED_SERIES
            else -> KEY_BLOCKED_GENERIC
        }
        prefs.edit().putStringSet(key, ids).apply()
    }

    companion object {
        const val DEFAULT_RECENTS_LIMIT = 15
        private const val KEY_RECENTS_LIMIT = "recents_limit"
        private const val KEY_PARENTAL_ENABLED = "parental_enabled"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_TMDB_ENABLED = "tmdb_enabled"
        private const val KEY_BLOCKED_LIVE = "blocked_live_categories"
        private const val KEY_BLOCKED_VOD = "blocked_vod_categories"
        private const val KEY_BLOCKED_SERIES = "blocked_series_categories"
        private const val KEY_BLOCKED_GENERIC = "blocked_generic_categories"
        private const val KEY_PLAYER_DECODER_MODE = "player_decoder_mode"
        private const val KEY_PLAYER_AUDIO_OUTPUT = "player_audio_output"
        private const val KEY_PLAYER_VIDEO_OUTPUT = "player_video_output"
        private const val KEY_PLAYER_ANDROID_DISPLAY_CHROMA = "player_android_display_chroma"
    }
}
