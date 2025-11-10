package com.iptv.playxy.domain

data class Category(
    val categoryId: String,
    val categoryName: String,
    val parentId: String,
    val orderIndex: Int = 0
)
