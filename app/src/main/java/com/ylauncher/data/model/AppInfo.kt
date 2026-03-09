package com.ylauncher.data.model

import android.graphics.drawable.Drawable
import android.os.UserHandle

data class AppInfo(
    val appLabel: String,
    val packageName: String,
    val activityClassName: String?,
    val userHandle: UserHandle,
    val icon: Drawable? = null,
) : Comparable<AppInfo> {
    override fun compareTo(other: AppInfo): Int =
        appLabel.compareTo(other.appLabel, ignoreCase = true)
}
