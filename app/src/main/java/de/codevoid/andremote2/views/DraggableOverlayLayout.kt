package de.codevoid.andremote2.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout

class DraggableOverlayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val dragThreshold = 10f
    private var isDragging = false
    private var startRawX = 0f
    private var startRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                startRawX = ev.rawX
                startRawY = ev.rawY
                lastRawX = ev.rawX
                lastRawY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - startRawX
                val dy = ev.rawY - startRawY
                if (Math.abs(dx) > dragThreshold || Math.abs(dy) > dragThreshold) {
                    isDragging = true
                    return true
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
