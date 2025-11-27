package com.iptv.playxy.domain

data class UserProfile(
    val id: Int = 1,
    val profileName: String,
    val username: String,
    val password: String,
    val url: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val expiry: Long? = null,
    val maxConnections: Int? = null,
    val status: String? = null
)
