package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 100
        const val PREFS_NAME = "andremote2"
        private const val PRESET_DMD_REMOTE_2 = 1
        const val DEFAULT_JOYSTICK_UP = 19
        const val DEFAULT_JOYSTICK_DOWN = 20
        const val DEFAULT_JOYSTICK_LEFT = 21
        const val DEFAULT_JOYSTICK_RIGHT = 22
        const val DEFAULT_BUTTON_TOP = 66
        const val DEFAULT_BUTTON_BOTTOM = 111
        const val DEFAULT_LEVER_UP = 136
        const val DEFAULT_LEVER_DOWN = 137
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var btnToggleOverlay: MaterialButton
    private lateinit var btnGrantShizuku: MaterialButton
    private lateinit var sliderSize: Slider
    private lateinit var sliderOpacity: Slider
    private lateinit var tvSize: TextView
    private lateinit var tvOpacity: TextView
    private lateinit var tvEventLog: TextView
    private lateinit var scrollEventLog: ScrollView
    private lateinit var switchButtonHoldMode: MaterialSwitch

    private lateinit var actvJoystickUp: MaterialAutoCompleteTextView
    private lateinit var actvJoystickDown: MaterialAutoCompleteTextView
    private lateinit var actvJoystickLeft: MaterialAutoCompleteTextView
    private lateinit var actvJoystickRight: MaterialAutoCompleteTextView
    private lateinit var actvButtonTop: MaterialAutoCompleteTextView
    private lateinit var actvButtonBottom: MaterialAutoCompleteTextView
    private lateinit var actvLeverUp: MaterialAutoCompleteTextView
    private lateinit var actvLeverDown: MaterialAutoCompleteTextView
    private lateinit var actvPreset: MaterialAutoCompleteTextView

    private val keyActvs: List<MaterialAutoCompleteTextView> by lazy {
        listOf(actvJoystickUp, actvJoystickDown, actvJoystickLeft, actvJoystickRight,
            actvButtonTop, actvButtonBottom, actvLeverUp, actvLeverDown)
    }
    private lateinit var setKeyButtons: List<MaterialButton>

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

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        btnGrantShizuku = findViewById(R.id.btnGrantShizuku)
        sliderSize = findViewById(R.id.sliderSize)
        sliderOpacity = findViewById(R.id.sliderOpacity)
        tvSize = findViewById(R.id.tvSize)
        tvOpacity = findViewById(R.id.tvOpacity)
        tvEventLog = findViewById(R.id.tvEventLog)
        scrollEventLog = findViewById(R.id.scrollEventLog)
        switchButtonHoldMode = findViewById(R.id.switchButtonHoldMode)
        actvPreset = findViewById(R.id.actvPreset)

        // Bind key-mapping rows via include IDs
        val rowJoystickUp = findViewById<android.view.View>(R.id.rowJoystickUp)
        val rowJoystickDown = findViewById<android.view.View>(R.id.rowJoystickDown)
        val rowJoystickLeft = findViewById<android.view.View>(R.id.rowJoystickLeft)
        val rowJoystickRight = findViewById<android.view.View>(R.id.rowJoystickRight)
        val rowButtonTop = findViewById<android.view.View>(R.id.rowButtonTop)
        val rowButtonBottom = findViewById<android.view.View>(R.id.rowButtonBottom)
        val rowLeverUp = findViewById<android.view.View>(R.id.rowLeverUp)
        val rowLeverDown = findViewById<android.view.View>(R.id.rowLeverDown)

        actvJoystickUp = rowJoystickUp.findViewById(R.id.actvKey)
        actvJoystickDown = rowJoystickDown.findViewById(R.id.actvKey)
        actvJoystickLeft = rowJoystickLeft.findViewById(R.id.actvKey)
        actvJoystickRight = rowJoystickRight.findViewById(R.id.actvKey)
        actvButtonTop = rowButtonTop.findViewById(R.id.actvKey)
        actvButtonBottom = rowButtonBottom.findViewById(R.id.actvKey)
        actvLeverUp = rowLeverUp.findViewById(R.id.actvKey)
        actvLeverDown = rowLeverDown.findViewById(R.id.actvKey)

        // Set row labels
        rowJoystickUp.findViewById<TextView>(R.id.tvLabel).setText(R.string.joystick_up)
        rowJoystickDown.findViewById<TextView>(R.id.tvLabel).setText(R.string.joystick_down)
        rowJoystickLeft.findViewById<TextView>(R.id.tvLabel).setText(R.string.joystick_left)
        rowJoystickRight.findViewById<TextView>(R.id.tvLabel).setText(R.string.joystick_right)
        rowButtonTop.findViewById<TextView>(R.id.tvLabel).setText(R.string.button_top)
        rowButtonBottom.findViewById<TextView>(R.id.tvLabel).setText(R.string.button_bottom)
        rowLeverUp.findViewById<TextView>(R.id.tvLabel).setText(R.string.lever_up)
        rowLeverDown.findViewById<TextView>(R.id.tvLabel).setText(R.string.lever_down)

        setKeyButtons = listOf(
            rowJoystickUp.findViewById(R.id.btnSetKey),
            rowJoystickDown.findViewById(R.id.btnSetKey),
            rowJoystickLeft.findViewById(R.id.btnSetKey),
            rowJoystickRight.findViewById(R.id.btnSetKey),
            rowButtonTop.findViewById(R.id.btnSetKey),
            rowButtonBottom.findViewById(R.id.btnSetKey),
            rowLeverUp.findViewById(R.id.btnSetKey),
            rowLeverDown.findViewById(R.id.btnSetKey)
        )

        setupPresetDropdown()
        setupKeyDropdowns()
        loadSettings()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        // Set Key buttons
        setKeyButtons.zip(keyActvs).forEach { (btn, actv) ->
            btn.setOnClickListener { showSetKeyDialog(actv) }
        }

        sliderSize.addOnChangeListener { _, value, fromUser ->
            val size = value.toInt()
            tvSize.text = "$size%"
            if (fromUser) saveIntPref("overlay_size", size)
        }

        sliderOpacity.addOnChangeListener { _, value, fromUser ->
            val opacity = value.toInt()
            tvOpacity.text = "$opacity%"
            if (fromUser) saveIntPref("overlay_opacity", opacity)
        }

        switchButtonHoldMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("button_hold_mode", isChecked).apply()
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
            saveKeyMappings()
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

        findViewById<MaterialButton>(R.id.btnSaveMappings).setOnClickListener {
            saveKeyMappings()
            Toast.makeText(this, R.string.mappings_saved, Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnClearLog).setOnClickListener {
            KeyEventLog.clear()
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
        KeyEventLog.setOnNewEntryListener { updateEventLogView() }
        updateEventLogView()
    }

    override fun onPause() {
        super.onPause()
        KeyEventLog.setOnNewEntryListener(null)
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
            KeyEvent.ACTION_UP -> "ACTION_UP"
            KeyEvent.ACTION_MULTIPLE -> "ACTION_MULTIPLE"
            else -> event.action.toString()
        }
        KeyEventLog.log("Received", "$action keyCode=${event.keyCode} flags=0x${event.flags.toString(16)} repeat=${event.repeatCount}")
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun updateShizukuButtonVisibility() {
        if (isShizukuAuthorized()) {
            btnGrantShizuku.visibility = android.view.View.GONE
        } else {
            btnGrantShizuku.visibility = android.view.View.VISIBLE
        }
    }

    private fun updateEventLogView() {
        val text = KeyEventLog.getEntries().joinToString("\n")
        tvEventLog.text = text
        scrollEventLog.post { scrollEventLog.fullScroll(ScrollView.FOCUS_DOWN) }
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
            // Shizuku pre-v11 not supported
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

    private fun setupKeyDropdowns() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, KeyEventCodes.displayNames)
        keyActvs.forEach { it.setAdapter(adapter) }
    }

    private fun setupPresetDropdown() {
        val presets = listOf(getString(R.string.preset_custom), getString(R.string.preset_dmd_remote_2))
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, presets)
        actvPreset.setAdapter(adapter)
        val savedPreset = prefs.getInt("preset", 0)
        actvPreset.setText(presets[savedPreset], false)
        actvPreset.setOnItemClickListener { _, _, position, _ ->
            val isDmdPreset = position == PRESET_DMD_REMOTE_2
            val currentPreset = prefs.getInt("preset", 0)
            if (isDmdPreset && currentPreset != PRESET_DMD_REMOTE_2) {
                // Preserve custom mapping before switching away from Custom preset
                saveCustomPresetMappings()
            }
            saveIntPref("preset", position)
            keyActvs.forEach { it.isEnabled = !isDmdPreset }
            setKeyButtons.forEach { it.isEnabled = !isDmdPreset }
            if (isDmdPreset) {
                populateKeyMappingUi(true)
                // Update keycode_* so OverlayService reads DMD defaults if running
                saveKeyMappings()
            } else {
                // Restore custom mapping when switching back to Custom
                populateKeyMappingUi(false)
                saveKeyMappings()
            }
        }
        // Apply initial enabled state based on saved preset
        val isDmdPreset = savedPreset == PRESET_DMD_REMOTE_2
        keyActvs.forEach { it.isEnabled = !isDmdPreset }
        setKeyButtons.forEach { it.isEnabled = !isDmdPreset }
    }

    private fun showSetKeyDialog(actv: MaterialAutoCompleteTextView) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(R.string.press_key_to_configure)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val idx = KeyEventCodes.entries.indexOfFirst { it.code == keyCode }
                if (idx >= 0) {
                    actv.setText(KeyEventCodes.displayNames[idx], false)
                    dialog.dismiss()
                    return@setOnKeyListener true
                }
            }
            false
        }
        dialog.show()
    }

    private fun loadSettings() {
        val size = prefs.getInt("overlay_size", 75)
        val opacity = prefs.getInt("overlay_opacity", 80)
        sliderSize.value = size.toFloat()
        sliderOpacity.value = opacity.toFloat()
        tvSize.text = "$size%"
        tvOpacity.text = "$opacity%"

        switchButtonHoldMode.isChecked = prefs.getBoolean("button_hold_mode", false)

        val isDmdPreset = prefs.getInt("preset", 0) == PRESET_DMD_REMOTE_2
        populateKeyMappingUi(isDmdPreset)
    }

    private fun populateKeyMappingUi(isDmdPreset: Boolean) {
        fun keyName(code: Int) = KeyEventCodes.displayNames[KeyEventCodes.indexOfCode(code)]
        fun resolveCode(key: String, default: Int) =
            if (isDmdPreset) default else prefs.getInt(key, default)
        actvJoystickUp.setText(keyName(resolveCode("custom_keycode_joystick_up", DEFAULT_JOYSTICK_UP)), false)
        actvJoystickDown.setText(keyName(resolveCode("custom_keycode_joystick_down", DEFAULT_JOYSTICK_DOWN)), false)
        actvJoystickLeft.setText(keyName(resolveCode("custom_keycode_joystick_left", DEFAULT_JOYSTICK_LEFT)), false)
        actvJoystickRight.setText(keyName(resolveCode("custom_keycode_joystick_right", DEFAULT_JOYSTICK_RIGHT)), false)
        actvButtonTop.setText(keyName(resolveCode("custom_keycode_button_top", DEFAULT_BUTTON_TOP)), false)
        actvButtonBottom.setText(keyName(resolveCode("custom_keycode_button_bottom", DEFAULT_BUTTON_BOTTOM)), false)
        actvLeverUp.setText(keyName(resolveCode("custom_keycode_lever_up", DEFAULT_LEVER_UP)), false)
        actvLeverDown.setText(keyName(resolveCode("custom_keycode_lever_down", DEFAULT_LEVER_DOWN)), false)
    }

    private fun saveKeyMappings() {
        prefs.edit()
            .putInt("keycode_joystick_up", actvToKeyCode(actvJoystickUp))
            .putInt("keycode_joystick_down", actvToKeyCode(actvJoystickDown))
            .putInt("keycode_joystick_left", actvToKeyCode(actvJoystickLeft))
            .putInt("keycode_joystick_right", actvToKeyCode(actvJoystickRight))
            .putInt("keycode_button_top", actvToKeyCode(actvButtonTop))
            .putInt("keycode_button_bottom", actvToKeyCode(actvButtonBottom))
            .putInt("keycode_lever_up", actvToKeyCode(actvLeverUp))
            .putInt("keycode_lever_down", actvToKeyCode(actvLeverDown))
            .apply()
        if (prefs.getInt("preset", 0) != PRESET_DMD_REMOTE_2) {
            saveCustomPresetMappings()
        }
    }

    private fun saveCustomPresetMappings() {
        prefs.edit()
            .putInt("custom_keycode_joystick_up", actvToKeyCode(actvJoystickUp))
            .putInt("custom_keycode_joystick_down", actvToKeyCode(actvJoystickDown))
            .putInt("custom_keycode_joystick_left", actvToKeyCode(actvJoystickLeft))
            .putInt("custom_keycode_joystick_right", actvToKeyCode(actvJoystickRight))
            .putInt("custom_keycode_button_top", actvToKeyCode(actvButtonTop))
            .putInt("custom_keycode_button_bottom", actvToKeyCode(actvButtonBottom))
            .putInt("custom_keycode_lever_up", actvToKeyCode(actvLeverUp))
            .putInt("custom_keycode_lever_down", actvToKeyCode(actvLeverDown))
            .apply()
    }

    private fun actvToKeyCode(actv: MaterialAutoCompleteTextView): Int {
        val idx = KeyEventCodes.displayNames.indexOf(actv.text.toString())
        return if (idx >= 0) KeyEventCodes.codeAtIndex(idx) else KeyEvent.KEYCODE_UNKNOWN
    }

    private fun saveIntPref(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
