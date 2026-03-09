package com.ylauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderTest {

    // --- Folder ---

    @Test
    fun `Folder id defaults to 0`() {
        val folder = Folder(name = "Games", position = 0)
        assertEquals(0L, folder.id)
    }

    @Test
    fun `Folder isExpanded defaults to false`() {
        val folder = Folder(name = "Work", position = 1)
        assertFalse(folder.isExpanded)
    }

    @Test
    fun `Folder all fields assigned correctly`() {
        val folder = Folder(id = 5, name = "Social", position = 2, isExpanded = true)
        assertEquals(5L, folder.id)
        assertEquals("Social", folder.name)
        assertEquals(2, folder.position)
        assertEquals(true, folder.isExpanded)
    }

    @Test
    fun `Folder equality`() {
        val a = Folder(id = 1, name = "A", position = 0)
        val b = Folder(id = 1, name = "A", position = 0)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `Folder inequality on different name`() {
        val a = Folder(id = 1, name = "A", position = 0)
        val b = Folder(id = 1, name = "B", position = 0)
        assertNotEquals(a, b)
    }

    @Test
    fun `Folder inequality on different position`() {
        val a = Folder(name = "A", position = 0)
        val b = Folder(name = "A", position = 1)
        assertNotEquals(a, b)
    }

    @Test
    fun `Folder copy changes only specified fields`() {
        val original = Folder(id = 3, name = "Utils", position = 1, isExpanded = false)
        val copied = original.copy(isExpanded = true)
        assertEquals(true, copied.isExpanded)
        assertEquals("Utils", copied.name)
        assertEquals(3L, copied.id)
        assertEquals(1, copied.position)
    }

    // --- FolderApp ---

    @Test
    fun `FolderApp construction with all fields`() {
        val app = FolderApp(
            folderId = 10,
            packageName = "com.app",
            activityClassName = "com.app.Main",
            displayName = "App",
            position = 0,
            userHandleString = "user_0",
        )
        assertEquals(10L, app.folderId)
        assertEquals("com.app", app.packageName)
        assertEquals("com.app.Main", app.activityClassName)
        assertEquals("App", app.displayName)
        assertEquals(0, app.position)
        assertEquals("user_0", app.userHandleString)
    }

    @Test
    fun `FolderApp activityClassName defaults to null`() {
        val app = FolderApp(
            folderId = 1,
            packageName = "com.test",
            displayName = "Test",
            position = 0,
        )
        assertNull(app.activityClassName)
    }

    @Test
    fun `FolderApp userHandleString defaults to empty`() {
        val app = FolderApp(
            folderId = 1,
            packageName = "com.test",
            displayName = "Test",
            position = 0,
        )
        assertEquals("", app.userHandleString)
    }

    @Test
    fun `FolderApp equality`() {
        val a = FolderApp(folderId = 1, packageName = "com.a", displayName = "A", position = 0)
        val b = FolderApp(folderId = 1, packageName = "com.a", displayName = "A", position = 0)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `FolderApp inequality on different folderId`() {
        val a = FolderApp(folderId = 1, packageName = "com.a", displayName = "A", position = 0)
        val b = FolderApp(folderId = 2, packageName = "com.a", displayName = "A", position = 0)
        assertNotEquals(a, b)
    }

    @Test
    fun `FolderApp copy preserves unchanged fields`() {
        val original = FolderApp(folderId = 5, packageName = "com.orig", displayName = "Orig", position = 2)
        val copied = original.copy(position = 3)
        assertEquals(3, copied.position)
        assertEquals(5L, copied.folderId)
        assertEquals("com.orig", copied.packageName)
        assertEquals("Orig", copied.displayName)
    }
}
