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
        NotificationStore.update(activeNotifications ?: emptyArray())
    }
}
