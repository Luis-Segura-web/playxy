package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for VOD streams
 * Fields match the JSON structure from the provider
 */
@JsonClass(generateAdapter = true)
data class VodStreamResponse(
    @field:Json(name = "num") val num: Any?,  // Can be Int or String
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "stream_type") val streamType: String?,
    @field:Json(name = "stream_id") val streamId: Any?,  // Can be Int or String
    @field:Json(name = "tmdb_id") val tmdbId: Any?,  // Can be Int, String, or null
    @field:Json(name = "stream_icon") val streamIcon: String?,
    @field:Json(name = "rating") val rating: Any?,  // Can be String or number
    @field:Json(name = "rating_5based") val rating5Based: Any?,  // Can be String, Double, Int
    @field:Json(name = "added") val added: Any?,  // Can be String or Long
    @field:Json(name = "is_adult") val isAdult: Any?,  // Can be String or Int
    @field:Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @field:Json(name = "container_extension") val containerExtension: String?,
    @field:Json(name = "custom_sid") val customSid: String?,
    @field:Json(name = "direct_source") val directSource: String?
)
