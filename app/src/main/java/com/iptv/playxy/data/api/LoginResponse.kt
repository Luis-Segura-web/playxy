package com.iptv.playxy.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @field:Json(name = "user_info") val userInfo: UserInfoResponse?,
    @field:Json(name = "server_info") val serverInfo: ServerInfoResponse?
)

@JsonClass(generateAdapter = true)
data class UserInfoResponse(
    @field:Json(name = "status") val status: String?,
    @field:Json(name = "exp_date") val expDate: String?,
    @field:Json(name = "max_connections") val maxConnections: String?,
    @field:Json(name = "active_cons") val activeConnections: String?
)

@JsonClass(generateAdapter = true)
data class ServerInfoResponse(
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "port") val port: String?
)
