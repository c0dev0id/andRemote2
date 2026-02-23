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
        if (shizukuEnabled) {
            bgHandler.post { runShizukuKeyEvent(keyCode) }
        } else {
            Log.w(TAG, "injectKey: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyLongPress(keyCode: Int) {
        if (shizukuEnabled) {
            bgHandler.post { runShizukuKeyEvent(keyCode, "--longpress") }
        } else {
            Log.w(TAG, "injectKeyLongPress: Shizuku not enabled, key $keyCode dropped")
        }
    }

    private fun runShizukuKeyEvent(keyCode: Int, action: String? = null) {
        try {
            val method = shizukuNewProcess ?: return
            val args = if (action != null) {
                arrayOf("input", "keyevent", action, keyCode.toString())
            } else {
                arrayOf("input", "keyevent", keyCode.toString())
            }
            val process = method.invoke(
                null,
                args,
                null as Array<String>?,
                null as String?
            ) as Process
            process.waitFor(SHIZUKU_PROCESS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku key injection failed for keyCode $keyCode", e)
        }
    }

}
