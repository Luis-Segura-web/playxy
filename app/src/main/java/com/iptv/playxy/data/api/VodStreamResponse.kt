package com.iptv.playxy.data.api

import com.google.gson.annotations.SerializedName

/**
 * API response model for VOD streams
 * Fields match the JSON structure from the provider
 */
data class VodStreamResponse(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: String?,
    @SerializedName("tmdb_id") val tmdbId: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("rating_5based") val rating5Based: String?,
    @SerializedName("added") val added: String?,
    @SerializedName("is_adult") val isAdult: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("direct_source") val directSource: String?
)
