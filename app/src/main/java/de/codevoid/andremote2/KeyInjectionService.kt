package de.codevoid.andremote2

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import rikka.shizuku.ShizukuBinderWrapper

class KeyInjectionService : Service() {

    companion object {
        private const val TAG = "KeyInjectionService"
        @Volatile var instance: KeyInjectionService? = null
            private set
        @Volatile var shizukuEnabled = false

        private val inputManager: Any? by lazy {
            try {
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
                val binder = getServiceMethod.invoke(null, "input") as? IBinder ?: return@lazy null
                val wrappedBinder = ShizukuBinderWrapper(binder)
                val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
                val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
                asInterfaceMethod.invoke(null, wrappedBinder)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize IInputManager binder", e)
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
            val downTime = SystemClock.uptimeMillis()
            bgHandler.post {
                injectInputEvent(keyCode, KeyEvent.ACTION_DOWN, downTime)
                injectInputEvent(keyCode, KeyEvent.ACTION_UP, downTime)
            }
        } else {
            Log.w(TAG, "injectKey: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyDown(keyCode: Int, downTime: Long) {
        KeyEventLog.log("KeyInjectionService", "injectKeyDown keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { injectInputEvent(keyCode, KeyEvent.ACTION_DOWN, downTime) }
        } else {
            Log.w(TAG, "injectKeyDown: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyUp(keyCode: Int, downTime: Long) {
        KeyEventLog.log("KeyInjectionService", "injectKeyUp keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post { injectInputEvent(keyCode, KeyEvent.ACTION_UP, downTime) }
        } else {
            Log.w(TAG, "injectKeyUp: Shizuku not enabled, key $keyCode dropped")
        }
    }

    fun injectKeyLongPress(keyCode: Int, downTime: Long) {
        KeyEventLog.log("KeyInjectionService", "injectKeyLongPress keyCode=$keyCode shizukuEnabled=$shizukuEnabled")
        if (shizukuEnabled) {
            bgHandler.post {
                injectInputEvent(keyCode, KeyEvent.ACTION_DOWN, downTime,
                    KeyEvent.FLAG_FROM_SYSTEM or KeyEvent.FLAG_LONG_PRESS, repeatCount = 1)
            }
        } else {
            Log.w(TAG, "injectKeyLongPress: Shizuku not enabled, key $keyCode dropped")
        }
    }

    private fun injectInputEvent(
        keyCode: Int,
        action: Int,
        downTime: Long,
        flags: Int = KeyEvent.FLAG_FROM_SYSTEM,
        repeatCount: Int = 0
    ) {
        val manager = inputManager
        if (manager != null) {
            try {
                val now = SystemClock.uptimeMillis()
                val keyEvent = KeyEvent(
                    downTime, now, action, keyCode,
                    repeatCount, 0, -1, 0,
                    flags,
                    InputDevice.SOURCE_KEYBOARD
                )
                val injectMethod = manager.javaClass.getMethod(
                    "injectInputEvent",
                    android.view.InputEvent::class.java,
                    Int::class.javaPrimitiveType
                )
                injectMethod.invoke(manager, keyEvent, 0)
            } catch (e: Exception) {
                Log.e(TAG, "injectInputEvent via binder failed for keyCode=$keyCode action=$action", e)
            }
        } else {
            Log.w(TAG, "IInputManager not available, key $keyCode action=$action dropped")
        }
    }

}
