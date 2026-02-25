package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log
import de.codevoid.andremote2.KeyEventLog
import de.codevoid.andremote2.KeyInjectionService
import de.codevoid.andremote2.UhidBridge
import kotlin.math.abs
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keycodeUp = 19
    private var keycodeDown = 20
    private var keycodeLeft = 21
    private var keycodeRight = 22

    private var uhidMode = false

    private val paintBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.FILL
    }
    private val paintKnob = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")
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

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f
    private var currentKeyCode = -1

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = minOf(w, h) / 2f - 8f
        knobRadius = baseRadius * 0.4f
        knobX = centerX
        knobY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawCircle(centerX, centerY, baseRadius, paintBase)
        canvas.drawCircle(centerX, centerY, baseRadius, paintBorder)

        drawArrow(canvas, centerX, centerY - baseRadius * 0.6f, 0f)
        drawArrow(canvas, centerX, centerY + baseRadius * 0.6f, 180f)
        drawArrow(canvas, centerX - baseRadius * 0.6f, centerY, 270f)
        drawArrow(canvas, centerX + baseRadius * 0.6f, centerY, 90f)

        canvas.drawCircle(knobX, knobY, knobRadius, paintKnob)
        canvas.drawCircle(knobX, knobY, knobRadius, paintBorder)
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, rotation: Float) {
        val size = baseRadius * 0.15f
        val path = Path().apply {
            moveTo(x, y - size)
            lineTo(x + size, y + size)
            lineTo(x - size, y + size)
            close()
        }
        canvas.save()
        canvas.rotate(rotation, x, y)
        canvas.drawPath(path, paintArrow)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt(dx * dx + dy * dy)

                val maxDist = baseRadius - knobRadius
                val ratio = if (dist > maxDist) maxDist / dist else 1f
                knobX = centerX + dx * ratio
                knobY = centerY + dy * ratio
                invalidate()

                if (uhidMode && UhidBridge.isRunning) {
                    sendAnalog(dx, dy, dist)
                } else {
                    if (dist > baseRadius * 0.3f) {
                        val direction = getDirection(dx, dy)
                        if (direction != currentKeyCode) {
                            if (currentKeyCode != -1) sendKeyUp(currentKeyCode)
                            currentKeyCode = direction
                            sendKeyDown(direction)
                        }
                    } else {
                        if (currentKeyCode != -1) sendKeyUp(currentKeyCode)
                        currentKeyCode = -1
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (uhidMode && UhidBridge.isRunning) {
                    UhidBridge.updateJoystick(0, 0)
                } else {
                    if (currentKeyCode != -1) sendKeyUp(currentKeyCode)
                    currentKeyCode = -1
                }
                knobX = centerX
                knobY = centerY
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendAnalog(dx: Float, dy: Float, dist: Float) {
        val deadZone = baseRadius * 0.3f
        if (dist <= deadZone) {
            UhidBridge.updateJoystick(0, 0)
            return
        }
        val active = (baseRadius - knobRadius) - deadZone
        val scale = if (active > 0f) ((dist - deadZone) / active).coerceIn(0f, 1f) else 1f
        val nx = if (dist > 0f) dx / dist else 0f
        val ny = if (dist > 0f) dy / dist else 0f
        val axisX = (nx * scale * 127f).toInt().coerceIn(-127, 127).toByte()
        val axisY = (ny * scale * 127f).toInt().coerceIn(-127, 127).toByte()
        KeyEventLog.log("JoystickView", "analog x=$axisX y=$axisY")
        UhidBridge.updateJoystick(axisX, axisY)
    }

    private fun getDirection(dx: Float, dy: Float): Int {
        return if (abs(dx) > abs(dy)) {
            if (dx > 0) keycodeRight else keycodeLeft
        } else {
            if (dy > 0) keycodeDown else keycodeUp
        }
    }

    private fun sendKeyDown(keyCode: Int) {
        KeyEventLog.log("JoystickView", "sendKeyDown keyCode=$keyCode shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKeyDown(keyCode)
            ?: Log.w("JoystickView", "KeyInjectionService not available")
    }

    private fun sendKeyUp(keyCode: Int) {
        KeyEventLog.log("JoystickView", "sendKeyUp keyCode=$keyCode shizukuEnabled=${KeyInjectionService.shizukuEnabled}")
        KeyInjectionService.instance?.injectKeyUp(keyCode)
            ?: Log.w("JoystickView", "KeyInjectionService not available")
    }

    fun isInsideShape(localX: Float, localY: Float): Boolean {
        val dx = localX - centerX
        val dy = localY - centerY
        return dx * dx + dy * dy <= baseRadius * baseRadius
    }

    fun setKeyCodes(up: Int, down: Int, left: Int, right: Int) {
        keycodeUp = up
        keycodeDown = down
        keycodeLeft = left
        keycodeRight = right
    }

    fun setUhidMode(enabled: Boolean) {
        uhidMode = enabled
    }
}
