package com.beackers.dumbhome.openapps

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beackers.dumbhome.R

class OpenAppsActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_apps)

        /* Problem that didn't exist
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
          val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
          v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
          insets
        }
        */

        recycler = findViewById(R.id.openAppsList)
        recycler.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()

        val apps = getOpenApps()
        recycler.adapter = OpenAppsAdapter(apps) { app ->
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                startActivity(intent)
                finish()
            }
        }

        recycler.post {
            if (apps.isNotEmpty()) {
                recycler.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: recycler.requestFocus()
            }
        }
    }

    private fun getOpenApps(): List<OpenAppEntry> {
        val activityManager = getSystemService(ActivityManager::class.java)
        val usageStatsManager = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val usageStats = usageStatsManager?.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - (7L * 24L * 60L * 60L * 1000L),
            now,
        ).orEmpty()

        val lastUsedByPackage = usageStats.associate { it.packageName to it.lastTimeUsed }

        val recentCutoff = now - (30L * 60L * 1000L) // 30 minutes

        val runningPackages = usageStats
            .filter { it.lastTimeUsed >= recentCutoff }
            .map { it.packageName }
            .toSet()

        val pm = packageManager
        return runningPackages
            .asSequence()
            .filter { it != packageName }
            .mapNotNull { pkg ->
                val launchIntent = pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
                val appInfo = launchIntent.resolveActivityInfo(pm, 0)?.applicationInfo
                    ?: pm.getApplicationInfo(pkg, 0)
                OpenAppEntry(
                    label = pm.getApplicationLabel(appInfo).toString(),
                    packageName = pkg,
                    icon = pm.getApplicationIcon(appInfo),
                    lastTimeUsed = lastUsedByPackage[pkg] ?: 0L,
                )
            }
            .sortedWith(compareByDescending<OpenAppEntry> { it.lastTimeUsed }.thenBy { it.label.lowercase() })
            .toList()
    }
}

private data class OpenAppEntry(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable,
    val lastTimeUsed: Long,
)

private class OpenAppsAdapter(
    private val apps: List<OpenAppEntry>,
    private val onLaunch: (OpenAppEntry) -> Unit,
) : RecyclerView.Adapter<OpenAppsAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.app_icon_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.label

        holder.itemView.setOnClickListener {
            onLaunch(app)
        }

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN &&
                (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
            ) {
                onLaunch(app)
                true
            } else {
                false
            }
        }
    }
}
