package com.beackers.dumbhome.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

import com.beackers.dumbhome.R
import com.beackers.dumbhome.WallpaperStorage
import com.beackers.dumbhome.databinding.ActivityLauncherBinding

import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding
    private lateinit var wheel: FrameLayout
    private lateinit var centerHighlight: View
    private lateinit var apps: List<AppEntry>

    private val itemViews = mutableListOf<TextView>()
    private val letterIndex = mutableMapOf<Char, Int>()
    private var selectedIndex = 0
    private var lastKeyPressed: Int = -1
    private var cycleOffset: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.setDecorFitsSystemWindows(false)
        setContentView(binding.root)
        window.navigationBarColor = Color.TRANSPARENT

        WallpaperStorage.load(this)?.let {
            binding.launcherLayout.background =
                android.graphics.drawable.BitmapDrawable(resources, it)
        }

        wheel = findViewById(R.id.appWheel)
        centerHighlight = findViewById(R.id.centerHighlight)

        val pickMode = intent.getBooleanExtra("pick_mode", false)
        apps = getLaunchableApps()
        buildLetterIndex()

        if (apps.isEmpty()) {
            addItemView("No apps")
        } else {
            rebuildWheel()
        }

        // Keep the selector visually centered while the app names move around it.
        centerHighlight.background = GradientDrawable().apply {
            cornerRadius = dp(10f)
            setColor(0xCC202020.toInt())
            setStroke(dp(1f).toInt(), 0x99FFFFFF.toInt())
        }

        if (pickMode) {
            centerHighlight.setOnClickListener {
                launchSelected(pickMode)
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (apps.isEmpty()) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                moveSelection(-1)
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveSelection(1)
                return true
            }
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                launchSelected(intent.getBooleanExtra("pick_mode", false))
                return true
            }
            in KeyEvent.KEYCODE_2..KeyEvent.KEYCODE_9 -> {
                jumpToLetterGroup(keyCode)
                return true
            }
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
            } else {
                null
            }
        }.sortedBy { it.label.lowercase() }
    }

    private fun buildLetterIndex() {
        letterIndex.clear()
        apps.forEachIndexed { index, app ->
            app.label.firstOrNull()?.lowercaseChar()?.let { first ->
                if (first !in letterIndex) {
                    letterIndex[first] = index
                }
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
        val positions = letters.mapNotNull { letterIndex[it] }.sorted()
        if (positions.isEmpty()) return

        if (keyCode == lastKeyPressed) {
            cycleOffset = (cycleOffset + 1) % positions.size
        } else {
            cycleOffset = 0
            lastKeyPressed = keyCode
        }

        selectedIndex = positions[cycleOffset]
        rebuildWheel()
    }

    private fun moveSelection(delta: Int) {
        selectedIndex = (selectedIndex + delta).floorMod(apps.size)
        lastKeyPressed = -1
        cycleOffset = 0
        rebuildWheel()
    }

    private fun rebuildWheel() {
        wheel.removeAllViews()
        itemViews.clear()

        val visibleRadius = 2
        for (distance in -visibleRadius..visibleRadius) {
            val index = (selectedIndex + distance).floorMod(apps.size)
            val app = apps[index]

            val view = TextView(this).apply {
                text = app.label
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                typeface = if (distance == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                textSize = if (distance == 0) 20f else 17f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                isFocusable = false
            }

            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(if (distance == 0) 56f else 48f).toInt()
            )
            params.gravity = Gravity.CENTER
            view.layoutParams = params

            val absDistance = kotlin.math.abs(distance)
            view.translationY = dp(distance * 58f)
            view.translationX = dp(absDistance * absDistance * 9f)
            view.scaleX = 1f - (absDistance * 0.10f)
            view.scaleY = 1f - (absDistance * 0.10f)
            view.alpha = 1f - (absDistance * 0.23f)

            if (distance == 0) {
                view.background = centerHighlight.background
                view.background = GradientDrawable().apply {
                    cornerRadius = dp(10f)
                    setColor(0xCC202020.toInt())
                    setStroke(dp(1f).toInt(), 0x99FFFFFF.toInt())
                }
                view.setPadding(dp(12f).toInt(), 0, dp(12f).toInt(), 0)
            }

            wheel.addView(view)
            itemViews += view
        }
    }

    private fun addItemView(text: String) {
        val view = TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 18f
        }
        wheel.addView(view)
    }

    private fun launchSelected(pickMode: Boolean) {
        val app = apps.getOrNull(selectedIndex) ?: return

        if (pickMode) {
            setResult(
                RESULT_OK,
                Intent().putExtra("package", app.packageName)
            )
            finish()
            return
        }

        packageManager.getLaunchIntentForPackage(app.packageName)?.let {
            startActivity(it)
            finish()
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus
}
