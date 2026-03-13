package com.beackers.dumbhome.notifications

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.view.accessibility.AccessibilityEvent
import java.lang.System.currentTimeMillis

class DumbNotificationAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return
        }

        val notification = event.parcelableData as? Notification
        val title = notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        NotificationStore.updateFromAccessibility(
            NotificationStore.NotificationEntry(
                packageName = event.packageName?.toString().orEmpty(),
                title = title,
                text = text,
                postTime = notification?.`when`?.takeIf { it > 0L } ?: currentTimeMillis()
            )
        )
    }

    override fun onInterrupt() = Unit
}
