package com.beackers.dumbhome.notifications

import android.app.PendingIntent

data class NotificationRow(
  val key: String,
  val appName: String,
  val title: String,
  val text: String,
  val intent: PendingIntent?
)
