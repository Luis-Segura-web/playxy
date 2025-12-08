package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for live streams
 * Fields match the JSON structure from the provider
 */
@JsonClass(generateAdapter = true)
data class LiveStreamResponse(
    @field:Json(name = "num") val num: Any?,  // Can be Int or String
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "stream_type") val streamType: String?,
    @field:Json(name = "stream_id") val streamId: Any?,  // Can be Int or String
    @field:Json(name = "stream_icon") val streamIcon: String?,
    @field:Json(name = "epg_channel_id") val epgChannelId: String?,
    @field:Json(name = "added") val added: Any?,  // Can be String or Long
    @field:Json(name = "is_adult") val isAdult: Any?,  // Can be String or Int
    @field:Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "tv_archive") val tvArchive: Any?,  // Can be String or Int (0/1)
    @field:Json(name = "direct_source") val directSource: String?,
    @field:Json(name = "tv_archive_duration") val tvArchiveDuration: Any?  // Can be String or Int
)
