package com.beackers.dumbhome.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent

class DumbNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
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
        sendBroadcast(Intent("com.beackers.dumbhome.NOTIFICATIONS_UPDATED"))
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
