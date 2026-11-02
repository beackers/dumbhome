package com.beackers.dumbhome.launcher

import android.content.pm.PackageManager

class AppRepository(
    private val packageManager: PackageManager
) {
    fun getLaunchableApps(): List<AppEntry> {
        return packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { app ->
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                    ?: return@mapNotNull null

                AppEntry(
                    label = packageManager.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    icon = packageManager.getApplicationIcon(app),
                    launchIntent = launchIntent
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
