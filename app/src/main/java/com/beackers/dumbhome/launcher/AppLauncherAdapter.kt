package com.beackers.dumbhome.launcher

import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

import com.beackers.dumbhome.R

import androidx.recyclerview.widget.RecyclerView

class AppLauncherAdapter(
    private val apps: List<AppEntry>,
    private val onLaunch: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppLauncherAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.name.text = app.label

        holder.itemView.setOnClickListener {
            onLaunch(app)
        }
    }
}
