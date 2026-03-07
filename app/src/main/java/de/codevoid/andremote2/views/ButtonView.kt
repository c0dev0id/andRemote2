package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import androidx.core.content.ContextCompat
import de.codevoid.andremote2.KeyInjectionService
import de.codevoid.andremote2.R

class ButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keyCode = 66
    private var isPressed = false
    private var pressDownTime = 0L

    private val longPressRunnable = Runnable {
        KeyInjectionService.instance?.injectKeyLongPress(keyCode, pressDownTime)
            ?: Log.w("ButtonView", "KeyInjectionService not available for long press")
    }

    private val paintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_button_normal)
        style = Paint.Style.FILL
    }
    private val paintPressed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_knob)
        style = Paint.Style.FILL
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_border)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 32f
    }

    var label: String = "BTN"
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val paint = if (isPressed) paintPressed else paintNormal
        val cornerRadius = height / 4f
        canvas.drawRoundRect(4f, 4f, width - 4f, height - 4f, cornerRadius, cornerRadius, paint)
        canvas.drawRoundRect(4f, 4f, width - 4f, height - 4f, cornerRadius, cornerRadius, paintBorder)
        canvas.drawText(label, width / 2f, height / 2f + paintText.textSize / 3, paintText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                pressDownTime = SystemClock.uptimeMillis()
                sendKeyDown()
                postDelayed(longPressRunnable, 500)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                removeCallbacks(longPressRunnable)
                invalidate()
                sendKeyUp()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                removeCallbacks(longPressRunnable)
                sendKeyUp()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendKeyDown() {
        KeyInjectionService.instance?.injectKeyDown(keyCode, pressDownTime)
            ?: Log.w("ButtonView", "KeyInjectionService not available")
    }

    private fun sendKeyUp() {
        KeyInjectionService.instance?.injectKeyUp(keyCode, pressDownTime)
            ?: Log.w("ButtonView", "KeyInjectionService not available")
    }

    fun isInsideShape(localX: Float, localY: Float): Boolean {
        val cornerRadius = height / 4f
        val left = 4f
        val right = width - 4f
        val top = 4f
        val bottom = height - 4f
        if (localX < left || localX > right || localY < top || localY > bottom) return false
        val dx = maxOf(left + cornerRadius - localX, 0f, localX - (right - cornerRadius))
        val dy = maxOf(top + cornerRadius - localY, 0f, localY - (bottom - cornerRadius))
        return dx * dx + dy * dy <= cornerRadius * cornerRadius
    }

    fun setKeyCode(code: Int) {
        keyCode = code
    }
}
