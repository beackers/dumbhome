package com.beackers.dumbhome.launcher

import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.view.KeyEvent

import com.beackers.dumbhome.R
import com.beackers.dumbhome.openapps.OpenAppsActivity

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LauncherActivity : AppCompatActivity() {
  companion object {
    private const val OPEN_APPS_SENTINEL = "__open_apps__"
  }

  private lateinit var recycler: RecyclerView
  private lateinit var apps: List<AppEntry>

  private val letterIndex = mutableMapOf<Char, Int>()
  private var lastKeyPressed: Int = -1
  private var cycleOffset: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_launcher)

    recycler = findViewById<RecyclerView>(R.id.appList)
    val pickMode = intent.getBooleanExtra("pick_mode", false)
    apps = getLaunchableApps(includeOpenApps = !pickMode)
    buildLetterIndex()
    recycler.layoutManager = LinearLayoutManager(this)

    recycler.adapter = AppLauncherAdapter(apps) { app ->
        if (app.packageName == OPEN_APPS_SENTINEL) {
            startActivity(Intent(this, OpenAppsActivity::class.java))
            finish()
        } else if (pickMode) {
            val result = Intent().putExtra("package", app.packageName)
            setResult(RESULT_OK, result)
            finish()
        } else {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            startActivity(intent)
            finish()
        }
    }
  }

  override fun onBackPressed() {
    finish()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode in KeyEvent.KEYCODE_2..KeyEvent.KEYCODE_9) {
      jumpToLetterGroup(keyCode)
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  private fun getLaunchableApps(includeOpenApps: Boolean): List<AppEntry> {
    val pm = packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val launchableApps = packages.mapNotNull { app ->
      val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
      if (launchIntent != null) {
        AppEntry(
          label = pm.getApplicationLabel(app).toString(),
          packageName = app.packageName
        )
      } else null
    }.sortedBy { it.label.lowercase() }

    return if (includeOpenApps) {
      listOf(AppEntry(label = "Open apps in memory", packageName = OPEN_APPS_SENTINEL)) + launchableApps
    } else {
      launchableApps
    }
  }

  private fun buildLetterIndex() {
    letterIndex.clear()
    apps.forEachIndexed { index, app ->
      val first = app.label.firstOrNull()?.lowercaseChar()
      if (first != null && first !in letterIndex) {
        letterIndex[first] = index
      }
    }
  }

  private fun lettersForKey(keyCode: Int): String? {
    return when (keyCode) {
      KeyEvent.KEYCODE_2 -> "abc"
      KeyEvent.KEYCODE_3 -> "def"
      KeyEvent.KEYCODE_4 -> "ghi"
      KeyEvent.KEYCODE_5 -> "jkl"
      KeyEvent.KEYCODE_6 -> "mno"
      KeyEvent.KEYCODE_7 -> "pqrs"
      KeyEvent.KEYCODE_8 -> "tuv"
      KeyEvent.KEYCODE_9 -> "wxyz"
      else -> null
    }
  }

  private fun jumpToLetterGroup(keyCode: Int) {

    val letters = lettersForKey(keyCode) ?: return

    val positions = letters
      .mapNotNull { letterIndex[it] }
      .sorted()

    if (positions.isEmpty()) return

    if (keyCode == lastKeyPressed) {
      cycleOffset = (cycleOffset + 1) % positions.size
    } else {
      cycleOffset = 0
      lastKeyPressed = keyCode
    }

    val index = positions[cycleOffset]

    (recycler.layoutManager as LinearLayoutManager)
      .scrollToPositionWithOffset(index, 0)
  }
}
