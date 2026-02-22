package de.codevoid.andremote2.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import kotlin.math.abs

class DraggableOverlayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dragThreshold = 10f
    private var isDragging = false
    private var touchFromButton = false
    private var startRawX = 0f
    private var startRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                touchFromButton = isTouchOnButton(ev)
                startRawX = ev.rawX
                startRawY = ev.rawY
                lastRawX = ev.rawX
                lastRawY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!touchFromButton) return false
                val dx = ev.rawX - startRawX
                val dy = ev.rawY - startRawY
                if (abs(dx) > dragThreshold || abs(dy) > dragThreshold) {
                    isDragging = true
                    return true
                }
            }
        }
        return false
    }

    private fun isTouchOnButton(ev: MotionEvent): Boolean {
        return isButtonUnder(this, ev.x.toInt(), ev.y.toInt())
    }

    private fun isButtonUnder(group: android.view.ViewGroup, x: Int, y: Int): Boolean {
        for (i in 0 until group.childCount) {
            val child = group.getChildAt(i)
            if (child.visibility != VISIBLE) continue
            if (x >= child.left && x <= child.right && y >= child.top && y <= child.bottom) {
                if (child is ButtonView) return true
                if (child is android.view.ViewGroup) {
                    val childX = x - child.left
                    val childY = y - child.top
                    if (isButtonUnder(child, childX, childY)) return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = (event.rawX - lastRawX).toInt()
                    val dy = (event.rawY - lastRawY).toInt()
                    onDrag?.invoke(dx, dy)
                    lastRawX = event.rawX
                    lastRawY = event.rawY
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return isDragging
    }
}
