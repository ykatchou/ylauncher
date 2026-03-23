package com.ykatchou.ylauncher.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ykatchou.ylauncher.data.model.Folder
import com.ykatchou.ylauncher.data.model.FolderApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderDaoTest {

    private lateinit var database: YLauncherDatabase
    private lateinit var dao: FolderDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YLauncherDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.folderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertFolder_andVerifyViaFlow() = runTest {
        val folder = Folder(name = "Games", position = 0)

        val id = dao.insertFolder(folder)

        val folders = dao.getAllFolders().first()
        assertEquals(1, folders.size)
        assertEquals("Games", folders[0].name)
        assertEquals(id, folders[0].id)
    }

    @Test
    fun updateFolderName() = runTest {
        val id = dao.insertFolder(Folder(name = "Old Name", position = 0))

        dao.updateFolder(Folder(id = id, name = "New Name", position = 0))

        val folders = dao.getAllFolders().first()
        assertEquals(1, folders.size)
        assertEquals("New Name", folders[0].name)
    }

    @Test
    fun deleteFolder_cascadesToFolderApps() = runTest {
        val folderId = dao.insertFolder(Folder(name = "Temp", position = 0))
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.one",
                displayName = "App One",
                position = 0
            )
        )
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.two",
                displayName = "App Two",
                position = 1
            )
        )

        dao.deleteFolder(folderId)

        val folders = dao.getAllFolders().first()
        assertTrue(folders.isEmpty())
        val apps = dao.getAppsInFolderOnce(folderId)
        assertTrue(apps.isEmpty())
    }

    @Test
    fun insertFolderApps_andVerifyOrdering() = runTest {
        val folderId = dao.insertFolder(Folder(name = "Work", position = 0))
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.c",
                displayName = "C",
                position = 2
            )
        )
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.a",
                displayName = "A",
                position = 0
            )
        )
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.b",
                displayName = "B",
                position = 1
            )
        )

        val apps = dao.getAppsInFolder(folderId).first()

        assertEquals(3, apps.size)
        assertEquals("A", apps[0].displayName)
        assertEquals("B", apps[1].displayName)
        assertEquals("C", apps[2].displayName)
    }

    @Test
    fun removeSpecificFolderApp() = runTest {
        val folderId = dao.insertFolder(Folder(name = "Social", position = 0))
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.keep",
                displayName = "Keep",
                position = 0
            )
        )
        dao.insertFolderApp(
            FolderApp(
                folderId = folderId,
                packageName = "com.app.remove",
                displayName = "Remove",
                position = 1
            )
        )

        dao.removeFolderApp(folderId, "com.app.remove")

        val apps = dao.getAppsInFolderOnce(folderId)
        assertEquals(1, apps.size)
        assertEquals("com.app.keep", apps[0].packageName)
    }

    @Test
    fun multipleFolders_orderedByPosition() = runTest {
        dao.insertFolder(Folder(name = "Third", position = 2))
        dao.insertFolder(Folder(name = "First", position = 0))
        dao.insertFolder(Folder(name = "Second", position = 1))

        val folders = dao.getAllFolders().first()

        assertEquals(3, folders.size)
        assertEquals("First", folders[0].name)
        assertEquals("Second", folders[1].name)
        assertEquals("Third", folders[2].name)
    }
}
