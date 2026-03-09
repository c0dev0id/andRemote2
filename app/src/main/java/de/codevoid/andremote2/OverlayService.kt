package de.codevoid.andremote2

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        @Volatile var isRunning = false
        const val CHANNEL_ID = "overlay_channel"
        private const val BASE_WIDTH_DP = 242
        private const val BASE_HEIGHT_DP = 426
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var prefs: SharedPreferences
    private lateinit var overlayParams: WindowManager.LayoutParams

    private var baseWidth = 0
    private var baseHeight = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PrefKeys.OVERLAY_SIZE || key == PrefKeys.OVERLAY_OPACITY) {
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
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.notification_action_stop), stopIntent)
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

        val density = resources.displayMetrics.density
        baseWidth = (BASE_WIDTH_DP * density + 0.5f).toInt()
        baseHeight = (BASE_HEIGHT_DP * density + 0.5f).toInt()

        val size = prefs.getInt(PrefKeys.OVERLAY_SIZE, 75).coerceIn(10, 200)
        val scale = size / 100f

        overlayParams = WindowManager.LayoutParams(
            (baseWidth * scale).toInt(),
            (baseHeight * scale).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt(PrefKeys.OVERLAY_X, 100)
            y = prefs.getInt(PrefKeys.OVERLAY_Y, 200)
        }

        applyScaleAndAlpha()

        // Set bottom button to keycode 111 (ROUND BUTTON 2 per protocol)
        val buttonBottom = overlayView.findViewById<de.codevoid.andremote2.views.ButtonView>(R.id.buttonBottom)
        buttonBottom.setKeyCode(111)

        val root = overlayView.findViewById<de.codevoid.andremote2.views.DraggableOverlayLayout>(R.id.overlayRoot)
        root.onDrag = { dx, dy ->
            overlayParams.x += dx
            overlayParams.y += dy
            windowManager.updateViewLayout(overlayView, overlayParams)
            prefs.edit()
                .putInt(PrefKeys.OVERLAY_X, overlayParams.x)
                .putInt(PrefKeys.OVERLAY_Y, overlayParams.y)
                .apply()
        }

        windowManager.addView(overlayView, overlayParams)
    }

    private fun applyScaleAndAlpha() {
        val size = prefs.getInt(PrefKeys.OVERLAY_SIZE, 75).coerceIn(10, 200)
        val opacity = prefs.getInt(PrefKeys.OVERLAY_OPACITY, 80).coerceIn(0, 100)
        val scale = size / 100f

        overlayView.alpha = opacity / 100f

        overlayParams.width = (baseWidth * scale).toInt()
        overlayParams.height = (baseHeight * scale).toInt()

        if (::overlayView.isInitialized && ::overlayParams.isInitialized &&
            ::windowManager.isInitialized && overlayView.isAttachedToWindow) {
            windowManager.updateViewLayout(overlayView, overlayParams)
        }
    }

}
