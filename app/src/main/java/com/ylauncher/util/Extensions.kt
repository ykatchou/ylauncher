package com.ylauncher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.lang.reflect.Method

fun Context.openDialerApp() {
    try {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        showToast("No dialer app found")
    }
}

fun Context.openCameraApp() {
    try {
        val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    } catch (e: Exception) {
        showToast("No camera app found")
    }
}

fun Context.expandNotificationDrawer() {
    try {
        val statusBarService = getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expand: Method = statusBarManager.getMethod("expandNotificationsPanel")
        expand.invoke(statusBarService)
    } catch (e: Exception) {
        // Silently fail
    }
}

fun Context.openSearch(query: String? = null) {
    try {
        val url = "https://duck.co/?q=${query?.replace(" ", "%20") ?: ""}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        showToast("No browser found")
    }
}

fun Context.openAppInfo(packageName: String) {
    try {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Cannot open app info")
    }
}

fun Context.uninstallApp(packageName: String) {
    try {
        val intent = Intent(Intent.ACTION_DELETE)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Cannot uninstall app")
    }
}

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
