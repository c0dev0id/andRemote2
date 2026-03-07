package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var presetManager: RemotePresetManager
    private lateinit var activePreset: RemoteKeyPreset

    private lateinit var btnToggleOverlay: MaterialButton
    private lateinit var btnGrantShizuku: MaterialButton
    private lateinit var sliderSize: Slider
    private lateinit var sliderOpacity: Slider
    private lateinit var tvSize: TextView
    private lateinit var tvOpacity: TextView

    // Key mapping UI
    private lateinit var togglePreset: MaterialButtonToggleGroup
    private lateinit var btnMapRemote: MaterialButton
    private lateinit var remoteKeyTable: LinearLayout

    // Wizard
    private lateinit var wizardPanel: View
    private var wizardVisible = false
    private lateinit var wizardCapturePresetName: TextView
    private lateinit var wizardCaptureProgress: TextView
    private lateinit var wizardCaptureAction: TextView
    private lateinit var wizardCaptureCurrent: TextView
    private lateinit var wizardSkipButton: MaterialButton

    private var isCapturingKey = false
    private var captureTargetPreset: RemoteKeyPreset? = null
    private var captureActionIndex = 0
    private var captureKeycodes = IntArray(RemoteKeyPreset.ACTION_COUNT)

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    onShizukuPermissionGranted()
                } else {
                    Toast.makeText(this, R.string.shizuku_permission_denied, Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("andremote2", MODE_PRIVATE)
        presetManager = RemotePresetManager(prefs)
        activePreset = presetManager.getActivePreset()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        btnGrantShizuku = findViewById(R.id.btnGrantShizuku)
        sliderSize = findViewById(R.id.sliderSize)
        sliderOpacity = findViewById(R.id.sliderOpacity)
        tvSize = findViewById(R.id.tvSize)
        tvOpacity = findViewById(R.id.tvOpacity)
        togglePreset = findViewById(R.id.togglePreset)
        btnMapRemote = findViewById(R.id.btnMapRemote)
        remoteKeyTable = findViewById(R.id.remoteKeyTable)

        wizardPanel = findViewById(R.id.wizardPanel)
        wizardCapturePresetName = wizardPanel.findViewById(R.id.wizard_capture_preset_name)
        wizardCaptureProgress = wizardPanel.findViewById(R.id.wizard_capture_progress)
        wizardCaptureAction = wizardPanel.findViewById(R.id.wizard_capture_action)
        wizardCaptureCurrent = wizardPanel.findViewById(R.id.wizard_capture_current)
        wizardSkipButton = wizardPanel.findViewById(R.id.btn_wizard_skip)

        setupPresetToggle()
        loadSettings()
        updateKeyAssignmentTable()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        wizardSkipButton.setOnClickListener { skipCurrentAction() }

        sliderSize.addOnChangeListener { _, value, fromUser ->
            val size = value.toInt()
            tvSize.text = "$size%"
            if (fromUser) prefs.edit().putInt(PrefKeys.OVERLAY_SIZE, size).apply()
        }

        sliderOpacity.addOnChangeListener { _, value, fromUser ->
            val opacity = value.toInt()
            tvOpacity.text = "$opacity%"
            if (fromUser) prefs.edit().putInt(PrefKeys.OVERLAY_OPACITY, opacity).apply()
        }

        btnToggleOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (!isShizukuAuthorized()) {
                onGrantShizukuClicked()
                return@setOnClickListener
            }
            if (OverlayService.isRunning) {
                stopService(Intent(this, OverlayService::class.java))
                btnToggleOverlay.text = getString(R.string.start_overlay)
            } else {
                startForegroundService(Intent(this, OverlayService::class.java))
                btnToggleOverlay.text = getString(R.string.stop_overlay)
            }
        }

        btnGrantShizuku.setOnClickListener {
            onGrantShizukuClicked()
        }

        btnMapRemote.setOnClickListener {
            startRemoteCapture()
        }
    }

    override fun onResume() {
        super.onResume()
        btnToggleOverlay.text = if (OverlayService.isRunning)
            getString(R.string.stop_overlay)
        else
            getString(R.string.start_overlay)
        KeyInjectionService.shizukuEnabled = isShizukuAuthorized()
        updateShizukuButtonVisibility()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_coffee) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/codevoid")))
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(this, R.string.no_browser_app, Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isCapturingKey && event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                cancelCaptureSession()
            } else {
                recordCapturedKey(event.keyCode)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setupPresetToggle() {
        val useCustom = presetManager.isCustomActive()
        togglePreset.check(if (useCustom) R.id.btnPresetCustom else R.id.btnPresetDmd)
        btnMapRemote.isEnabled = useCustom

        togglePreset.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val custom = checkedId == R.id.btnPresetCustom
                presetManager.setCustomActive(custom)
                activePreset = presetManager.getActivePreset()
                btnMapRemote.isEnabled = custom
                updateKeyAssignmentTable()
            }
        }
    }

    private fun loadSettings() {
        val size = prefs.getInt(PrefKeys.OVERLAY_SIZE, 75)
        val opacity = prefs.getInt(PrefKeys.OVERLAY_OPACITY, 80)
        sliderSize.value = size.toFloat()
        sliderOpacity.value = opacity.toFloat()
        tvSize.text = "$size%"
        tvOpacity.text = "$opacity%"
    }

    private fun updateKeyAssignmentTable() {
        remoteKeyTable.removeAllViews()
        val colorLabel = ContextCompat.getColor(this, R.color.text_secondary)
        val colorValue = ContextCompat.getColor(this, R.color.text_primary)
        for (actionIdx in RemoteKeyPreset.WIZARD_ORDER) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = dpToPx(2) }
            }
            val actionView = TextView(this).apply {
                text = RemoteKeyPreset.ACTION_NAMES[actionIdx]
                setTextColor(colorLabel)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val keyView = TextView(this).apply {
                text = keyDisplayName(activePreset.keycodes[actionIdx])
                setTextColor(colorValue)
                textSize = 13f
                gravity = Gravity.END
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(actionView)
            row.addView(keyView)
            remoteKeyTable.addView(row)
        }
    }

    private fun keyDisplayName(keycode: Int): String =
        if (keycode == KeyEvent.KEYCODE_UNKNOWN) "—"
        else KeyEvent.keyCodeToString(keycode).removePrefix("KEYCODE_")

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density + 0.5f).toInt()

    // --- Wizard ---

    private fun startRemoteCapture() {
        val template = presetManager.getCustomPreset()
        captureTargetPreset = template
        captureActionIndex = 0
        captureKeycodes = template.keycodes.copyOf()
        isCapturingKey = true

        wizardPanel.visibility = View.VISIBLE
        wizardVisible = true
        updateCaptureScreen()
    }

    private fun updateCaptureScreen() {
        val target = captureTargetPreset ?: return
        val actionIdx = RemoteKeyPreset.WIZARD_ORDER[captureActionIndex]
        wizardCapturePresetName.text = target.name
        wizardCaptureProgress.text = getString(
            R.string.wizard_action_of,
            captureActionIndex + 1,
            RemoteKeyPreset.ACTION_COUNT
        )
        wizardCaptureAction.text = RemoteKeyPreset.ACTION_NAMES[actionIdx]
        wizardCaptureCurrent.text = getString(
            R.string.wizard_current_key,
            keyDisplayName(captureKeycodes[actionIdx])
        )
    }

    private fun recordCapturedKey(keycode: Int) {
        captureKeycodes[RemoteKeyPreset.WIZARD_ORDER[captureActionIndex]] = keycode
        captureActionIndex++
        if (captureActionIndex >= RemoteKeyPreset.ACTION_COUNT) {
            finishCapture()
        } else {
            updateCaptureScreen()
        }
    }

    private fun skipCurrentAction() {
        captureActionIndex++
        if (captureActionIndex >= RemoteKeyPreset.ACTION_COUNT) {
            finishCapture()
        } else {
            updateCaptureScreen()
        }
    }

    private fun finishCapture() {
        val saved = RemoteKeyPreset("Custom", captureKeycodes.copyOf())
        presetManager.saveCustomPreset(saved)
        presetManager.setCustomActive(true)
        activePreset = saved
        isCapturingKey = false
        captureTargetPreset = null
        Toast.makeText(this, getString(R.string.wizard_mapping_saved), Toast.LENGTH_SHORT).show()
        closeWizard()
    }

    private fun cancelCaptureSession() {
        isCapturingKey = false
        captureTargetPreset = null
        closeWizard()
    }

    private fun closeWizard() {
        wizardPanel.visibility = View.GONE
        wizardVisible = false
        isCapturingKey = false
        captureTargetPreset = null
        // Sync toggle to reflect possibly-changed active preset
        val useCustom = presetManager.isCustomActive()
        togglePreset.check(if (useCustom) R.id.btnPresetCustom else R.id.btnPresetDmd)
        btnMapRemote.isEnabled = useCustom
        activePreset = presetManager.getActivePreset()
        updateKeyAssignmentTable()
    }

    // --- Shizuku helpers ---

    private fun updateShizukuButtonVisibility() {
        btnGrantShizuku.visibility = if (isShizukuAuthorized()) View.GONE else View.VISIBLE
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isShizukuAuthorized(): Boolean {
        if (!isShizukuInstalled()) return false
        return try {
            Shizuku.pingBinder() && !Shizuku.isPreV11() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    private fun onShizukuPermissionGranted() {
        KeyInjectionService.shizukuEnabled = true
        Toast.makeText(this, R.string.shizuku_grant_success, Toast.LENGTH_LONG).show()
        updateShizukuButtonVisibility()
    }

    private fun onGrantShizukuClicked() {
        if (!isShizukuInstalled()) {
            Toast.makeText(this, R.string.shizuku_not_installed, Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_LONG).show()
                return
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, R.string.shizuku_not_running, Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                onShizukuPermissionGranted()
            } else {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
        } catch (e: RuntimeException) {
            Toast.makeText(this, getString(R.string.shizuku_grant_failed, e.message ?: e.javaClass.simpleName), Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
