package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import rikka.shizuku.Shizuku
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class KeyInjectionService : AccessibilityService() {

    companion object {
        private const val TAG = "KeyInjectionService"
        private const val KEY_REPEAT_DELAY_MS = 100L
        private const val SHIZUKU_PROCESS_TIMEOUT_MS = 2000L
        var instance: KeyInjectionService? = null
            private set
        @Volatile var shizukuEnabled = false

        private val shizukuNewProcess: Method? by lazy {
            try {
                Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }
    }

    private val instrumentation = Instrumentation()
    private lateinit var handlerThread: HandlerThread
    private lateinit var bgHandler: Handler
    private val shizukuRepeatRunnables = mutableMapOf<Int, Runnable>()

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
        if (hasInjectEventsPermission()) {
            bgHandler.post {
                sendKeySync(KeyEvent.ACTION_DOWN, keyCode)
                sendKeySync(KeyEvent.ACTION_UP, keyCode)
            }
        } else if (shizukuEnabled) {
            bgHandler.post { runShizukuKeyEvent(keyCode) }
        }
    }

    fun injectKeyDown(keyCode: Int) {
        if (hasInjectEventsPermission()) {
            bgHandler.post { sendKeySync(KeyEvent.ACTION_DOWN, keyCode) }
        } else if (shizukuEnabled) {
            bgHandler.post { startShizukuKeyRepeat(keyCode) }
        }
    }

    fun injectKeyUp(keyCode: Int) {
        if (hasInjectEventsPermission()) {
            bgHandler.post { sendKeySync(KeyEvent.ACTION_UP, keyCode) }
        } else if (shizukuEnabled) {
            bgHandler.post { stopShizukuKeyRepeat(keyCode) }
        }
    }

    private fun hasInjectEventsPermission(): Boolean {
        return checkSelfPermission("android.permission.INJECT_EVENTS") == PackageManager.PERMISSION_GRANTED
    }

    private fun startShizukuKeyRepeat(keyCode: Int) {
        stopShizukuKeyRepeat(keyCode)
        val runnable = object : Runnable {
            override fun run() {
                if (!shizukuRepeatRunnables.containsKey(keyCode)) return
                runShizukuKeyEvent(keyCode)
                if (shizukuRepeatRunnables.containsKey(keyCode)) {
                    bgHandler.postDelayed(this, KEY_REPEAT_DELAY_MS)
                }
            }
        }
        shizukuRepeatRunnables[keyCode] = runnable
        bgHandler.post(runnable)
    }

    private fun stopShizukuKeyRepeat(keyCode: Int) {
        shizukuRepeatRunnables.remove(keyCode)?.let { bgHandler.removeCallbacks(it) }
    }

    private fun runShizukuKeyEvent(keyCode: Int) {
        try {
            val method = shizukuNewProcess ?: return
            val process = method.invoke(
                null,
                arrayOf("input", "keyevent", keyCode.toString()),
                null as Array<String>?,
                null as String?
            ) as Process
            process.waitFor(SHIZUKU_PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku key injection failed for keyCode $keyCode", e)
        }
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
