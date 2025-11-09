package com.iptv.playxy.data.api

import com.squareup.moshi.Json

/**
 * API response model for categories
 * Fields match the JSON structure from the provider
 */
data class CategoryResponse(
    @field:Json(name = "category_id") val categoryId: String?,
    @field:Json(name = "category_name") val categoryName: String?,
    @field:Json(name = "parent_id") val parentId: String?
)
