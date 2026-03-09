package com.ylauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_apps")
data class FavoriteApp(
    @PrimaryKey
    val position: Int,
    val packageName: String,
    val activityClassName: String? = null,
    val displayName: String,
    val userHandleString: String = "",
    val folderId: Long? = null,
    val iconEmoji: String? = null,
) {
    val isFolder: Boolean get() = folderId != null
}
