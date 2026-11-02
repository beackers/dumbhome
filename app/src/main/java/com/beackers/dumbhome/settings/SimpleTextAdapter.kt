package com.beackers.dumbhome

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleTextAdapter(
    private var items: List<SettingsRow>,
    private val onClick: ((SettingsRow) -> Unit)? = null
) : RecyclerView.Adapter<SimpleTextAdapter.TextVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_row, parent, false) as TextView
        return TextVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TextVH, position: Int) {
        val item = items[position]
        holder.text.text = item.label
        holder.text.setOnClickListener { onClick?.invoke(item) }
    }

    fun submit(newItems: List<SettingsRow>) {
        items = newItems.toList()
        notifyDataSetChanged()
    }

    class TextVH(val text: TextView) : RecyclerView.ViewHolder(text)
}

data class SettingsRow(
    val label: String,
    val action: SettingsRowAction,
    val prefKey: String? = null
)

enum class SettingsRowAction {
    CHANGE_WALLPAPER,
    CONFIGURE_SHORTCUT,
    CLOSE
}
