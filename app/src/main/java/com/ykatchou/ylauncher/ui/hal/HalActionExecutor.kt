package com.ykatchou.ylauncher.ui.hal

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import com.ykatchou.ylauncher.util.AppLauncher
import com.ykatchou.ylauncher.util.FlashlightHelper
import com.ykatchou.ylauncher.util.expandNotificationDrawer
import com.ykatchou.ylauncher.util.openCameraApp
import com.ykatchou.ylauncher.util.openDialerApp
import com.ykatchou.ylauncher.util.showToast

object HalActionExecutor {

    fun execute(
        context: Context,
        actionKey: String,
        assistantPackage: String = "",
        onOpenDrawer: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onEditFavorites: () -> Unit = {},
    ) {
        val action = HalAction.fromKey(actionKey)
        when (action) {
            HalAction.NONE -> {}
            HalAction.APP_DRAWER -> onOpenDrawer()
            HalAction.SETTINGS -> onOpenSettings()
            HalAction.EDIT_FAVORITES -> onEditFavorites()
            HalAction.ASSISTANT -> {
                if (assistantPackage.isNotBlank() && AppLauncher.launch(context, assistantPackage)) return
                val intent = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { context.startActivity(intent) } catch (_: Exception) {
                    context.showToast("No assistant found")
                }
            }
            HalAction.NOTIFICATIONS -> context.expandNotificationDrawer()
            HalAction.CAMERA -> context.openCameraApp()
            HalAction.PHONE -> context.openDialerApp()
            HalAction.FLASHLIGHT -> FlashlightHelper.toggle(context)
            HalAction.CHANGE_WALLPAPER -> {
                try {
                    context.startActivity(
                        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (_: Exception) {
                    try {
                        context.startActivity(
                            Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                        context.showToast("No wallpaper picker found")
                    }
                }
            }
            HalAction.CUSTOM_APP -> {
                val decoded = HalAction.decodeApp(actionKey)
                if (decoded != null) {
                    val launched = AppLauncher.launch(context, decoded.first, decoded.second)
                    if (!launched) context.showToast("App not found")
                }
            }
        }
    }
}
