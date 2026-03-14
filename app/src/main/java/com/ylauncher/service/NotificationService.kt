package com.ylauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ylauncher.data.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        if (title.isBlank() && text.isBlank()) return

        val notification = AppNotification(
            packageName = sbn.packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            notificationKey = sbn.key,
        )
        val current = _notifications.value.toMutableMap()
        current[sbn.packageName] = notification
        _notifications.value = current
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val current = _notifications.value.toMutableMap()
        current.remove(sbn.packageName)
        _notifications.value = current
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        // Seed with existing notifications
        try {
            val current = mutableMapOf<String, AppNotification>()
            for (sbn in activeNotifications) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                if (title.isBlank() && text.isBlank()) continue
                current[sbn.packageName] = AppNotification(
                    packageName = sbn.packageName,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime,
                    notificationKey = sbn.key,
                )
            }
            _notifications.value = current
        } catch (_: Exception) { }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    companion object {
        var instance: NotificationService? = null
            private set

        private val _notifications = MutableStateFlow<Map<String, AppNotification>>(emptyMap())
        val notifications: StateFlow<Map<String, AppNotification>> = _notifications.asStateFlow()

        fun dismiss(packageName: String) {
            val key = _notifications.value[packageName]?.notificationKey ?: return
            val current = _notifications.value.toMutableMap()
            current.remove(packageName)
            _notifications.value = current
            instance?.cancelNotification(key)
        }
    }
}
