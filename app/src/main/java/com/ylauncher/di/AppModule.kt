package com.ylauncher.di

import android.content.Context
import androidx.room.Room
import com.ylauncher.data.db.FavoriteDao
import com.ylauncher.data.db.FolderDao
import com.ylauncher.data.db.YLauncherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): YLauncherDatabase {
        return Room.databaseBuilder(
            context,
            YLauncherDatabase::class.java,
            "ylauncher.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideFavoriteDao(database: YLauncherDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideFolderDao(database: YLauncherDatabase): FolderDao {
        return database.folderDao()
    }
}
