package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for live streams
 * Fields match the JSON structure from the provider
 */
data class LiveStreamResponse(
    @Json(name = "num") val num: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "stream_type") val streamType: String?,
    @Json(name = "stream_id") val streamId: String?,
    @Json(name = "stream_icon") val streamIcon: String?,
    @Json(name = "epg_channel_id") val epgChannelId: String?,
    @Json(name = "added") val added: String?,
    @Json(name = "is_adult") val isAdult: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "tv_archive") val tvArchive: String?,
    @Json(name = "direct_source") val directSource: String?,
    @Json(name = "tv_archive_duration") val tvArchiveDuration: String?
)
