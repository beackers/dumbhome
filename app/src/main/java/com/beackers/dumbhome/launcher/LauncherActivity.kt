package com.beackers.dumbhome.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.beackers.dumbhome.R
import com.beackers.dumbhome.WallpaperStorage
import com.beackers.dumbhome.databinding.ActivityLauncherBinding

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding
    private lateinit var recycler: RecyclerView
    private lateinit var apps: List<AppEntry>

    private lateinit var layoutManager: WheelLayoutManager

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

        recycler = findViewById(R.id.appList)
        val pickMode = intent.getBooleanExtra("pick_mode", false)
        apps = getLaunchableApps()
        buildLetterIndex()

        layoutManager = WheelLayoutManager()
        recycler.layoutManager = layoutManager
        recycler.adapter = AppLauncherAdapter(apps) { app ->
            if (pickMode) {
                setResult(
                    RESULT_OK,
                    Intent().putExtra("package", app.packageName)
                )
                finish()
            } else {
                packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    startActivity(it)
                    finish()
                }
            }
        }

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
        centerOnSelected(animated = true)
    }

    private fun moveSelection(delta: Int) {
        selectedIndex = (selectedIndex + delta).floorMod(apps.size)
        lastKeyPressed = -1
        cycleOffset = 0
        centerOnSelected(animated = true)
    }

    private fun centerOnSelected(animated: Boolean) {
        if (apps.isEmpty()) return

        if (!animated) {
            layoutManager.scrollToScreenCenter(selectedIndex)
            layoutManager.applyWheelTransforms()
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
                val viewCenter = (viewStart + viewEnd) / 2
                val boxCenter = (boxStart + boxEnd) / 2
                return boxCenter - viewCenter
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return 120
            }
        }
        scroller.targetPosition = selectedIndex
        layoutManager.startSmoothScroll(scroller)
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

    private inner class WheelLayoutManager :
        LinearLayoutManager(this@LauncherActivity, VERTICAL, false) {

        private val itemSpacing = dp(60f)
        private val horizontalRadius = dp(150f)
        private val itemHeight = dp(56f)
        private val maxAngle = Math.toRadians(72.0)

        override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
            super.onLayoutChildren(recycler, state)
            applyWheelTransforms()
        }

        override fun scrollVerticallyBy(
            dy: Int,
            recycler: RecyclerView.Recycler,
            state: RecyclerView.State
        ): Int {
            val consumed = super.scrollVerticallyBy(dy, recycler, state)
            applyWheelTransforms()
            return consumed
        }

        fun scrollToScreenCenter(position: Int) {
            val location = IntArray(2)
            recycler.getLocationOnScreen(location)
            val screenCenterY = resources.displayMetrics.heightPixels / 2
            val desiredTop = screenCenterY - location[1] - (itemHeight / 2f).toInt()
            scrollToPositionWithOffset(position, desiredTop)
            applyWheelTransforms()
        }

        fun applyWheelTransforms() {
            val centerY = height / 2f

            for (i in 0 until childCount) {
                val child = getChildAt(i) ?: continue
                val childCenterY = (getDecoratedTop(child) + getDecoratedBottom(child)) / 2f
                val distance = (childCenterY - centerY) / itemSpacing

                val clamped = distance.coerceIn(-2.5f, 2.5f)
                val angle = clamped * maxAngle / 2.0

                // The list follows the visible left half of a wheel.
                // Both ends recede toward the left; the text itself never rotates.
                child.translationX =
                    (-horizontalRadius * (1.0 - kotlin.math.cos(angle))).toFloat()

                val absDistance = kotlin.math.abs(clamped)
                child.scaleX = 1f - (absDistance * 0.07f)
                child.scaleY = 1f - (absDistance * 0.07f)
                child.alpha = (1f - absDistance * 0.20f).coerceAtLeast(0.25f)

                val adapterPosition = getPosition(child)
                val isSelected = adapterPosition == selectedIndex
                val nameView = child.findViewById<TextView>(R.id.appName)
                nameView.isSelected = isSelected
                nameView.textSize = when {
                    absDistance < 0.5f -> 20f
                    absDistance < 1.5f -> 17f
                    absDistance < 2.25f -> 14f
                    else -> 12f
                }
                if (isSelected) {
                    child.scaleX = 1f
                    child.scaleY = 1f
                    child.alpha = 1f
                    child.elevation = dp(4f)
                    nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
                    nameView.gravity = android.view.Gravity.CENTER_VERTICAL
                } else {
                    child.elevation = 0f
                    nameView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
                    nameView.gravity = android.view.Gravity.CENTER_VERTICAL
                }
            }
        }
    }
}
