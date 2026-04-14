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
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        @Volatile var isRunning = false
        const val CHANNEL_ID = "overlay_channel"
        private const val BASE_WIDTH_DP = 242
        private const val BASE_HEIGHT_DP = 426
        private const val HANDLE_SIZE_DP = 48
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var prefs: SharedPreferences
    private lateinit var overlayParams: WindowManager.LayoutParams

    private var baseWidth = 0
    private var baseHeight = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            PrefKeys.OVERLAY_SIZE, PrefKeys.OVERLAY_OPACITY ->
                mainHandler.post { applyScaleAndAlpha() }
            PrefKeys.JOYSTICK_360 ->
                mainHandler.post { applyJoystickMode(prefs) }
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

        val buttonBottom = overlayView.findViewById<de.codevoid.andremote2.views.ButtonView>(R.id.buttonBottom)
        buttonBottom.setKeyCode(111)

        val overlayContent = overlayView.findViewById<de.codevoid.andremote2.views.DraggableOverlayLayout>(R.id.overlayContent)
        overlayContent.onDrag = { dx, dy ->
            overlayParams.x += dx
            overlayParams.y += dy
            windowManager.updateViewLayout(overlayView, overlayParams)
            prefs.edit()
                .putInt(PrefKeys.OVERLAY_X, overlayParams.x)
                .putInt(PrefKeys.OVERLAY_Y, overlayParams.y)
                .apply()
        }

        val handle = overlayView.findViewById<de.codevoid.andremote2.views.CollapseHandleView>(R.id.collapseHandle)
        handle.onDrag = { dx, dy ->
            overlayParams.x += dx
            overlayParams.y += dy
            windowManager.updateViewLayout(overlayView, overlayParams)
            prefs.edit()
                .putInt(PrefKeys.OVERLAY_X, overlayParams.x)
                .putInt(PrefKeys.OVERLAY_Y, overlayParams.y)
                .apply()
        }
        handle.onTap = { toggleMenu() }

        overlayView.findViewById<TextView>(R.id.menuItemCollapse).setOnClickListener {
            toggleCollapsed()
            hideMenu()
        }
        overlayView.findViewById<TextView>(R.id.menuItem360).setOnClickListener {
            toggleJoystick360()
            hideMenu()
        }

        applyJoystickMode(prefs)

        windowManager.addView(overlayView, overlayParams)

        applyCollapsed(prefs.getBoolean(PrefKeys.OVERLAY_COLLAPSED, false))
    }

    private fun toggleMenu() {
        val menu = overlayView.findViewById<View>(R.id.handleMenu)
        if (menu.visibility == View.VISIBLE) hideMenu() else showMenu()
    }

    private fun showMenu() {
        val collapsed = prefs.getBoolean(PrefKeys.OVERLAY_COLLAPSED, false)
        overlayView.findViewById<TextView>(R.id.menuItemCollapse).setText(
            if (collapsed) R.string.menu_expand else R.string.menu_collapse
        )
        val is360 = prefs.getBoolean(PrefKeys.JOYSTICK_360, false)
        overlayView.findViewById<TextView>(R.id.menuItem360).setText(
            if (is360) R.string.menu_360_on else R.string.menu_360_off
        )
        if (collapsed) expandWindowForMenu()
        overlayView.findViewById<View>(R.id.handleMenu).visibility = View.VISIBLE
    }

    private fun hideMenu() {
        overlayView.findViewById<View>(R.id.handleMenu).visibility = View.GONE
        if (prefs.getBoolean(PrefKeys.OVERLAY_COLLAPSED, false)) applyCollapsedWindowSize()
    }

    private fun expandWindowForMenu() {
        val size = prefs.getInt(PrefKeys.OVERLAY_SIZE, 75).coerceIn(10, 200)
        val scale = size / 100f
        overlayParams.width = (baseWidth * scale).toInt()
        overlayParams.height = (baseHeight * scale).toInt()
        windowManager.updateViewLayout(overlayView, overlayParams)
    }

    private fun toggleCollapsed() {
        val collapsed = !prefs.getBoolean(PrefKeys.OVERLAY_COLLAPSED, false)
        prefs.edit().putBoolean(PrefKeys.OVERLAY_COLLAPSED, collapsed).apply()
        applyCollapsed(collapsed)
    }

    private fun applyCollapsed(collapsed: Boolean) {
        overlayView.findViewById<View>(R.id.overlayContent).visibility =
            if (collapsed) View.GONE else View.VISIBLE
        if (collapsed) applyCollapsedWindowSize() else applyScaleAndAlpha()
    }

    private fun applyCollapsedWindowSize() {
        val px = (HANDLE_SIZE_DP * resources.displayMetrics.density + 0.5f).toInt()
        overlayParams.width = px
        overlayParams.height = px
        windowManager.updateViewLayout(overlayView, overlayParams)
    }

    private fun toggleJoystick360() {
        val enabled = !prefs.getBoolean(PrefKeys.JOYSTICK_360, false)
        prefs.edit().putBoolean(PrefKeys.JOYSTICK_360, enabled).apply()
        // prefListener handles applyJoystickMode via the JOYSTICK_360 case
    }

    private fun applyJoystickMode(prefs: SharedPreferences) {
        val joystick = overlayView.findViewById<de.codevoid.andremote2.views.JoystickView>(R.id.joystickView)
        joystick.setMode360(prefs.getBoolean(PrefKeys.JOYSTICK_360, false))
    }

    private fun applyScaleAndAlpha() {
        if (prefs.getBoolean(PrefKeys.OVERLAY_COLLAPSED, false) &&
            overlayView.findViewById<View>(R.id.handleMenu).visibility != View.VISIBLE) return

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
