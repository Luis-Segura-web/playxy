package com.iptv.playxy.data.repository

import android.content.Context
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

    fun getBlockedCategories(type: String): Set<String> {
        val key = when (type) {
            "live" -> KEY_BLOCKED_LIVE
            "vod" -> KEY_BLOCKED_VOD
            "series" -> KEY_BLOCKED_SERIES
            else -> KEY_BLOCKED_GENERIC
        }
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
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
        private const val KEY_BLOCKED_LIVE = "blocked_live_categories"
        private const val KEY_BLOCKED_VOD = "blocked_vod_categories"
        private const val KEY_BLOCKED_SERIES = "blocked_series_categories"
        private const val KEY_BLOCKED_GENERIC = "blocked_generic_categories"
    }
}
