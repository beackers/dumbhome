package com.beackers.dumbhome.launcher

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager

import com.beackers.dumbhome.R

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LauncherActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launcher)

    val recycler = findViewById<RecyclerView>(R.id.appList)
    val apps = getLaunchableApps()
    recycler.layoutManager = LinearLayoutManager(this)
    recycler.adapter = AppLauncherAdapter(apps) { app ->
        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
        startActivity(intent)
        finish()
    }
  }

  override fun onBackPressed() {
    finish()
  }

  private fun getLaunchableApps(): List<AppEntry> {
    val pm = packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return packages.mapNotNull { app ->
      val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
      if (launchIntent != null) {
        AppEntry(
          label = pm.getApplicationLabel(app).toString(),
          packageName = app.packageName
        )
      } else null
    }.sortedBy { it.label.lowercase() }
  }
}
