package de.codevoid.andremote2

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

/**
 * Manages the UHID bridge process lifecycle and sends HID reports to the
 * virtual gamepad device.
 *
 * The bridge process (UhidBridgeProcess) is launched via the Shizuku server's
 * newProcess() AIDL call, which runs the process with shell privileges needed
 * to open /dev/uhid.
 *
 * The combined 4-byte HID report is maintained here and updated by the views:
 *   - JoystickView updates X and Y axes
 *   - LeverView updates Z axis
 *   - ButtonView updates individual button bits
 */
object UhidBridge {

    private const val TAG = "UhidBridge"

    @Volatile var isRunning = false
        private set

    private var remoteProcess: moe.shizuku.server.IRemoteProcess? = null
    private var outputStream: java.io.OutputStream? = null

    @Volatile private var axisX: Byte = 0
    @Volatile private var axisY: Byte = 0
    @Volatile private var axisZ: Byte = 0
    @Volatile private var buttons: Byte = 0

    private lateinit var handlerThread: HandlerThread
    private lateinit var bgHandler: Handler

    fun start(context: Context) {
        if (isRunning) return
        if (!::handlerThread.isInitialized || !handlerThread.isAlive) {
            handlerThread = HandlerThread("UhidBridge").apply { start() }
            bgHandler = Handler(handlerThread.looper)
        }
        try {
            val apkPath = context.packageCodePath
            val cmd = arrayOf(
                "app_process",
                "-Djava.class.path=$apkPath",
                "/system/bin",
                "de.codevoid.andremote2.UhidBridgeProcess"
            )
            val binder = Shizuku.getBinder()
                ?: throw Exception("Shizuku binder not available")
            val service = IShizukuService.Stub.asInterface(binder)
            val proc = service.newProcess(cmd, null, null)
            val pfd = proc.outputStream
            outputStream = java.io.FileOutputStream(pfd.fileDescriptor)
            remoteProcess = proc
            isRunning = true
            Log.i(TAG, "UhidBridge started (apk=$apkPath)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start UhidBridge", e)
            isRunning = false
        }
    }

    fun stop() {
        isRunning = false
        try { outputStream?.close() } catch (_: Exception) {}
        try { remoteProcess?.destroy() } catch (_: Exception) {}
        outputStream = null
        remoteProcess = null
        axisX = 0; axisY = 0; axisZ = 0; buttons = 0
        if (::handlerThread.isInitialized) handlerThread.quitSafely()
        Log.i(TAG, "UhidBridge stopped")
    }

    fun updateJoystick(x: Byte, y: Byte) {
        axisX = x
        axisY = y
        sendReport()
    }

    fun updateLever(z: Byte) {
        axisZ = z
        sendReport()
    }

    fun setButton(index: Int, pressed: Boolean) {
        buttons = if (pressed) {
            (buttons.toInt() or (1 shl index)).toByte()
        } else {
            (buttons.toInt() and (1 shl index).inv()).toByte()
        }
        sendReport()
    }

    private fun sendReport() {
        if (!isRunning) return
        val x = axisX; val y = axisY; val z = axisZ; val b = buttons
        bgHandler.post {
            try {
                val os = outputStream ?: return@post
                os.write(byteArrayOf(x, y, z, b))
                os.flush()
            } catch (e: Exception) {
                Log.w(TAG, "sendReport failed: ${e.message}")
                if (remoteProcess?.asBinder()?.isBinderAlive != true) {
                    isRunning = false
                }
            }
        }
    }
}
