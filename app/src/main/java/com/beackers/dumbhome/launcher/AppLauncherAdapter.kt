package com.beackers.dumbhome.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R

class AppLauncherAdapter(
    private val apps: List<AppEntry>,
    private val onLaunch: (AppEntry) -> Unit
) : RecyclerView.Adapter<AppLauncherAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]

        holder.name.text = app.label
        holder.name.isSelected = false
        holder.icon.setImageDrawable(app.icon)
        holder.icon.visibility = View.VISIBLE

        holder.itemView.apply {
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
            isLongClickable = false
            defaultFocusHighlightEnabled = false
            foreground = null
            background = null
            stateListAnimator = null
        }

        holder.itemView.setOnClickListener {
            onLaunch(app)
        }
    }
}
