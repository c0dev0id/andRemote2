package de.codevoid.andremote2.views

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.app.Instrumentation
import java.util.concurrent.Executors

class ButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var keyCode = 66
    private var isPressed = false
    private val handler = Handler(Looper.getMainLooper())
    private val repeatInterval = 500L
    private val instrumentation = Instrumentation()
    private val executor = Executors.newSingleThreadExecutor()

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

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (isPressed) {
                sendKeyEvent()
                handler.postDelayed(this, repeatInterval)
            }
        }
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
                invalidate()
                sendKeyEvent()
                handler.postDelayed(repeatRunnable, repeatInterval)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                invalidate()
                handler.removeCallbacks(repeatRunnable)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun sendKeyEvent() {
        executor.execute {
            try {
                instrumentation.sendKeyDownUpSync(keyCode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setKeyCode(code: Int) {
        keyCode = code
    }
}
