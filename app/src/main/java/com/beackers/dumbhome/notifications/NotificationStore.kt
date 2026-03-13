package com.beackers.dumbhome.notifications

import java.util.concurrent.CopyOnWriteArrayList

object NotificationStore {
    data class NotificationEntry(
        val packageName: String,
        val title: String,
        val text: String,
        val postTime: Long
    )

    private val current = CopyOnWriteArrayList<NotificationEntry>()

    fun updateFromStatusBar(packageNotifications: List<NotificationEntry>) {
        current.clear()
        current.addAll(packageNotifications.sortedByDescending { it.postTime })
    }

    fun updateFromAccessibility(notification: NotificationEntry) {
        if (notification.packageName.isBlank() && notification.title.isBlank() && notification.text.isBlank()) {
            return
        }

        current.removeAll { it.packageName == notification.packageName && it.title == notification.title && it.text == notification.text }
        current.add(0, notification)
    }

    fun list(): List<NotificationEntry> = current.toList().sortedByDescending { it.postTime }
}
