package com.beackers.dumbhome.openapps

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.app.AppOpsManager
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.beackers.dumbhome.R
import com.beackers.dumbhome.databinding.ActivityOpenAppsBinding

class OpenAppsActivity : AppCompatActivity() {
  private lateinit var binding: ActivityOpenAppsBinding
  private var center: RecentApp? = null
  private var left: RecentApp? = null
  private var right: RecentApp? = null
  private var up: RecentApp? = null
  private var down: RecentApp? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityOpenAppsBinding.inflate(layoutInflater)
    setContentView(binding.root)
  }

  override fun onResume() {
    super.onResume()

    if (!hasUsagePermissions()) {
      startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
      return
    }

    val apps = getTopRecentApps(this)
    
    center = apps.getOrNull(0)
    right = apps.getOrNull(1)
    down = apps.getOrNull(2)
    left = apps.getOrNull(3)
    up = apps.getOrNull(4)

    // set icons
    binding.centerIcon.setImageDrawable(center?.icon)
    binding.leftIcon.setImageDrawable(left?.icon)
    binding.rightIcon.setImageDrawable(right?.icon)
    binding.upIcon.setImageDrawable(up?.icon)
    binding.downIcon.setImageDrawable(down?.icon)

    // hide dead buttons
    binding.centerIcon.visibility = if (center != null) View.VISIBLE else View.INVISIBLE
    binding.leftIcon.visibility = if (left != null) View.VISIBLE else View.INVISIBLE
    binding.rightIcon.visibility = if (right != null) View.VISIBLE else View.INVISIBLE
    binding.upIcon.visibility = if (up != null) View.VISIBLE else View.INVISIBLE
    binding.downIcon.visibility = if (down != null) View.VISIBLE else View.INVISIBLE
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val app = when (keyCode) {
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_DPAD_CENTER -> center
      KeyEvent.KEYCODE_DPAD_RIGHT -> right
      KeyEvent.KEYCODE_DPAD_LEFT -> left
      KeyEvent.KEYCODE_DPAD_DOWN -> down
      KeyEvent.KEYCODE_DPAD_UP -> up
      else -> null
    }
    app?.let {
      startActivity(it.intent)
      finish()
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  private fun getTopRecentApps(context: Context, limit: Int = 5): List<RecentApp> {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()

    val stats = usm.queryUsageStats(
      UsageStatsManager.INTERVAL_DAILY,
      now - (6 * 60 * 60 * 1000),
      now
    ) ?: emptyList()

    val pm = context.packageManager

    return stats
    .filter { it.lastTimeUsed > 0 }
    .sortedByDescending { it.lastTimeUsed }
    .mapNotNull { stat ->
      if (stat.packageName == packageName) return@mapNotNull null

      val intent = pm.getLaunchIntentForPackage(stat.packageName)
        ?: return@mapNotNull null

      val info = pm.getApplicationInfo(stat.packageName, 0)

      RecentApp(
        label = pm.getApplicationLabel(info).toString(),
        icon = pm.getApplicationIcon(info),
        intent = intent
      )
    }
    .distinctBy { it.intent.component?.packageName }
    .take(limit)
  }
  
  private fun hasUsagePermissions(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
      AppOpsManager.OPSTR_GET_USAGE_STATS,
      android.os.Process.myUid(),
      packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
  }
}

private data class RecentApp(
  val label: String,
  val icon: android.graphics.drawable.Drawable,
  val intent: Intent
)
