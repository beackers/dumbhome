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

    fun rows(): List<NotificationRow> {
      return current.map { sbn ->
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString()
          ?: extras.getCharSequence("android.bigText")?.toString()
          ?: ""

        NotificationRow(
          key = sbn.key,
          packageName = sbn.packageName,
          title = title,
          text = text,
          intent = sbn.notification.contentIntent
        )
      }
    }
}
