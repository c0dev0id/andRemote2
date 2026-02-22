package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.app.Instrumentation
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class KeyInjectionService : AccessibilityService() {

    companion object {
        var instance: KeyInjectionService? = null
            private set
    }

    private val instrumentation = Instrumentation()

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
        // Must run off the main thread since sendKeyDownUpSync blocks
        Thread {
            try {
                instrumentation.sendKeyDownUpSync(keyCode)
            } catch (e: Exception) {
                Log.w("KeyInjectionService", "injectKey failed: $e")
            }
        }.start()
    }
}
