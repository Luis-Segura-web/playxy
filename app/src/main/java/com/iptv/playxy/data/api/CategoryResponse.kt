package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * API response model for categories
 * Fields match the JSON structure from the provider
 */
@JsonClass(generateAdapter = true)
data class CategoryResponse(
    @field:Json(name = "category_id") val categoryId: Any?,  // Can be String or Int
    @field:Json(name = "category_name") val categoryName: String?,
    @field:Json(name = "parent_id") val parentId: Any?  // Can be String or Int
)
