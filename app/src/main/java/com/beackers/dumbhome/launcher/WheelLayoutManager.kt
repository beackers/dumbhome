package com.beackers.dumbhome.launcher

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beackers.dumbhome.R
import kotlin.math.cos

class WheelLayoutManager(context: Context) : LinearLayoutManager(context, VERTICAL, false) {
    private val density = context.resources.displayMetrics.density
    private val itemSpacing = dp(60f)
    private val horizontalRadius = dp(150f)
    private val maxAngle = Math.toRadians(72.0)
    private var selectedIndex = 0
    private var attachedRecycler: RecyclerView? = null
    private var recenterWhenIdle = false

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        attachedRecycler = view
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        attachedRecycler = null
        recenterWhenIdle = false
        super.onDetachedFromWindow(view, recycler)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        applyWheelTransforms()
    }

    override fun onScrollStateChanged(state: Int) {
        super.onScrollStateChanged(state)

        if (state == RecyclerView.SCROLL_STATE_IDLE && recenterWhenIdle) {
            recenterWhenIdle = false
            attachedRecycler?.let { recycler ->
                scrollToScreenCenter(recycler, selectedIndex)
            }
        }

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

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
        applyWheelTransforms()
    }

    fun smoothScrollToScreenCenter(position: Int) {
        val recycler = attachedRecycler ?: return
        recenterWhenIdle = true

        val scroller = object : androidx.recyclerview.widget.LinearSmoothScroller(recycler.context) {
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

        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    fun scrollToScreenCenter(recycler: RecyclerView, position: Int) {
        // Lay out the target once so we can use its real measured height instead
        // of duplicating the XML row height in Kotlin.
        scrollToPositionWithOffset(position, 0)
        val childHeight = findViewByPosition(position)?.measuredHeight ?: 0
        val recyclerCenterY = recycler.height / 2f
        val desiredTop = (
            recyclerCenterY - childHeight / 2f - recycler.paddingTop
        ).toInt()

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

    private fun dp(value: Float): Float = value * density
}
