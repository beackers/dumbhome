package com.beackers.dumbhome.notifications

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R

class NotificationAdapter(
    private val onNotificationHandled: (String) -> Unit
) : ListAdapter<NotificationRow, NotificationAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view
        val appName: TextView = view.findViewById(R.id.notificationApp)
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val text: TextView = view.findViewById(R.id.notificationBody)
        var pendingAction: Runnable? = null

        fun cancelPendingAction() {
            pendingAction?.let(itemView::removeCallbacks)
            pendingAction = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.cancelPendingAction()

        val row = getItem(position)
        holder.appName.text = row.appName.ifEmpty { "Unknown app" }
        holder.title.text = row.title.ifEmpty { "(No title)" }
        holder.text.text = row.text.ifEmpty { "(No message)" }

        var handled = false

        fun handleNotification() {
            if (handled) return
            handled = true

            if (row.intent != null) {
                try {
                    row.intent.send()
                } catch (_: Exception) {
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
                    if (event.repeatCount == 0 && holder.pendingAction == null) {
                        holder.pendingAction = Runnable { handleNotification() }
                        holder.container.postDelayed(holder.pendingAction!!, 450L)
                    }
                    true
                }
                KeyEvent.ACTION_UP -> {
                    holder.cancelPendingAction()
                    true
                }
                else -> true
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.cancelPendingAction()
        super.onViewRecycled(holder)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NotificationRow>() {
            override fun areItemsTheSame(oldItem: NotificationRow, newItem: NotificationRow): Boolean =
                oldItem.key == newItem.key

            override fun areContentsTheSame(oldItem: NotificationRow, newItem: NotificationRow): Boolean =
                oldItem == newItem
        }
    }
}
