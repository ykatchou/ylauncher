package com.ylauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ylauncher.data.model.FavoriteApp
import com.ylauncher.data.model.Folder
import com.ylauncher.data.model.FolderApp

@Database(
    entities = [FavoriteApp::class, Folder::class, FolderApp::class],
    version = 3,
    exportSchema = false,
)
abstract class YLauncherDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun folderDao(): FolderDao
}
