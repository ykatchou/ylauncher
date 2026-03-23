package com.ykatchou.ylauncher.ui.components

import android.appwidget.AppWidgetManager
import android.view.ViewGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ykatchou.ylauncher.widget.LauncherWidgetHost

@Composable
fun AppWidgetContainer(
    widgetId: Int,
    widgetHost: LauncherWidgetHost,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val providerInfo = remember(widgetId) {
        try {
            appWidgetManager.getAppWidgetInfo(widgetId)
        } catch (_: Exception) { null }
    }

    if (providerInfo == null) return

    AndroidView(
        factory = {
            widgetHost.createView(context, widgetId, providerInfo).apply {
                setAppWidget(widgetId, providerInfo)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongClick() })
            },
    )
}
