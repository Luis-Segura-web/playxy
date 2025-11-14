package com.iptv.playxy.util

import com.iptv.playxy.domain.LiveStream
import com.iptv.playxy.domain.UserProfile
import com.iptv.playxy.domain.VodStream

object StreamUrlBuilder {

    /**
     * Build the streaming URL for a live channel
     * Format: http://url:port/live/username/password/stream_id.ext
     */
    fun buildLiveStreamUrl(profile: UserProfile, stream: LiveStream, extension: String = "ts"): String {
        // If directSource is provided, use it
        if (!stream.directSource.isNullOrEmpty()) {
            return stream.directSource
        }

        // Build URL from profile and stream ID
        val baseUrl = profile.url.trimEnd('/')
        return "$baseUrl/live/${profile.username}/${profile.password}/${stream.streamId}.$extension"
    }

    /**
     * Build the streaming URL for a VOD stream
     * Format: http://url:port/movie/username/password/stream_id.ext
     */
    fun buildVodStreamUrl(profile: UserProfile, stream: VodStream): String {
        // If directSource is provided, use it
        if (!stream.directSource.isNullOrEmpty()) {
            return stream.directSource
        }

        // Build URL from profile and stream ID
        val baseUrl = profile.url.trimEnd('/')
        val extension = stream.containerExtension.ifEmpty { "mp4" }
        return "$baseUrl/movie/${profile.username}/${profile.password}/${stream.streamId}.$extension"
    }

    /**
     * Build the streaming URL for series episode
     * Format: http://url:port/series/username/password/stream_id.ext
     */
    fun buildSeriesStreamUrl(profile: UserProfile, streamId: String, extension: String = "mp4"): String {
        val baseUrl = profile.url.trimEnd('/')
        return "$baseUrl/series/${profile.username}/${profile.password}/$streamId.$extension"
    }
}

