package de.codevoid.andremote2

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class RemoteAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var isRunning = false
    }

    override fun onServiceConnected() {
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        RemoteControl.foregroundPackage = pkg
        RemoteControl.isDMD2InView =
            pkg == "com.thorkracing.dmd2launcher" || pkg == "com.thorkracing.dmdplayground"
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        RemoteControl.foregroundPackage = ""
        RemoteControl.isDMD2InView = false
    }
}
