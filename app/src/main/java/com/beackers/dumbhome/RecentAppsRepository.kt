package com.beackers.dumbhome

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.graphics.drawable.Drawable

class RecentAppsRepository(private val context: Context) {

    fun getTopRecentApps(limit: Int = 5): List<RecentApp> {
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        val stats = usageStats.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - LOOKBACK_MILLIS,
            now
        ) ?: emptyList()

        val packageManager = context.packageManager

        return stats
            .filter { it.lastTimeUsed > 0 }
            .sortedByDescending { it.lastTimeUsed }
            .mapNotNull { stat ->
                if (stat.packageName == context.packageName) return@mapNotNull null

                val intent = packageManager.getLaunchIntentForPackage(stat.packageName)
                    ?: return@mapNotNull null

                val info = try {
                    packageManager.getApplicationInfo(stat.packageName, 0)
                } catch (_: Exception) {
                    return@mapNotNull null
                }

                RecentApp(
                    label = packageManager.getApplicationLabel(info).toString(),
                    icon = packageManager.getApplicationIcon(info),
                    intent = intent
                )
            }
            .distinctBy { it.intent.component?.packageName }
            .take(limit)
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    companion object {
        private const val LOOKBACK_MILLIS = 6 * 60 * 60 * 1000L
    }
}

data class RecentApp(
    val label: String,
    val icon: Drawable,
    val intent: android.content.Intent
)
