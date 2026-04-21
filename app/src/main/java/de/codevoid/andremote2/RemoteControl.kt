package de.codevoid.andremote2

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

object RemoteControl {
    private const val ACTION = "com.thorkracing.wireddevices.keypress"
    private const val DEVICE_NAME = "Remote2"
    private const val INJECT_ACTION = "hk.topicon.injectinput.INPUT_EVENT"

    @Volatile var isDMD2InView: Boolean = false
    @Volatile var foregroundPackage: String = ""

    private val NAV_APPS = setOf(
        "com.bodunov.galileo", "com.mapitare.scandinavia", "com.thorkracing.advsos",
        "net.osmand", "menion.android.locus", "com.RallySystem", "com.waze",
        "com.vecturagames.android.app.gpxviewer", "uk.rdzl.topo.gps",
        "com.trailbehind.android.gaiagps.pro", "com.google.android.apps.maps",
        "com.kurviger.app", "com.rallynavigator.rallyroadbookreader",
        "com.eroadbook", "io.roadcaptain.app", "net.osmand.plus",
        "com.garmin.pnd.hydra", "com.bodunov.GalileoPro",
        "com.vecturagames.android.app.gpxviewer.pro"
    )

    private val REPEAT_CODES = setOf(19, 20, 21, 22, 29, 111, 136, 137)

    private val repeatHandler = Handler(Looper.getMainLooper())
    private var repeatContext: Context? = null
    private var injectedKeyCode: Int = -1

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val ctx = repeatContext ?: return
            injectKey(ctx, injectedKeyCode, true)
            repeatHandler.postDelayed(this, 250)
        }
    }

    fun sendPress(context: Context, keyCode: Int) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra(if (isDMD2InView) "down-r2" else "key_press", keyCode)
            putExtra("deviceName", DEVICE_NAME)
        })
        if (shouldInject()) {
            injectedKeyCode = keyCode
            injectKey(context, keyCode, true)
            if (keyCode in REPEAT_CODES) {
                repeatHandler.removeCallbacks(repeatRunnable)
                repeatContext = context
                repeatHandler.postDelayed(repeatRunnable, 250)
            }
        }
    }

    fun sendRelease(context: Context, keyCode: Int) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra(if (isDMD2InView) "up-r2" else "key_release", keyCode)
            putExtra("deviceName", DEVICE_NAME)
        })
        if (injectedKeyCode == keyCode) {
            repeatHandler.removeCallbacks(repeatRunnable)
            repeatContext = null
            injectKey(context, keyCode, false)
            injectedKeyCode = -1
        }
    }

    fun sendJoy(context: Context, joy: String) {
        context.sendBroadcast(Intent(ACTION).apply {
            putExtra("joy", joy)
            putExtra("deviceName", DEVICE_NAME)
        })
    }

    private fun shouldInject(): Boolean {
        if (isDMD2InView) return false
        val pkg = foregroundPackage
        if (pkg.contains("com.rallymoto")) return false
        return pkg !in NAV_APPS
    }

    @SuppressLint("WrongConstant")
    private fun injectKey(context: Context, keyCode: Int, pressed: Boolean) {
        val now = SystemClock.uptimeMillis()
        val action = if (pressed) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP
        val downTime = if (pressed) now else 0L
        context.sendBroadcast(Intent(INJECT_ACTION).apply {
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            addFlags(0x01000000)  // FLAG_RECEIVER_INCLUDE_BACKGROUND (@hide)
            putExtra("event", KeyEvent(downTime, now, action, keyCode, 0, 0))
        })
    }
}
