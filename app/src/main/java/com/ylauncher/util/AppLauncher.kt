package com.ylauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle

object AppLauncher {

    fun launch(
        context: Context,
        packageName: String,
        activityClassName: String? = null,
        userHandle: UserHandle = Process.myUserHandle(),
    ): Boolean {
        if (packageName.isBlank()) return false

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

        return try {
            if (activityClassName != null) {
                val componentName = ComponentName(packageName, activityClassName)
                launcherApps.startMainActivity(componentName, userHandle, null, null)
                true
            } else {
                val activities = launcherApps.getActivityList(packageName, userHandle)
                if (activities.isNotEmpty()) {
                    launcherApps.startMainActivity(
                        activities[0].componentName,
                        userHandle,
                        null,
                        null,
                    )
                    true
                } else {
                    // Fallback: use PackageManager
                    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        context.startActivity(intent)
                        true
                    } else {
                        false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
