package com.ylauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteAppTest {

    private fun favoriteApp(
        position: Int = 0,
        pkg: String = "com.example.app",
        activity: String? = null,
        displayName: String = "Example",
        userHandleString: String = "",
    ) = FavoriteApp(position, pkg, activity, displayName, userHandleString)

    // --- construction & defaults ---

    @Test
    fun `default activityClassName is null`() {
        val fav = favoriteApp()
        assertNull(fav.activityClassName)
    }

    @Test
    fun `default userHandleString is empty`() {
        val fav = favoriteApp()
        assertEquals("", fav.userHandleString)
    }

    @Test
    fun `all fields are correctly assigned`() {
        val fav = FavoriteApp(
            position = 3,
            packageName = "com.test",
            activityClassName = "com.test.Main",
            displayName = "Test App",
            userHandleString = "user_0",
        )
        assertEquals(3, fav.position)
        assertEquals("com.test", fav.packageName)
        assertEquals("com.test.Main", fav.activityClassName)
        assertEquals("Test App", fav.displayName)
        assertEquals("user_0", fav.userHandleString)
    }

    // --- equality ---

    @Test
    fun `equal instances are equal`() {
        val a = favoriteApp(position = 1, pkg = "com.a", displayName = "A")
        val b = favoriteApp(position = 1, pkg = "com.a", displayName = "A")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `different position means not equal`() {
        val a = favoriteApp(position = 0)
        val b = favoriteApp(position = 1)
        assertNotEquals(a, b)
    }

    @Test
    fun `different packageName means not equal`() {
        val a = favoriteApp(pkg = "com.a")
        val b = favoriteApp(pkg = "com.b")
        assertNotEquals(a, b)
    }

    @Test
    fun `different displayName means not equal`() {
        val a = favoriteApp(displayName = "App A")
        val b = favoriteApp(displayName = "App B")
        assertNotEquals(a, b)
    }

    @Test
    fun `different userHandleString means not equal`() {
        val a = favoriteApp(userHandleString = "user_0")
        val b = favoriteApp(userHandleString = "user_10")
        assertNotEquals(a, b)
    }

    // --- copy ---

    @Test
    fun `copy preserves unchanged fields`() {
        val original = favoriteApp(position = 2, pkg = "com.orig", displayName = "Orig")
        val copied = original.copy(displayName = "Updated")
        assertEquals("Updated", copied.displayName)
        assertEquals(2, copied.position)
        assertEquals("com.orig", copied.packageName)
    }
}
