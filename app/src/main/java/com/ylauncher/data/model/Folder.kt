package com.ylauncher.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val position: Int,
    val isExpanded: Boolean = false,
    val iconEmoji: String = "📁",
)

@Entity(
    tableName = "folder_apps",
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("folderId")],
    primaryKeys = ["folderId", "packageName"],
)
data class FolderApp(
    val folderId: Long,
    val packageName: String,
    val activityClassName: String? = null,
    val displayName: String,
    val position: Int,
    val userHandleString: String = "",
)
