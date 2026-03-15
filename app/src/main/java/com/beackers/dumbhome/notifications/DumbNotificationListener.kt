package com.beackers.dumbhome.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class DumbNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    private fun refresh() {
        val rows = (activeNotifications ?: emptyArray()).map {
            NotificationStore.NotificationEntry(
                packageName = it.packageName.orEmpty(),
                title = it.notification.extras.getCharSequence("android.title")?.toString().orEmpty(),
                text = it.notification.extras.getCharSequence("android.text")?.toString().orEmpty(),
                postTime = it.postTime
            )
        }
        NotificationStore.updateFromStatusBar(rows)
    }
}
