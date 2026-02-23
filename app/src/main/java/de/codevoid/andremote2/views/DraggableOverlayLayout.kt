package de.codevoid.andremote2.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout

class DraggableOverlayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var lastRawX = 0f
    private var lastRawY = 0f
    private var isDragging = false
    private var touchStartedOnChild = false

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    private fun isTouchOnInteractiveChild(group: ViewGroup, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child is JoystickView || child is LeverView || child is ButtonView) {
                child.getLocationOnScreen(location)
                if (rawX >= location[0] && rawX <= location[0] + child.width &&
                    rawY >= location[1] && rawY <= location[1] + child.height) {
                    return true
                }
            } else if (child is ViewGroup) {
                if (isTouchOnInteractiveChild(child, rawX, rawY)) return true
            }
        }
        return false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastRawX = event.rawX
                lastRawY = event.rawY
                isDragging = false
                touchStartedOnChild = isTouchOnInteractiveChild(this, event.rawX, event.rawY)
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchStartedOnChild) return false
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
