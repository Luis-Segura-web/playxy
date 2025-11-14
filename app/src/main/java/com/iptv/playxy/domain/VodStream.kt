package com.iptv.playxy.domain

data class VodStream(
    val streamId: String,
    val name: String,
    val streamIcon: String? = null,
    val tmdbId: String? = null,
    val rating: Float = 0f,
    val rating5Based: Float = 0f,
    val containerExtension: String,
    val added: String? = null,
    val isAdult: Boolean = false,
    val categoryId: String,
    val customSid: String? = null,
    val directSource: String? = null
)
