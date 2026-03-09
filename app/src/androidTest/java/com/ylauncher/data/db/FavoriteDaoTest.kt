package com.ylauncher.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ylauncher.data.model.FavoriteApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var database: YLauncherDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YLauncherDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.favoriteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertFavorite_andVerifyCount() = runTest {
        val favorite = FavoriteApp(
            position = 0,
            packageName = "com.example.app",
            displayName = "Example App"
        )

        dao.insertFavorite(favorite)

        assertEquals(1, dao.count())
    }

    @Test
    fun insertMultipleFavorites_verifyOrderingByPosition() = runTest {
        val favorites = listOf(
            FavoriteApp(position = 2, packageName = "com.app.c", displayName = "App C"),
            FavoriteApp(position = 0, packageName = "com.app.a", displayName = "App A"),
            FavoriteApp(position = 1, packageName = "com.app.b", displayName = "App B"),
        )

        dao.insertAll(favorites)

        val result = dao.getAllFavoritesOnce()
        assertEquals(3, result.size)
        assertEquals("com.app.a", result[0].packageName)
        assertEquals("com.app.b", result[1].packageName)
        assertEquals("com.app.c", result[2].packageName)
    }

    @Test
    fun getAllFavorites_flowEmitsCorrectList() = runTest {
        val favorites = listOf(
            FavoriteApp(position = 0, packageName = "com.app.first", displayName = "First"),
            FavoriteApp(position = 1, packageName = "com.app.second", displayName = "Second"),
        )
        dao.insertAll(favorites)

        val result = dao.getAllFavorites().first()

        assertEquals(2, result.size)
        assertEquals("First", result[0].displayName)
        assertEquals("Second", result[1].displayName)
    }

    @Test
    fun updateFavorite_changesDisplayName() = runTest {
        val favorite = FavoriteApp(
            position = 0,
            packageName = "com.example.app",
            displayName = "Old Name"
        )
        dao.insertFavorite(favorite)

        dao.updateFavorite(favorite.copy(displayName = "New Name"))

        val result = dao.getAllFavoritesOnce()
        assertEquals(1, result.size)
        assertEquals("New Name", result[0].displayName)
    }

    @Test
    fun deleteFavoriteAt_removesCorrectEntry() = runTest {
        dao.insertAll(
            listOf(
                FavoriteApp(position = 0, packageName = "com.app.a", displayName = "A"),
                FavoriteApp(position = 1, packageName = "com.app.b", displayName = "B"),
                FavoriteApp(position = 2, packageName = "com.app.c", displayName = "C"),
            )
        )

        dao.deleteFavoriteAt(1)

        val result = dao.getAllFavoritesOnce()
        assertEquals(2, result.size)
        assertEquals("com.app.a", result[0].packageName)
        assertEquals("com.app.c", result[1].packageName)
    }

    @Test
    fun deleteAll_clearsAllFavorites() = runTest {
        dao.insertAll(
            listOf(
                FavoriteApp(position = 0, packageName = "com.app.a", displayName = "A"),
                FavoriteApp(position = 1, packageName = "com.app.b", displayName = "B"),
            )
        )
        assertEquals(2, dao.count())

        dao.deleteAll()

        assertEquals(0, dao.count())
    }

    @Test
    fun insertFavorite_withSamePosition_replacesPrevious() = runTest {
        val original = FavoriteApp(
            position = 0,
            packageName = "com.app.original",
            displayName = "Original"
        )
        dao.insertFavorite(original)

        val replacement = FavoriteApp(
            position = 0,
            packageName = "com.app.replacement",
            displayName = "Replacement"
        )
        dao.insertFavorite(replacement)

        val result = dao.getAllFavoritesOnce()
        assertEquals(1, result.size)
        assertEquals("com.app.replacement", result[0].packageName)
        assertEquals("Replacement", result[0].displayName)
    }
}
