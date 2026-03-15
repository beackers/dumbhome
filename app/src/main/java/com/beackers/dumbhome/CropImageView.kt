package com.beackers.dumbhome

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.max

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatImageView(context, attrs) {
    private val drawMatrix = Matrix()
    private val inverseMatrix = Matrix()
    private var mode = MODE_NONE
    private var lastX = 0f
    private var lastY = 0f
    private var startDistance = 0f
    private var minScale = 1f

    var cropRect: RectF = RectF()
        set(value) {
            field = RectF(value)
            fitToCropRect()
        }

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null || cropRect.isEmpty) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = MODE_DRAG
                lastX = event.x
                lastY = event.y
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    startDistance = distance(event)
                    mode = MODE_ZOOM
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    MODE_DRAG -> {
                        val dx = event.x - lastX
                        val dy = event.y - lastY
                        drawMatrix.postTranslate(dx, dy)
                        lastX = event.x
                        lastY = event.y
                    }

                    MODE_ZOOM -> {
                        if (event.pointerCount >= 2) {
                            val newDistance = distance(event)
                            if (startDistance > 0f && newDistance > 0f) {
                                val scale = newDistance / startDistance
                                val focusX = (event.getX(0) + event.getX(1)) / 2f
                                val focusY = (event.getY(0) + event.getY(1)) / 2f
                                drawMatrix.postScale(scale, scale, focusX, focusY)
                                startDistance = newDistance
                            }
                        }
                    }
                }
                enforceBounds()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                mode = MODE_NONE
                startDistance = 0f
                enforceBounds()
            }
        }

        imageMatrix = drawMatrix
        return true
    }

    fun setImageBitmapAndReset(bitmap: Bitmap) {
        setImageBitmap(bitmap)
        post { fitToCropRect() }
    }

    fun createCroppedBitmap(sourceBitmap: Bitmap): Bitmap {
        val mappedCrop = RectF(cropRect)
        drawMatrix.invert(inverseMatrix)
        inverseMatrix.mapRect(mappedCrop)

        val left = mappedCrop.left.coerceIn(0f, sourceBitmap.width.toFloat())
        val top = mappedCrop.top.coerceIn(0f, sourceBitmap.height.toFloat())
        val right = mappedCrop.right.coerceIn(0f, sourceBitmap.width.toFloat())
        val bottom = mappedCrop.bottom.coerceIn(0f, sourceBitmap.height.toFloat())

        val width = max(1, (right - left).toInt())
        val height = max(1, (bottom - top).toInt())

        return Bitmap.createBitmap(sourceBitmap, left.toInt(), top.toInt(), width, height)
    }

    private fun fitToCropRect() {
        val d = drawable ?: return
        if (cropRect.isEmpty) return

        val sourceW = d.intrinsicWidth.toFloat()
        val sourceH = d.intrinsicHeight.toFloat()
        if (sourceW <= 0f || sourceH <= 0f) return

        val scale = max(cropRect.width() / sourceW, cropRect.height() / sourceH)
        minScale = scale

        drawMatrix.reset()
        drawMatrix.postScale(scale, scale)
        val displayW = sourceW * scale
        val displayH = sourceH * scale
        val dx = cropRect.left + (cropRect.width() - displayW) / 2f
        val dy = cropRect.top + (cropRect.height() - displayH) / 2f
        drawMatrix.postTranslate(dx, dy)
        imageMatrix = drawMatrix
    }

    private fun enforceBounds() {
        val d = drawable ?: return
        val sourceRect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.mapRect(sourceRect)

        val currentScale = currentScale()
        if (currentScale < minScale) {
            val factor = minScale / currentScale
            drawMatrix.postScale(factor, factor, cropRect.centerX(), cropRect.centerY())
            drawMatrix.mapRect(sourceRect)
        }

        var dx = 0f
        var dy = 0f
        if (sourceRect.left > cropRect.left) dx = cropRect.left - sourceRect.left
        if (sourceRect.top > cropRect.top) dy = cropRect.top - sourceRect.top
        if (sourceRect.right < cropRect.right) dx = cropRect.right - sourceRect.right
        if (sourceRect.bottom < cropRect.bottom) dy = cropRect.bottom - sourceRect.bottom

        drawMatrix.postTranslate(dx, dy)
    }

    private fun currentScale(): Float {
        val values = FloatArray(9)
        drawMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    private fun distance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private companion object {
        const val MODE_NONE = 0
        const val MODE_DRAG = 1
        const val MODE_ZOOM = 2
    }
}
