package de.codevoid.andremote2

import android.content.Context
import android.content.Intent

object RemoteControl {
    private const val ACTION = "com.thorkracing.wireddevices.keypress"
    private const val DEVICE_NAME = "Remote2"

    fun sendPress(context: Context, keyCode: Int) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra("key_press", keyCode)
            putExtra("deviceName", DEVICE_NAME)
        })
    }

    fun sendRelease(context: Context, keyCode: Int) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra("key_release", keyCode)
            putExtra("deviceName", DEVICE_NAME)
        })
    }

    fun sendJoy(context: Context, joy: String) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra("joy", joy)
            putExtra("deviceName", DEVICE_NAME)
        })
    }
}
