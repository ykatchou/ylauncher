package com.ykatchou.ylauncher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ykatchou.ylauncher.data.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        reseedNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        reseedNotifications()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        reseedNotifications()
    }

    private fun reseedNotifications() {
        try {
            // Group by package to get counts and pick the most recent notification
            val grouped = activeNotifications
                .filter { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras.getCharSequence("android.title")?.toString() ?: ""
                    val text = extras.getCharSequence("android.text")?.toString() ?: ""
                    title.isNotBlank() || text.isNotBlank()
                }
                .groupBy { it.packageName }

            val current = mutableMapOf<String, AppNotification>()
            for ((pkg, sbns) in grouped) {
                val latest = sbns.maxBy { it.postTime }
                val extras = latest.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                current[pkg] = AppNotification(
                    packageName = pkg,
                    title = title,
                    text = text,
                    timestamp = latest.postTime,
                    notificationKey = latest.key,
                    count = sbns.size,
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

        fun reseed() {
            instance?.reseedNotifications()
        }

        fun dismiss(packageName: String) {
            val key = _notifications.value[packageName]?.notificationKey ?: return
            val current = _notifications.value.toMutableMap()
            current.remove(packageName)
            _notifications.value = current
            instance?.cancelNotification(key)
        }
    }
}
