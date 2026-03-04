package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KeyInjectionService : AccessibilityService() {

    companion object {
        var instance: KeyInjectionService? = null
            private set
    }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.shutdownNow()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun injectKey(keyCode: Int) {
        val globalAction = keyCodeToGlobalAction(keyCode)
        if (globalAction != null) {
            val success = performGlobalAction(globalAction)
            if (!success) {
                Log.w("KeyInjectionService",
                    "performGlobalAction failed for keyCode=$keyCode action=$globalAction")
            }
        } else {
            executor.execute {
                try {
                    Runtime.getRuntime()
                        .exec(arrayOf("input", "keyevent", keyCode.toString()))
                        .waitFor()
                } catch (e: Exception) {
                    Log.w("KeyInjectionService",
                        "input keyevent failed for keyCode=$keyCode", e)
                }
            }
        }
    }

    private fun keyCodeToGlobalAction(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK    -> GLOBAL_ACTION_BACK
            KeyEvent.KEYCODE_HOME    -> GLOBAL_ACTION_HOME
            KeyEvent.KEYCODE_ESCAPE  -> GLOBAL_ACTION_BACK
            else -> null
        }
    }
}
