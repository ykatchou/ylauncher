package com.ykatchou.ylauncher.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
import com.ykatchou.ylauncher.data.db.YLauncherDatabase
import com.ykatchou.ylauncher.widget.LauncherWidgetHost
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE favorite_apps ADD COLUMN panelId INTEGER NOT NULL DEFAULT 0")
    }
}

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
        )
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavoriteDao(database: YLauncherDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideFolderDao(database: YLauncherDatabase): FolderDao {
        return database.folderDao()
    }

    @Provides
    @Singleton
    fun provideWidgetHost(@ApplicationContext context: Context): LauncherWidgetHost {
        return LauncherWidgetHost(context)
    }
}
