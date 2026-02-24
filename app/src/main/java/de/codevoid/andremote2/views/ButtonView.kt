package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.util.Log
import de.codevoid.andremote2.KeyEventLog
import de.codevoid.andremote2.KeyInjectionService

class ButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val LONG_PRESS_THRESHOLD_MS = 500L
    }

    private var keyCode = 66
    private var isPressed = false
    private var pressStartTime = 0L

    private val paintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        style = Paint.Style.FILL
    }
    private val paintPressed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
        style = Paint.Style.FILL
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
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

    var holdMode: Boolean = false

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
                if (holdMode) {
                    sendKeyDown()
                } else {
                    pressStartTime = SystemClock.elapsedRealtime()
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                invalidate()
                if (holdMode) {
                    sendKeyUp()
                } else {
                    val duration = SystemClock.elapsedRealtime() - pressStartTime
                    if (duration >= LONG_PRESS_THRESHOLD_MS) {
                        sendKeyLongPress()
                    } else {
                        sendKey()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                if (holdMode) {
                    sendKeyUp()
                } else {
                    pressStartTime = 0L
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendKey() {
        KeyEventLog.log("ButtonView", "sendKey key=$keyCode label=$label shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKey(keyCode)
            ?: Log.w("ButtonView", "KeyInjectionService not available")
    }

    private fun sendKeyLongPress() {
        KeyEventLog.log("ButtonView", "sendKeyLongPress key=$keyCode label=$label shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKeyLongPress(keyCode)
            ?: Log.w("ButtonView", "KeyInjectionService not available")
    }

    private fun sendKeyDown() {
        KeyEventLog.log("ButtonView", "sendKeyDown key=$keyCode label=$label shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKeyDown(keyCode)
            ?: Log.w("ButtonView", "KeyInjectionService not available")
    }

    private fun sendKeyUp() {
        KeyEventLog.log("ButtonView", "sendKeyUp key=$keyCode label=$label shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKeyUp(keyCode)
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
