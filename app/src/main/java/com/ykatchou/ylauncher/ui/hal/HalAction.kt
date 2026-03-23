package com.ykatchou.ylauncher.ui.hal

enum class HalAction(val label: String) {
    NONE("Do nothing"),
    ASSISTANT("Open assistant"),
    APP_DRAWER("Open app drawer"),
    SETTINGS("Open settings"),
    LOCK_SCREEN("Lock screen"),
    NOTIFICATIONS("Notification shade"),
    CAMERA("Open camera"),
    PHONE("Open phone"),
    FLASHLIGHT("Toggle flashlight"),
    EDIT_FAVORITES("Edit favorites"),
    CHANGE_WALLPAPER("Change wallpaper"),
    CUSTOM_APP("Open app…"),
    ;

    companion object {
        fun fromKey(key: String): HalAction {
            if (key.startsWith("CUSTOM_APP:")) return CUSTOM_APP
            return entries.find { it.name == key } ?: ASSISTANT
        }

        fun encodeApp(packageName: String, activityName: String? = null): String =
            "CUSTOM_APP:$packageName:${activityName.orEmpty()}"

        fun decodeApp(key: String): Pair<String, String?>? {
            if (!key.startsWith("CUSTOM_APP:")) return null
            val parts = key.removePrefix("CUSTOM_APP:").split(":", limit = 2)
            return Pair(parts[0], parts.getOrNull(1)?.takeIf { it.isNotBlank() })
        }
    }
}
