package com.iptv.playxy.data.api

import com.google.gson.annotations.SerializedName

/**
 * API response model for categories
 * Fields match the JSON structure from the provider
 */
data class CategoryResponse(
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("category_name") val categoryName: String?,
    @SerializedName("parent_id") val parentId: String?
)
