package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.hardware.input.InputManager
import android.os.SystemClock
import android.util.Log
import android.view.InputEvent
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
        try {
            val im = getSystemService(INPUT_SERVICE) as InputManager
            // InputManager.injectInputEvent() is a @hide API available since API 16.
            // On API 26+ (minSdk), accessibility services hold INJECT_EVENTS permission,
            // so this call succeeds without special system privileges.
            val method = InputManager::class.java.getDeclaredMethod(
                "injectInputEvent", InputEvent::class.java, Int::class.java
            )
            method.isAccessible = true
            val downTime = SystemClock.uptimeMillis()
            val down = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            val up = KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
            // INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2: block until event is handled
            method.invoke(im, down, 2)
            method.invoke(im, up, 2)
        } catch (e: Exception) {
            Log.w("KeyInjectionService", "injectKey failed: $e")
        }
    }
}
