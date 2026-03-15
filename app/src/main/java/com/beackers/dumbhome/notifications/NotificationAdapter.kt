package com.beackers.dumbhome.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R

class NotificationAdapter(
    private val rows: List<NotificationRow>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(val view: TextView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_row, parent, false) as TextView
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]

        holder.view.text =
            "${row.packageName}\n${row.title}\n${row.text}"

        holder.view.setOnClickListener {
            row.intent?.send()
        }
    }
}
