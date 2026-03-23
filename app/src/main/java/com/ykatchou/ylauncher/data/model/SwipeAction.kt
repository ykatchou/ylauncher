package com.ykatchou.ylauncher.data.model

enum class SwipeDirection {
    LEFT, RIGHT, UP, DOWN
}

data class SwipeAction(
    val direction: SwipeDirection,
    val appName: String,
    val packageName: String,
    val activityClassName: String? = null,
    val enabled: Boolean = true,
)
