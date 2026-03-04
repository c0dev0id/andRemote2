package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            serviceInfo = serviceInfo.apply {
                flags = flags or AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun injectKey(keyCode: Int) {
        injectKeyDown(keyCode)
        injectKeyUp(keyCode)
    }

    fun injectKeyDown(keyCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val conn = getInputMethod()?.getCurrentInputConnection()
            if (conn != null) {
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                return
            }
        }
        val globalAction = keyCodeToGlobalAction(keyCode)
        if (globalAction != null) {
            performGlobalAction(globalAction)
        } else {
            Log.w("KeyInjectionService",
                "Cannot inject keyCode=$keyCode: no input connection and no global action mapping")
        }
    }

    fun injectKeyUp(keyCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val conn = getInputMethod()?.getCurrentInputConnection()
            if (conn != null) {
                conn.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                return
            }
        }
        Log.d("KeyInjectionService",
            "injectKeyUp ignored for keyCode=$keyCode (API < 33 or no input connection)")
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
