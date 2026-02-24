package de.codevoid.andremote2

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit

class KeyInjectionService : Service() {

    companion object {
        private const val TAG = "KeyInjectionService"
        private const val SHIZUKU_PROCESS_TIMEOUT_MS = 2000L
        var instance: KeyInjectionService? = null
            private set
        @Volatile var shizukuEnabled = false

        private val shizukuNewProcess: Method? by lazy {
            try {
                Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                ).also { it.isAccessible = true }
            } catch (e: Exception) {
                null
            }
        }
    }

    private lateinit var handlerThread: HandlerThread
    private lateinit var bgHandler: Handler

    override fun onCreate() {
        super.onCreate()
        instance = this
        handlerThread = HandlerThread("KeyInjector").apply { start() }
        bgHandler = Handler(handlerThread.looper)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        if (::handlerThread.isInitialized) {
            handlerThread.quitSafely()
        }
    }

    fun injectKey(keyCode: Int) {
        KeyEventLog.log("KeyInjectionService", "injectKey keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { runShizukuKeyEvent(keyCode) }
        } else {
            Log.w(TAG, "injectKey: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyLongPress(keyCode: Int) {
        KeyEventLog.log("KeyInjectionService", "injectKeyLongPress keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { runShizukuKeyEvent(keyCode, "--longpress") }
        } else {
            Log.w(TAG, "injectKeyLongPress: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyDown(keyCode: Int) {
        KeyEventLog.log("KeyInjectionService", "injectKeyDown keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { runShizukuInputCommand("input", "keyevent", "--down", keyCode.toString()) }
        } else {
            Log.w(TAG, "injectKeyDown: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyUp(keyCode: Int) {
        KeyEventLog.log("KeyInjectionService", "injectKeyUp keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { runShizukuInputCommand("input", "keyevent", "--up", keyCode.toString()) }
        } else {
            Log.w(TAG, "injectKeyUp: Shizuku not enabled, key $keyCode dropped")
        }
    }

    private fun runShizukuKeyEvent(keyCode: Int, action: String? = null) {
        val args = if (action != null) {
            arrayOf("input", "keyevent", action, keyCode.toString())
        } else {
            arrayOf("input", "keyevent", keyCode.toString())
        }
        runShizukuInputCommand(*args)
    }

    private fun runShizukuInputCommand(vararg args: String) {
        try {
            val method = shizukuNewProcess ?: return
            val process = method.invoke(
                null,
                arrayOf(*args),
                null as Array<String>?,
                null as String?
            ) as Process
            process.waitFor(SHIZUKU_PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku command failed: ${args.joinToString(" ")}", e)
        }
    }

}
