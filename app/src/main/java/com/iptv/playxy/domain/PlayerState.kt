package com.iptv.playxy.domain

sealed class PlayerState {
    object Idle : PlayerState()
    object Playing : PlayerState()
    object Paused : PlayerState()
    object Buffering : PlayerState()
    data class Error(val message: String) : PlayerState()
}
