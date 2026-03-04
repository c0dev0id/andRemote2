package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyInjectionService : AccessibilityService() {

    companion object {
        var instance: KeyInjectionService? = null
            private set
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
            val success = performGlobalAction(globalAction)
            if (!success) {
                Log.w("KeyInjectionService",
                    "performGlobalAction failed for keyCode=$keyCode action=$globalAction")
            }
        } else {
            Log.w("KeyInjectionService",
                "No global-action mapping for keyCode=$keyCode")
        }
    }

    private fun keyCodeToGlobalAction(keyCode: Int): Int? {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP      -> GLOBAL_ACTION_DPAD_UP
            KeyEvent.KEYCODE_DPAD_DOWN    -> GLOBAL_ACTION_DPAD_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT    -> GLOBAL_ACTION_DPAD_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT   -> GLOBAL_ACTION_DPAD_RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER  -> GLOBAL_ACTION_DPAD_CENTER
            KeyEvent.KEYCODE_ENTER        -> GLOBAL_ACTION_DPAD_CENTER
            KeyEvent.KEYCODE_BACK         -> GLOBAL_ACTION_BACK
            KeyEvent.KEYCODE_HOME         -> GLOBAL_ACTION_HOME
            KeyEvent.KEYCODE_ESCAPE       -> GLOBAL_ACTION_BACK
            else -> null
        }
    }
}
