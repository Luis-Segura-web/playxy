package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for VOD streams
 * Fields match the JSON structure from the provider
 */
data class VodStreamResponse(
    @field:Json(name = "num") val num: Int?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "stream_type") val streamType: String?,
    @field:Json(name = "stream_id") val streamId: String?,
    @field:Json(name = "tmdb_id") val tmdbId: String?,
    @field:Json(name = "stream_icon") val streamIcon: String?,
    @field:Json(name = "rating") val rating: String?,
    @field:Json(name = "rating_5based") val rating5Based: String?,
    @field:Json(name = "added") val added: String?,
    @field:Json(name = "is_adult") val isAdult: String?,
    @field:Json(name = "category_id") val categoryId: String?,
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "direct_source") val directSource: String?
)
