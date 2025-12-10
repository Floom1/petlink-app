package com.example.petlink.data.model

data class NotificationItem(
    val id: Int,
    val notification_type: String,
    val content: String,
    val is_read: Boolean,
    val created_at: String?,
    val application: Int?
)
