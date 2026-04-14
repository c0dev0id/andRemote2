package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import de.codevoid.andremote2.R
import kotlin.math.sqrt

class CollapseHandleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_base)
        style = Paint.Style.FILL
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_border)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    var onTap: (() -> Unit)? = null
    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null

    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var isDragging = false

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 4f
        canvas.drawCircle(cx, cy, r, paintFill)
        canvas.drawCircle(cx, cy, r, paintBorder)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                lastRawX = event.rawX
                lastRawY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastRawX).toInt()
                val dy = (event.rawY - lastRawY).toInt()
                if (!isDragging) {
                    val totalDx = event.rawX - downRawX
                    val totalDy = event.rawY - downRawY
                    if (sqrt(totalDx * totalDx + totalDy * totalDy) > touchSlop) {
                        isDragging = true
                    }
                }
                if (isDragging) onDrag?.invoke(dx, dy)
                lastRawX = event.rawX
                lastRawY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) onTap?.invoke()
                isDragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
