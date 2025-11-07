package com.iptv.playxy.domain

data class LiveStream(
    val streamId: String,
    val name: String,
    val streamIcon: String? = null,
    val isAdult: Boolean = false,
    val categoryId: String,
    val tvArchive: Boolean = false,
    val epgChannelId: String? = null,
    val added: String? = null,
    val customSid: String? = null,
    val directSource: String? = null,
    val tvArchiveDuration: Int = 0
)
