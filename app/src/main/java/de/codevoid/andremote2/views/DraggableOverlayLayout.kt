package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout

class DraggableOverlayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    companion object {
        private const val SCALE_TOLERANCE = 0.001f
    }

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var isDragging = false
    private var touchStartedOnChild = false

    private val touchScaleMatrix = Matrix()
    private var cachedScaleX = Float.NaN
    private var cachedScaleY = Float.NaN

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    private fun isTouchOnInteractiveChild(group: ViewGroup, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        val sx = scaleX
        val sy = scaleY
        if (sx == 0f || sy == 0f) return false
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is JoystickView || child is LeverView || child is ButtonView) {
                child.getLocationOnScreen(location)
                val localX = (rawX - location[0]) / sx
                val localY = (rawY - location[1]) / sy
                if (child is JoystickView && child.isInsideShape(localX, localY)) return true
                if (child is LeverView && child.isInsideShape(localX, localY)) return true
                if (child is ButtonView && child.isInsideShape(localX, localY)) return true
            } else if (child is ViewGroup) {
                if (isTouchOnInteractiveChild(child, rawX, rawY)) return true
            }
        }
        return false
    }

    private fun isScaleNearUnity(sx: Float, sy: Float): Boolean =
        kotlin.math.abs(sx - 1f) < SCALE_TOLERANCE && kotlin.math.abs(sy - 1f) < SCALE_TOLERANCE

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val sx = scaleX
        val sy = scaleY
        if (isScaleNearUnity(sx, sy) || sx == 0f || sy == 0f) return super.dispatchTouchEvent(event)
        if (sx != cachedScaleX || sy != cachedScaleY) {
            touchScaleMatrix.setScale(1f / sx, 1f / sy, 0f, 0f)
            cachedScaleX = sx
            cachedScaleY = sy
        }
        val transformed = MotionEvent.obtain(event)
        return try {
            transformed.transform(touchScaleMatrix)
            super.dispatchTouchEvent(transformed)
        } finally {
            transformed.recycle()
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN && touchStartedOnChild) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                isDragging = false
                touchStartedOnChild = isTouchOnInteractiveChild(this, event.rawX, event.rawY)
            }
            MotionEvent.ACTION_MOVE -> {
                isDragging = true
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                touchStartedOnChild = false
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                onDrag?.invoke(dx, dy)
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
