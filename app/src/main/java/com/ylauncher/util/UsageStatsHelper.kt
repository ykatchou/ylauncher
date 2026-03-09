package com.ylauncher.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import com.ylauncher.data.model.AppInfo
import com.ylauncher.data.repository.AppRepository

object UsageStatsHelper {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Returns the top N most-used app package names from the last [days] days,
     * excluding system launchers and ourselves.
     */
    fun getTopApps(
        context: Context,
        appRepository: AppRepository,
        count: Int = 8,
        days: Int = 30,
    ): List<AppInfo> {
        if (!hasPermission(context)) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days.toLong() * 24 * 60 * 60 * 1000)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime,
        )

        if (stats.isNullOrEmpty()) return emptyList()

        val excludedPackages = setOf(
            context.packageName,
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "bitpit.launcher",
            "bitpit.launcher.pro",
        )

        // Aggregate total foreground time per package
        val usageByPackage = mutableMapOf<String, Long>()
        for (stat in stats) {
            if (stat.packageName in excludedPackages) continue
            if (stat.totalTimeInForeground <= 0) continue
            usageByPackage[stat.packageName] =
                (usageByPackage[stat.packageName] ?: 0) + stat.totalTimeInForeground
        }

        // Sort by usage time descending, resolve to AppInfo
        return usageByPackage.entries
            .sortedByDescending { it.value }
            .mapNotNull { (pkg, _) -> appRepository.findAppByPackage(pkg) }
            .take(count)
    }

    /**
     * Returns the most recently used apps (by last time used), excluding
     * system launchers, ourselves, and the given set of excluded packages.
     */
    fun getRecentApps(
        context: Context,
        appRepository: AppRepository,
        count: Int = 3,
        excludePackages: Set<String> = emptySet(),
    ): List<AppInfo> {
        if (!hasPermission(context)) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24L * 60 * 60 * 1000) // last 24 hours

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime,
        )

        if (stats.isNullOrEmpty()) return emptyList()

        val systemExcluded = setOf(
            context.packageName,
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "bitpit.launcher",
            "bitpit.launcher.pro",
        )

        val allExcluded = systemExcluded + excludePackages

        // Pick most recently used, deduplicated
        return stats
            .filter { it.packageName !in allExcluded && it.lastTimeUsed > 0 && it.totalTimeInForeground > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .distinctBy { it.packageName }
            .mapNotNull { appRepository.findAppByPackage(it.packageName) }
            .take(count)
    }
}
