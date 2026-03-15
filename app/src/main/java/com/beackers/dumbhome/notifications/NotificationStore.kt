package com.beackers.dumbhome.notifications

import android.service.notification.StatusBarNotification
import android.content.Context
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
        val text = extras.getCharSequence("android.text")?.toString()
          ?: extras.getCharSequence("android.bigText")?.toString()
          ?: ""
        val pm = context.packageManager
        val appName = try {
            pm.getApplicationLabel(
              pm.getApplicationInfo(
                sbn.packageName, 0)
              ).toString()
            } catch (e: Exception) {
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
