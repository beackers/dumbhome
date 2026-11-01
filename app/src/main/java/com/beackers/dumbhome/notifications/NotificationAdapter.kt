package com.beackers.dumbhome.notifications

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R

class NotificationAdapter(
    private val rows: List<NotificationRow>,
    private val onNotificationHandled: (String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view
        val appName: TextView = view.findViewById(R.id.notificationApp)
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val text: TextView = view.findViewById(R.id.notificationBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]

        holder.appName.text = row.appName.ifEmpty { "Unknown app" }
        holder.title.text = row.title.ifEmpty { "(No title)" }
        holder.text.text = row.text.ifEmpty { "(No message)" }

        val handler = Handler(Looper.getMainLooper())
        var handled = false
        var pendingAction: Runnable? = null

        fun handleNotification() {
            if (handled) return
            handled = true

            if (row.intent != null) {
                try {
                    row.intent.send()
                } catch (_: Exception) {
                    // A stale PendingIntent is no more useful than no intent.
                    onNotificationHandled(row.key)
                }
            } else {
                onNotificationHandled(row.key)
            }
        }

        holder.container.setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_ENTER) {
                return@setOnKeyListener false
            }

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0 && pendingAction == null) {
                        pendingAction = Runnable { handleNotification() }
                        handler.postDelayed(pendingAction!!, 450L)
                    }
                    true
                }
                KeyEvent.ACTION_UP -> {
                    pendingAction?.let(handler::removeCallbacks)
                    pendingAction = null
                    true
                }
                else -> true
            }
        }
    }
}
