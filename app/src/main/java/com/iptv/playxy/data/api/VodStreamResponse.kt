package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for VOD streams
 * Fields match the JSON structure from the provider
 */
data class VodStreamResponse(
    @Json(name = "num") val num: Int?,
    @Json(name = "name") val name: String?,
    @Json(name = "stream_type") val streamType: String?,
    @Json(name = "stream_id") val streamId: String?,
    @Json(name = "tmdb_id") val tmdbId: String?,
    @Json(name = "stream_icon") val streamIcon: String?,
    @Json(name = "rating") val rating: String?,
    @Json(name = "rating_5based") val rating5Based: String?,
    @Json(name = "added") val added: String?,
    @Json(name = "is_adult") val isAdult: String?,
    @Json(name = "category_id") val categoryId: String?,
    @Json(name = "container_extension") val containerExtension: String?,
    @Json(name = "custom_sid") val customSid: String?,
    @Json(name = "direct_source") val directSource: String?
)
