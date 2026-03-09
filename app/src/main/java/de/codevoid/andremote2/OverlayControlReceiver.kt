package de.codevoid.andremote2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings

class OverlayControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW = "de.codevoid.andremote2.OVERLAY_SHOW"
        const val ACTION_HIDE = "de.codevoid.andremote2.OVERLAY_HIDE"
        const val ACTION_TOGGLE = "de.codevoid.andremote2.OVERLAY_TOGGLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SHOW -> show(context)
            ACTION_HIDE -> hide(context)
            ACTION_TOGGLE -> if (OverlayService.isRunning) hide(context) else show(context)
        }
    }

    private fun show(context: Context) {
        if (!Settings.canDrawOverlays(context)) return
        if (!OverlayService.isRunning) {
            context.startForegroundService(Intent(context, OverlayService::class.java))
        }
    }

    private fun hide(context: Context) {
        if (OverlayService.isRunning) {
            context.startService(Intent(context, OverlayService::class.java).apply { action = "STOP" })
        }
    }
}
