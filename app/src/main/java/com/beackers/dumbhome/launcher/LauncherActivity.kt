package com.beackers.dumbhome.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R
import com.beackers.dumbhome.WallpaperStorage
import com.beackers.dumbhome.databinding.ActivityLauncherBinding

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding
    private lateinit var recycler: RecyclerView
    private lateinit var apps: List<AppEntry>
    private lateinit var layoutManager: WheelLayoutManager

    private val letterIndex = mutableMapOf<Char, Int>()
    private var selectedIndex = 0
    private var lastKeyPressed = KeyEvent.KEYCODE_UNKNOWN
    private var cycleOffset = 0
    private val pickMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_PICK_MODE, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.setDecorFitsSystemWindows(false)
        window.navigationBarColor = Color.TRANSPARENT

        WallpaperStorage.load(this)?.let {
            binding.launcherLayout.background =
                android.graphics.drawable.BitmapDrawable(resources, it)
        }

        recycler = findViewById(R.id.appList)
        apps = AppRepository(packageManager).getLaunchableApps()
        buildLetterIndex()

        layoutManager = WheelLayoutManager(this)
        recycler.layoutManager = layoutManager
        recycler.adapter = AppLauncherAdapter(apps, ::launchApp)

        recycler.post {
            if (apps.isNotEmpty()) {
                centerOnSelected(animated = false)
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (apps.isEmpty()) return super.onKeyDown(keyCode, event)

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveSelection(-1)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveSelection(1)
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> launchSelected()
            in KeyEvent.KEYCODE_2..KeyEvent.KEYCODE_9 -> jumpToLetterGroup(keyCode)
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    private fun buildLetterIndex() {
        letterIndex.clear()
        apps.forEachIndexed { index, app ->
            app.label.firstOrNull()?.lowercaseChar()?.let { first ->
                letterIndex.putIfAbsent(first, index)
            }
        }
    }

    private fun jumpToLetterGroup(keyCode: Int) {
        val letters = lettersForKey(keyCode) ?: return
        val positions = letters.mapNotNull(letterIndex::get).sorted()
        if (positions.isEmpty()) return

        if (keyCode == lastKeyPressed) {
            cycleOffset = (cycleOffset + 1) % positions.size
        } else {
            cycleOffset = 0
            lastKeyPressed = keyCode
        }

        selectedIndex = positions[cycleOffset]
        centerOnSelected(animated = true)
    }

    private fun moveSelection(delta: Int) {
        selectedIndex = (selectedIndex + delta).floorMod(apps.size)
        lastKeyPressed = KeyEvent.KEYCODE_UNKNOWN
        cycleOffset = 0
        centerOnSelected(animated = true)
    }

    private fun centerOnSelected(animated: Boolean) {
        if (apps.isEmpty()) return

        layoutManager.setSelectedIndex(selectedIndex)

        if (!animated) {
            layoutManager.scrollToScreenCenter(recycler, selectedIndex)
            return
        }

        val scroller = object : LinearSmoothScroller(this) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_ANY

            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                return (boxStart + boxEnd) / 2 - (viewStart + viewEnd) / 2
            }

            override fun calculateTimeForScrolling(dx: Int): Int = 120
        }

        scroller.targetPosition = selectedIndex
        layoutManager.startSmoothScroll(scroller)
    }

    private fun launchSelected() {
        apps.getOrNull(selectedIndex)?.let(::launchApp)
    }

    private fun launchApp(app: AppEntry) {
        if (pickMode) {
            setResult(
                RESULT_OK,
                Intent().putExtra(EXTRA_SELECTED_PACKAGE, app.packageName)
            )
            finish()
            return
        }

        startActivity(app.launchIntent)
        finish()
    }

    private fun lettersForKey(keyCode: Int): String? = when (keyCode) {
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

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus

    companion object {
        const val EXTRA_PICK_MODE = "pick_mode"
        const val EXTRA_SELECTED_PACKAGE = "package"
    }
}
