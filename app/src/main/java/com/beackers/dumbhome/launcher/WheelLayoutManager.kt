package com.beackers.dumbhome.launcher

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R
import kotlin.math.cos

class WheelLayoutManager(
    context: Context
) : LinearLayoutManager(context, VERTICAL, false) {

    private val density = context.resources.displayMetrics.density
    private val itemSpacing = dp(60f)
    private val horizontalRadius = dp(150f)
    private val maxAngle = Math.toRadians(72.0)

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        applyWheelTransforms(0)
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        val consumed = super.scrollVerticallyBy(dy, recycler, state)
        applyWheelTransforms(0)
        return consumed
    }

    fun scrollToScreenCenter(recycler: RecyclerView, position: Int, selectedIndex: Int) {
        val recyclerLocation = IntArray(2)
        recycler.getLocationOnScreen(recyclerLocation)

        val screenCenterY = resources.displayMetrics.heightPixels / 2f
        val recyclerCenterY = screenCenterY - recyclerLocation[1]
        val itemHeight = recycler.resources.getDimensionPixelSize(R.dimen.launcher_item_height)
        val desiredTop = (recyclerCenterY - itemHeight / 2f).toInt()

        scrollToPositionWithOffset(position, desiredTop)
        applyWheelTransforms(selectedIndex)
    }

    fun applyWheelTransforms(selectedIndex: Int) {
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
            val scale = 1f - (absDistance * 0.07f)
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = (1f - absDistance * 0.20f).coerceAtLeast(0.25f)

            val isSelected = getPosition(child) == selectedIndex
            val nameView = child.findViewById<TextView>(R.id.appName)
            nameView.isSelected = isSelected

            if (isSelected) {
                child.scaleX = 1f
                child.scaleY = 1f
                child.alpha = 1f
                child.elevation = dp(4f)
                nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            } else {
                child.elevation = 0f
                nameView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            }
        }
    }

    private fun screenCenterInRecyclerView(): Float {
        val recycler = findRecyclerView()
        if (recycler == null) return height / 2f

        val location = IntArray(2)
        recycler.getLocationOnScreen(location)
        return resources.displayMetrics.heightPixels / 2f - location[1]
    }

    private fun findRecyclerView(): RecyclerView? {
        // LayoutManager does not expose its RecyclerView directly, so derive the
        // center from the current viewport when the view is already centered.
        return null
    }

    private fun dp(value: Float): Float = value * density
}
