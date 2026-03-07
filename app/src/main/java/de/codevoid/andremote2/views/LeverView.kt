package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import androidx.core.content.ContextCompat
import de.codevoid.andremote2.KeyInjectionService
import de.codevoid.andremote2.R

class LeverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keycodeUp = 136
    private var keycodeDown = 137

    private var currentKeyCode = -1
    private var pressDownTime = 0L
    private var startY = 0f
    private var leverY = 0f

    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_track)
        style = Paint.Style.FILL
    }
    private val paintLever = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_lever)
        style = Paint.Style.FILL
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_border)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.control_arrow)
        style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        leverY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        val trackWidth = width * 0.4f
        val trackLeft = (width - trackWidth) / 2f
        val trackRight = trackLeft + trackWidth
        val cornerR = trackWidth / 2f

        canvas.drawRoundRect(trackLeft, 8f, trackRight, height - 8f, cornerR, cornerR, paintTrack)
        canvas.drawRoundRect(trackLeft, 8f, trackRight, height - 8f, cornerR, cornerR, paintBorder)

        val arrowSize = trackWidth * 0.3f
        val cx = width / 2f
        drawTriangle(canvas, cx, height * 0.2f, arrowSize, true, paintArrow)
        drawTriangle(canvas, cx, height * 0.8f, arrowSize, false, paintArrow)

        val knobHeight = height * 0.15f
        val knobTop = leverY - knobHeight / 2f
        val knobBottom = leverY + knobHeight / 2f
        canvas.drawRoundRect(
            trackLeft - 8f, knobTop, trackRight + 8f, knobBottom,
            cornerR, cornerR, paintLever
        )
        canvas.drawRoundRect(
            trackLeft - 8f, knobTop, trackRight + 8f, knobBottom,
            cornerR, cornerR, paintBorder
        )
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, size: Float, up: Boolean, paint: Paint) {
        val path = Path()
        if (up) {
            path.moveTo(cx, cy - size)
            path.lineTo(cx + size, cy + size)
            path.lineTo(cx - size, cy + size)
        } else {
            path.moveTo(cx, cy + size)
            path.lineTo(cx + size, cy - size)
            path.lineTo(cx - size, cy - size)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                leverY = event.y.coerceIn(height * 0.1f, height * 0.9f)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - startY
                leverY = event.y.coerceIn(height * 0.1f, height * 0.9f)
                invalidate()

                val threshold = height * 0.15f
                val newKeyCode = when {
                    dy < -threshold -> keycodeUp
                    dy > threshold -> keycodeDown
                    else -> -1
                }

                if (newKeyCode != currentKeyCode) {
                    if (currentKeyCode != -1) sendKeyUp(currentKeyCode)
                    currentKeyCode = newKeyCode
                    if (newKeyCode != -1) sendKeyDown(newKeyCode)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (currentKeyCode != -1) sendKeyUp(currentKeyCode)
                currentKeyCode = -1
                leverY = height / 2f
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendKeyDown(keyCode: Int) {
        pressDownTime = SystemClock.uptimeMillis()
        KeyInjectionService.instance?.injectKeyDown(keyCode, pressDownTime)
            ?: Log.w("LeverView", "KeyInjectionService not available")
    }

    private fun sendKeyUp(keyCode: Int) {
        KeyInjectionService.instance?.injectKeyUp(keyCode, pressDownTime)
            ?: Log.w("LeverView", "KeyInjectionService not available")
    }

    fun isInsideShape(localX: Float, localY: Float): Boolean {
        val trackWidth = width * 0.4f
        val left = (width - trackWidth) / 2f - 8f
        val right = (width + trackWidth) / 2f + 8f
        val top = 8f
        val bottom = height - 8f
        val cornerR = trackWidth / 2f
        if (localX < left || localX > right || localY < top || localY > bottom) return false
        val dx = maxOf(left + cornerR - localX, 0f, localX - (right - cornerR))
        val dy = maxOf(top + cornerR - localY, 0f, localY - (bottom - cornerR))
        return dx * dx + dy * dy <= cornerR * cornerR
    }

    fun setKeyCodes(up: Int, down: Int) {
        keycodeUp = up
        keycodeDown = down
    }
}
