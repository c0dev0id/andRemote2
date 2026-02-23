package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import de.codevoid.andremote2.KeyInjectionService

class LeverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keycodeUp = 136
    private var keycodeDown = 137

    private var currentKeyCode = -1
    private var startY = 0f
    private var leverY = 0f

    private val paintTrack = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.FILL
    }
    private val paintLever = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#777777")
        style = Paint.Style.FILL
    }
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val paintArrow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")
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
                    if (currentKeyCode != -1) {
                        sendKeyUp(currentKeyCode)
                    }
                    currentKeyCode = newKeyCode
                    if (newKeyCode != -1) {
                        sendKeyDown(newKeyCode)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (currentKeyCode != -1) {
                    sendKeyUp(currentKeyCode)
                }
                currentKeyCode = -1
                leverY = height / 2f
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendKeyDown(keyCode: Int) {
        val service = KeyInjectionService.instance
        if (service != null) {
            service.injectKeyDown(keyCode)
        } else {
            Log.w("LeverView", "KeyInjectionService not available")
        }
    }

    private fun sendKeyUp(keyCode: Int) {
        val service = KeyInjectionService.instance
        if (service != null) {
            service.injectKeyUp(keyCode)
        } else {
            Log.w("LeverView", "KeyInjectionService not available")
        }
    }

    fun setKeyCodes(up: Int, down: Int) {
        keycodeUp = up
        keycodeDown = down
    }
}
