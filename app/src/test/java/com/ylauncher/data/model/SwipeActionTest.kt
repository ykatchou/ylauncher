package com.ylauncher.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeActionTest {

    private fun swipeAction(
        direction: SwipeDirection = SwipeDirection.LEFT,
        appName: String = "Browser",
        pkg: String = "com.browser",
        activity: String? = null,
        enabled: Boolean = true,
    ) = SwipeAction(direction, appName, pkg, activity, enabled)

    // --- SwipeDirection enum ---

    @Test
    fun `SwipeDirection has exactly four values`() {
        val values = SwipeDirection.entries
        assertEquals(4, values.size)
    }

    @Test
    fun `SwipeDirection contains all expected directions`() {
        val names = SwipeDirection.entries.map { it.name }.toSet()
        assertEquals(setOf("LEFT", "RIGHT", "UP", "DOWN"), names)
    }

    @Test
    fun `SwipeDirection valueOf round-trips`() {
        SwipeDirection.entries.forEach { dir ->
            assertEquals(dir, SwipeDirection.valueOf(dir.name))
        }
    }

    // --- defaults ---

    @Test
    fun `enabled defaults to true`() {
        val action = swipeAction()
        assertTrue(action.enabled)
    }

    @Test
    fun `activityClassName defaults to null`() {
        val action = swipeAction()
        assertNull(action.activityClassName)
    }

    // --- equality ---

    @Test
    fun `equal instances are equal`() {
        val a = swipeAction(direction = SwipeDirection.UP, appName = "Maps", pkg = "com.maps")
        val b = swipeAction(direction = SwipeDirection.UP, appName = "Maps", pkg = "com.maps")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `different direction means not equal`() {
        val a = swipeAction(direction = SwipeDirection.LEFT)
        val b = swipeAction(direction = SwipeDirection.RIGHT)
        assertNotEquals(a, b)
    }

    @Test
    fun `different enabled flag means not equal`() {
        val a = swipeAction(enabled = true)
        val b = swipeAction(enabled = false)
        assertNotEquals(a, b)
    }

    @Test
    fun `different packageName means not equal`() {
        val a = swipeAction(pkg = "com.a")
        val b = swipeAction(pkg = "com.b")
        assertNotEquals(a, b)
    }

    // --- copy ---

    @Test
    fun `copy changes only specified fields`() {
        val original = swipeAction(direction = SwipeDirection.DOWN, appName = "Phone", pkg = "com.phone")
        val copied = original.copy(enabled = false)
        assertFalse(copied.enabled)
        assertEquals(SwipeDirection.DOWN, copied.direction)
        assertEquals("Phone", copied.appName)
        assertEquals("com.phone", copied.packageName)
    }

    @Test
    fun `copy can change direction`() {
        val original = swipeAction(direction = SwipeDirection.LEFT)
        val copied = original.copy(direction = SwipeDirection.RIGHT)
        assertEquals(SwipeDirection.RIGHT, copied.direction)
        assertEquals(original.appName, copied.appName)
    }
}
