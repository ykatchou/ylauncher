package com.ykatchou.ylauncher.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ykatchou.ylauncher.data.db.FavoriteDao
import com.ykatchou.ylauncher.data.db.FolderDao
import com.ykatchou.ylauncher.data.db.PanelDao
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

/**
 * Promotes panels from a positional-index concept (name/HAL config stored as
 * delimited strings in DataStore, keyed by list position) to a real `panels` table
 * with a stable id, so favorite_apps.panelId can be a proper foreign key with cascade
 * delete. Seeds one Panel row per distinct panelId already referenced by favorite_apps
 * (id = old panelId) so every existing favorite keeps its current panel with zero data
 * movement; placeholder names are reconciled with the user's real panel names from
 * DataStore in a one-time app-level pass after this migration runs (see
 * PrefsRepository/HomeViewModel), since DataStore can't be read synchronously here.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS panels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                position INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO panels (id, name, position)
            SELECT DISTINCT panelId, 'Panel ' || panelId, panelId FROM favorite_apps
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO panels (id, name, position) SELECT 0, 'Perso', 0 WHERE NOT EXISTS (SELECT 1 FROM panels)"
        )
        db.execSQL(
            "INSERT INTO panels (id, name, position) SELECT 1, 'Pro', 1 WHERE NOT EXISTS (SELECT 1 FROM panels WHERE id = 1)"
        )

        // Rebuild favorite_apps with a real FK to panels (SQLite can't ALTER a column to add a FK).
        db.execSQL(
            """
            CREATE TABLE favorite_apps_new (
                position INTEGER NOT NULL PRIMARY KEY,
                packageName TEXT NOT NULL,
                activityClassName TEXT,
                displayName TEXT NOT NULL,
                userHandleString TEXT NOT NULL DEFAULT '',
                folderId INTEGER,
                iconEmoji TEXT,
                panelId INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(panelId) REFERENCES panels(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO favorite_apps_new
            (position, packageName, activityClassName, displayName, userHandleString, folderId, iconEmoji, panelId)
            SELECT position, packageName, activityClassName, displayName, userHandleString, folderId, iconEmoji, panelId
            FROM favorite_apps
            """.trimIndent()
        )
        db.execSQL("DROP TABLE favorite_apps")
        db.execSQL("ALTER TABLE favorite_apps_new RENAME TO favorite_apps")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_favorite_apps_panelId ON favorite_apps(panelId)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE panels ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1")
    }
}

/**
 * `panels` must never be empty: `favorite_apps.panelId` is a foreign key onto it, so with no
 * panel row *every* favorite write fails with SQLITE_CONSTRAINT_FOREIGNKEY. Three paths can
 * leave it empty — a brand-new install (creates the schema straight at the latest version, so
 * no migration runs and only MIGRATION_3_4 ever seeds a panel), a destructive migration
 * (drops and recreates every table without going through onCreate), and any future state that
 * wipes the table. Seeding on create, on destructive migration *and* on every open covers all
 * three; the statement is a single idempotent no-op when a panel already exists.
 */
private const val SEED_DEFAULT_PANEL_SQL =
    "INSERT INTO panels (id, name, position, enabled) " +
        "SELECT 0, 'Perso', 0, 1 WHERE NOT EXISTS (SELECT 1 FROM panels)"

private val SEED_DEFAULT_PANEL_CALLBACK = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(SEED_DEFAULT_PANEL_SQL)
    }

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        db.execSQL(SEED_DEFAULT_PANEL_SQL)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        db.execSQL(SEED_DEFAULT_PANEL_SQL)
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
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(SEED_DEFAULT_PANEL_CALLBACK)
            .fallbackToDestructiveMigration(dropAllTables = true)
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
    fun providePanelDao(database: YLauncherDatabase): PanelDao {
        return database.panelDao()
    }

    @Provides
    @Singleton
    fun provideWidgetHost(@ApplicationContext context: Context): LauncherWidgetHost {
        return LauncherWidgetHost(context)
    }
}
