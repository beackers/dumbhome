package com.beackers.dumbhome.launcher

import android.os.Bundle
import android.content.pm.PackageManager
import android.view.KeyEvent
import android.view.View
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

import com.beackers.dumbhome.R
import com.beackers.dumbhome.WallpaperStorage
import com.beackers.dumbhome.databinding.ActivityLauncherBinding

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LauncherActivity : AppCompatActivity() {
  private lateinit var binding: ActivityLauncherBinding
  private lateinit var recycler: RecyclerView
  private lateinit var apps: List<AppEntry>

  private val letterIndex = mutableMapOf<Char, Int>()
  private var lastKeyPressed: Int = -1
  private var cycleOffset: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    binding = ActivityLauncherBinding.inflate(layoutInflater)
    super.onCreate(savedInstanceState)
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    window.setDecorFitsSystemWindows(false)
    setContentView(binding.root)
    window.navigationBarColor = android.graphics.Color.TRANSPARENT

    val wallpaper = loadSavedWallpaper()
    wallpaper?.let {
      binding.launcherLayout.background = BitmapDrawable(resources, it)
    }

    recycler = findViewById<RecyclerView>(R.id.appList)
    val pickMode = intent.getBooleanExtra("pick_mode", false)
    apps = getLaunchableApps()
    buildLetterIndex()
    recycler.layoutManager = LinearLayoutManager(this)

    recycler.adapter = AppLauncherAdapter(apps) { app ->
        if (pickMode) {
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

  private fun loadSavedWallpaper(): Bitmap? {
    return WallpaperStorage.load(this)
  }
}
