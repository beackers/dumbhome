package com.beackers.dumbhome.notifications

import android.content.Context
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

object NotificationStore {
    private val current = CopyOnWriteArrayList<StatusBarNotification>()

    fun update(all: Array<StatusBarNotification>) {
        current.clear()
        current.addAll(all.sortedByDescending { it.postTime })
    }

    fun rows(context: Context): List<NotificationRow> {
        return current.map { sbn ->
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
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
}
