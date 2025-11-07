package com.iptv.playxy.data.api

import com.google.gson.annotations.SerializedName

/**
 * API response model for live streams
 * Fields match the JSON structure from the provider
 */
data class LiveStreamResponse(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    @SerializedName("added") val added: String?,
    @SerializedName("is_adult") val isAdult: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("tv_archive") val tvArchive: String?,
    @SerializedName("direct_source") val directSource: String?,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: String?
)
