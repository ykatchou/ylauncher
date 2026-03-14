package com.ylauncher.data.model

data class AppNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val notificationKey: String = "",
)
