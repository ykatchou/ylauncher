package com.ykatchou.ylauncher

import android.app.Application
import com.ykatchou.ylauncher.util.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YLauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
        )
    }
}
