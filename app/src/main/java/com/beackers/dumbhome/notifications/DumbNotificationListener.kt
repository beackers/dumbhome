package com.beackers.dumbhome.notifications

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class DumbNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

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
        sendBroadcast(Intent(NotificationEvents.ACTION_UPDATED))
    }

    fun clearNotification(key: String) {
        cancelNotification(key)
        refresh()
    }

    fun clearAll() {
        cancelAllNotifications()
        refresh()
    }

    companion object {
        var instance: DumbNotificationListener? = null
    }
}
