package com.ykatchou.ylauncher.data.model

import android.os.Process
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppInfoTest {

    private val userHandle = Process.myUserHandle()

    private fun appInfo(
        label: String = "TestApp",
        pkg: String = "com.test.app",
        activity: String? = "com.test.app.MainActivity",
    ) = AppInfo(
        appLabel = label,
        packageName = pkg,
        activityClassName = activity,
        userHandle = userHandle,
    )

    // --- compareTo / sorting ---

    @Test
    fun `compareTo is case-insensitive`() {
        val lower = appInfo(label = "alpha")
        val upper = appInfo(label = "Alpha")
        assertEquals(0, lower.compareTo(upper))
    }

    @Test
    fun `compareTo orders alphabetically ignoring case`() {
        val a = appInfo(label = "banana")
        val b = appInfo(label = "Apple")
        assertTrue(a.compareTo(b) > 0)
        assertTrue(b.compareTo(a) < 0)
    }

    @Test
    fun `sorting a list of AppInfo is case-insensitive alphabetical`() {
        val apps = listOf(
            appInfo(label = "Zebra"),
            appInfo(label = "apple"),
            appInfo(label = "Mango"),
            appInfo(label = "banana"),
        )
        val sorted = apps.sorted()
        assertEquals(
            listOf("apple", "banana", "Mango", "Zebra"),
            sorted.map { it.appLabel },
        )
    }

    @Test
    fun `compareTo returns zero for identical labels`() {
        val a = appInfo(label = "Same")
        val b = appInfo(label = "Same", pkg = "com.other")
        assertEquals(0, a.compareTo(b))
    }

    // --- data class equality ---

    @Test
    fun `equal instances have same hashCode`() {
        val a = appInfo()
        val b = appInfo()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `different packageName means not equal`() {
        val a = appInfo(pkg = "com.a")
        val b = appInfo(pkg = "com.b")
        assertNotEquals(a, b)
    }

    @Test
    fun `different label means not equal`() {
        val a = appInfo(label = "App1")
        val b = appInfo(label = "App2")
        assertNotEquals(a, b)
    }

    // --- defaults ---

    @Test
    fun `icon defaults to null`() {
        val app = appInfo()
        assertNull(app.icon)
    }

    // --- toString ---

    @Test
    fun `toString contains appLabel and packageName`() {
        val app = appInfo(label = "MyApp", pkg = "com.my.app")
        val str = app.toString()
        assertTrue("toString should contain appLabel", str.contains("MyApp"))
        assertTrue("toString should contain packageName", str.contains("com.my.app"))
    }

    // --- copy ---

    @Test
    fun `copy preserves unchanged fields`() {
        val original = appInfo(label = "Original", pkg = "com.original")
        val copied = original.copy(appLabel = "Copied")
        assertEquals("Copied", copied.appLabel)
        assertEquals("com.original", copied.packageName)
        assertEquals(original.userHandle, copied.userHandle)
    }
}
