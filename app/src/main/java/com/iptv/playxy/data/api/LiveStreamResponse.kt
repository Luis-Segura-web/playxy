package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for live streams
 * Fields match the JSON structure from the provider
 */
@JsonClass(generateAdapter = true)
data class LiveStreamResponse(
    @field:Json(name = "num") val num: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "stream_type") val streamType: String?,
    @field:Json(name = "stream_id") val streamId: String?,
    @field:Json(name = "stream_icon") val streamIcon: String?,
    @field:Json(name = "epg_channel_id") val epgChannelId: String?,
    @field:Json(name = "added") val added: String?,
    @field:Json(name = "is_adult") val isAdult: String?,
    @field:Json(name = "category_id") val categoryId: String?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "tv_archive") val tvArchive: String?,
    @field:Json(name = "direct_source") val directSource: String?,
    @field:Json(name = "tv_archive_duration") val tvArchiveDuration: String?
)
