package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.TimeUnit

class KeyInjectionService : AccessibilityService() {

    companion object {
        var instance: KeyInjectionService? = null
            private set

        private const val TAG = "KeyInjectionService"
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun injectKey(keyCode: Int) {
        val globalAction = keyCodeToGlobalAction(keyCode)
        if (globalAction != null) {
            if (!performGlobalAction(globalAction)) {
                Log.w(TAG, "performGlobalAction($globalAction) returned false for keyCode=$keyCode")
            }
        } else {
            injectViaInputCommand(keyCode)
        }
    }

    private fun injectViaInputCommand(keyCode: Int) {
        Thread {
            try {
                val process = Runtime.getRuntime()
                    .exec(arrayOf("input", "keyevent", keyCode.toString()))
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroy()
                    Log.w(TAG, "input keyevent $keyCode timed out")
                } else if (process.exitValue() != 0) {
                    Log.w(TAG, "input keyevent $keyCode exited with code ${process.exitValue()}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "input keyevent $keyCode failed: $e")
            }
        }.start()
    }

    /**
     * Only map keycodes where performGlobalAction injects the exact same
     * keycode — no substitutions — so hardware-emulation fidelity is preserved.
     */
    private fun keyCodeToGlobalAction(keyCode: Int): Int? {
        if (Build.VERSION.SDK_INT >= 34) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP     -> return GLOBAL_ACTION_DPAD_UP
                KeyEvent.KEYCODE_DPAD_DOWN   -> return GLOBAL_ACTION_DPAD_DOWN
                KeyEvent.KEYCODE_DPAD_LEFT   -> return GLOBAL_ACTION_DPAD_LEFT
                KeyEvent.KEYCODE_DPAD_RIGHT  -> return GLOBAL_ACTION_DPAD_RIGHT
                KeyEvent.KEYCODE_DPAD_CENTER -> return GLOBAL_ACTION_DPAD_CENTER
            }
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> GLOBAL_ACTION_BACK
            else -> null
        }
    }
}
