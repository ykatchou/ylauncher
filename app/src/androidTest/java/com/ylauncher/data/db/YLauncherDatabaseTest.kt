package com.ylauncher.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YLauncherDatabaseTest {

    private lateinit var database: YLauncherDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YLauncherDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseCreationSucceeds() {
        assertNotNull(database)
        assertTrue(database.isOpen)
    }

    @Test
    fun favoriteDaoIsAccessible() {
        val dao = database.favoriteDao()
        assertNotNull(dao)
    }

    @Test
    fun folderDaoIsAccessible() {
        val dao = database.folderDao()
        assertNotNull(dao)
    }

    @Test
    fun databaseVersionIsOne() {
        val version = database.openHelper.readableDatabase.version
        assertEquals(1, version)
    }
}
