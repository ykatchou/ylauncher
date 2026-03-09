package com.ylauncher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import com.ylauncher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val packageManager = context.packageManager

    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refreshApps()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refreshApps()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refreshApps()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refreshApps()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refreshApps()
    }

    fun registerCallback() {
        launcherApps.registerCallback(callback)
    }

    fun unregisterCallback() {
        launcherApps.unregisterCallback(callback)
    }

    fun refreshApps() {
        val apps = mutableListOf<AppInfo>()
        for (profile in userManager.userProfiles) {
            val activities = launcherApps.getActivityList(null, profile)
            for (activity in activities) {
                apps.add(
                    AppInfo(
                        appLabel = activity.label.toString(),
                        packageName = activity.applicationInfo.packageName,
                        activityClassName = activity.componentName.className,
                        userHandle = profile,
                        icon = activity.getIcon(0),
                    )
                )
            }
        }
        apps.sort()
        _appList.value = apps
    }

    suspend fun getAppList(): List<AppInfo> = withContext(Dispatchers.IO) {
        refreshApps()
        _appList.value
    }

    fun filterApps(query: String): List<AppInfo> {
        if (query.isBlank()) return _appList.value
        val normalized = normalizeText(query.trim())
        return _appList.value.filter { app ->
            app.appLabel.contains(query.trim(), ignoreCase = true) ||
                normalizeText(app.appLabel).contains(normalized, ignoreCase = true)
        }
    }

    fun findAppByPackage(packageName: String, userHandle: UserHandle = Process.myUserHandle()): AppInfo? {
        return _appList.value.find { it.packageName == packageName && it.userHandle == userHandle }
    }

    fun resolveDefaultApp(intent: Intent): AppInfo? {
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val packageName = resolveInfo?.activityInfo?.packageName ?: return null
        return findAppByPackage(packageName)
    }

    private fun normalizeText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[-_+,. ]"), "")
    }
}
