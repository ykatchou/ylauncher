package com.ykatchou.ylauncher.widget

import android.appwidget.AppWidgetHost
import android.content.Context

class LauncherWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    companion object {
        const val HOST_ID = 1024
    }
}
