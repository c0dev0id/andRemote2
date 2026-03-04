package de.codevoid.andremote2

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        var isRunning = false
        const val CHANNEL_ID = "overlay_channel"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var prefs: SharedPreferences
    private lateinit var overlayParams: WindowManager.LayoutParams

    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "overlay_size" || key == "overlay_opacity") {
            mainHandler.post { applyScaleAndAlpha() }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        prefs = getSharedPreferences("andremote2", MODE_PRIVATE)
        createNotificationChannel()
        startForeground(1, buildNotification())
        startService(Intent(this, KeyInjectionService::class.java))
        showOverlay()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
        stopService(Intent(this, KeyInjectionService::class.java))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "DMD Remote Overlay",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).apply { action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DMD Remote Active")
            .setContentText("Use Stop action to dismiss")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_remote, null)

        // Measure at natural size to calculate scaled window dimensions
        overlayView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        overlayParams = WindowManager.LayoutParams(
            overlayView.measuredWidth,
            overlayView.measuredHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        overlayView.pivotX = 0f
        overlayView.pivotY = 0f
        applyScaleAndAlpha()

        setupControls()

        val root = overlayView.findViewById<de.codevoid.andremote2.views.DraggableOverlayLayout>(R.id.overlayRoot)
        root.onDrag = { dx, dy ->
            overlayParams.x += dx
            overlayParams.y += dy
            windowManager.updateViewLayout(overlayView, overlayParams)
        }

        windowManager.addView(overlayView, overlayParams)
    }

    private fun applyScaleAndAlpha() {
        val size = prefs.getInt("overlay_size", 75).coerceIn(10, 200)
        val opacity = prefs.getInt("overlay_opacity", 80).coerceIn(0, 100)
        val scale = size / 100f

        overlayView.alpha = opacity / 100f
        overlayView.scaleX = scale
        overlayView.scaleY = scale

        // Don't also resize the window — the view scale already handles it.
        // Keep window at natural measured size so the scaled view fits.

        if (::overlayView.isInitialized && ::overlayParams.isInitialized &&
            ::windowManager.isInitialized && overlayView.isAttachedToWindow) {
            windowManager.updateViewLayout(overlayView, overlayParams)
        }
    }

    private fun setupControls() {
        val joystick = overlayView.findViewById<de.codevoid.andremote2.views.JoystickView>(R.id.joystickView)
        val buttonTop = overlayView.findViewById<de.codevoid.andremote2.views.ButtonView>(R.id.buttonTop)
        val buttonBottom = overlayView.findViewById<de.codevoid.andremote2.views.ButtonView>(R.id.buttonBottom)
        val lever = overlayView.findViewById<de.codevoid.andremote2.views.LeverView>(R.id.leverView)

        joystick.setKeyCodes(
            prefs.getInt("keycode_joystick_up", 19),
            prefs.getInt("keycode_joystick_down", 20),
            prefs.getInt("keycode_joystick_left", 21),
            prefs.getInt("keycode_joystick_right", 22)
        )

        buttonTop.label = "TOP"
        buttonTop.setKeyCode(prefs.getInt("keycode_button_top", 66))

        buttonBottom.label = "BTM"
        buttonBottom.setKeyCode(prefs.getInt("keycode_button_bottom", 111))

        lever.setKeyCodes(
            prefs.getInt("keycode_lever_up", 136),
            prefs.getInt("keycode_lever_down", 137)
        )
    }

}
