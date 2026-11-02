package com.beackers.dumbhome.notifications

import android.content.Context
import android.service.notification.StatusBarNotification

object NotificationMapper {

    fun rows(context: Context, notifications: List<StatusBarNotification>): List<NotificationRow> {
        return notifications.map { sbn ->
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString().orEmpty()
            val text = extras.getCharSequence("android.bigText")?.toString()
                ?: extras.getCharSequence("android.text")?.toString()
                ?: ""

            val appName = try {
                val pm = context.packageManager
                pm.getApplicationLabel(
                    pm.getApplicationInfo(sbn.packageName, 0)
                ).toString()
            } catch (_: Exception) {
                sbn.packageName
            }

            NotificationRow(
                key = sbn.key,
                appName = appName,
                title = title,
                text = text,
                intent = sbn.notification.contentIntent
            )
        }
    }

    fun emptyRow(): NotificationRow = NotificationRow(
        key = "",
        appName = "",
        title = "",
        text = "All caught up :)",
        intent = null
    )
}
