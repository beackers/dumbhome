package com.beackers.dumbhome.notifications

import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

object NotificationStore {
    private val current = CopyOnWriteArrayList<StatusBarNotification>()

    fun update(all: Array<StatusBarNotification>) {
        current.clear()
        current.addAll(all.sortedByDescending { it.postTime })
    }

    fun list(): List<StatusBarNotification> = current.toList()
}
