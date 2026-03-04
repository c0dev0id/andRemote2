package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyInjectionService : AccessibilityService() {

    companion object {
        private const val TAG = "KeyInjectionService"
        var instance: KeyInjectionService? = null
            private set
    }

    private val instrumentation = Instrumentation()
    private lateinit var handlerThread: HandlerThread
    private lateinit var bgHandler: Handler

    override fun onServiceConnected() {
        instance = this
        handlerThread = HandlerThread("KeyInjector").apply { start() }
        bgHandler = Handler(handlerThread.looper)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (::handlerThread.isInitialized) {
            handlerThread.quitSafely()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun injectKey(keyCode: Int) {
        bgHandler.post {
            sendKeySync(KeyEvent.ACTION_DOWN, keyCode)
            sendKeySync(KeyEvent.ACTION_UP, keyCode)
        }
    }

    fun injectKeyDown(keyCode: Int) {
        bgHandler.post { sendKeySync(KeyEvent.ACTION_DOWN, keyCode) }
    }

    fun injectKeyUp(keyCode: Int) {
        bgHandler.post { sendKeySync(KeyEvent.ACTION_UP, keyCode) }
    }

    private fun sendKeySync(action: Int, keyCode: Int) {
        val now = SystemClock.uptimeMillis()
        val event = KeyEvent(now, now, action, keyCode, 0)
        try {
            instrumentation.sendKeySync(event)
        } catch (e: SecurityException) {
            Log.e(TAG, "INJECT_EVENTS permission not granted. " +
                    "Run: adb shell pm grant $packageName android.permission.INJECT_EVENTS", e)
        }
    }
}
