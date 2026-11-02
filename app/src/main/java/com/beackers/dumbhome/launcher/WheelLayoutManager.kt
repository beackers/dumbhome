package com.beackers.dumbhome.launcher

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.cos

class WheelLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {
    private val density = context.resources.displayMetrics.density
    private val itemSpacing = dp(60f)
    private val horizontalRadius = dp(150f)
    private val maxAngle = Math.toRadians(72.0)
    private var selectedIndex = 0
    private var recyclerTopOnScreen = 0f

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        updateViewport(recycler)
        super.onLayoutChildren(recycler, state)
        applyWheelTransforms()
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        updateViewport(recycler)
        val consumed = super.scrollVerticallyBy(dy, recycler, state)
        applyWheelTransforms()
        return consumed
    }

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
        applyWheelTransforms()
    }

    fun scrollToScreenCenter(recycler: RecyclerView, position: Int) {
        updateViewport(recycler)

        // Lay out the target once so we can use its real measured height instead
        // of duplicating the XML row height in Kotlin.
        scrollToPositionWithOffset(position, 0)
        val childHeight = findViewByPosition(position)?.measuredHeight ?: 0
        val screenCenterY = resources.displayMetrics.heightPixels / 2f
        val recyclerCenterY = screenCenterY - recyclerTopOnScreen
        val desiredTop = (recyclerCenterY - childHeight / 2f).toInt()

        scrollToPositionWithOffset(position, desiredTop)
        applyWheelTransforms()
    }

    fun applyWheelTransforms() {
        val centerY = screenCenterInRecyclerView()

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val childCenterY = (getDecoratedTop(child) + getDecoratedBottom(child)) / 2f
            val distance = (childCenterY - centerY) / itemSpacing
            val clamped = distance.coerceIn(-2.5f, 2.5f)
            val angle = clamped * maxAngle / 2.0

            child.translationX =
                (-horizontalRadius * (1.0 - cos(angle))).toFloat()

            val absDistance = kotlin.math.abs(clamped)
            val scale = 1f - absDistance * 0.07f
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = (1f - absDistance * 0.20f).coerceAtLeast(0.25f)

            val isSelected = getPosition(child) == selectedIndex
            val nameView = child.findViewById<TextView>(R.id.appName)
            nameView.isSelected = isSelected
            nameView.setTypeface(
                Typeface.DEFAULT,
                if (isSelected) Typeface.BOLD else Typeface.NORMAL
            )

            child.elevation = if (isSelected) dp(4f) else 0f
            if (isSelected) {
                child.scaleX = 1f
                child.scaleY = 1f
                child.alpha = 1f
            }
        }
    }

    private fun updateViewport(recycler: RecyclerView) {
        val location = IntArray(2)
        recycler.getLocationOnScreen(location)
        recyclerTopOnScreen = location[1].toFloat()
    }

    private fun screenCenterInRecyclerView(): Float {
        return resources.displayMetrics.heightPixels / 2f - recyclerTopOnScreen
    }

    private fun dp(value: Float): Float = value * density
}
