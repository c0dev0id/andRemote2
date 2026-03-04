package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 100
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
    private lateinit var btnToggleOverlay: Button
    private lateinit var btnGrantShizuku: Button
    private lateinit var seekBarSize: SeekBar
    private lateinit var seekBarOpacity: SeekBar
    private lateinit var tvSize: TextView
    private lateinit var tvOpacity: TextView

    private lateinit var spJoystickUp: Spinner
    private lateinit var spJoystickDown: Spinner
    private lateinit var spJoystickLeft: Spinner
    private lateinit var spJoystickRight: Spinner
    private lateinit var spButtonTop: Spinner
    private lateinit var spButtonBottom: Spinner
    private lateinit var spLeverUp: Spinner
    private lateinit var spLeverDown: Spinner
    private lateinit var spPreset: Spinner

    private val keySpinners: List<Spinner> by lazy {
        listOf(spJoystickUp, spJoystickDown, spJoystickLeft, spJoystickRight,
            spButtonTop, spButtonBottom, spLeverUp, spLeverDown)
    }
    private val setKeyButtonIds = listOf(
        R.id.btnSetJoystickUp, R.id.btnSetJoystickDown,
        R.id.btnSetJoystickLeft, R.id.btnSetJoystickRight,
        R.id.btnSetButtonTop, R.id.btnSetButtonBottom,
        R.id.btnSetLeverUp, R.id.btnSetLeverDown
    )

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

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        btnGrantShizuku = findViewById(R.id.btnGrantShizuku)
        seekBarSize = findViewById(R.id.seekBarSize)
        seekBarOpacity = findViewById(R.id.seekBarOpacity)
        tvSize = findViewById(R.id.tvSize)
        tvOpacity = findViewById(R.id.tvOpacity)

        spJoystickUp = findViewById(R.id.spJoystickUp)
        spJoystickDown = findViewById(R.id.spJoystickDown)
        spJoystickLeft = findViewById(R.id.spJoystickLeft)
        spJoystickRight = findViewById(R.id.spJoystickRight)
        spButtonTop = findViewById(R.id.spButtonTop)
        spButtonBottom = findViewById(R.id.spButtonBottom)
        spLeverUp = findViewById(R.id.spLeverUp)
        spLeverDown = findViewById(R.id.spLeverDown)
        spPreset = findViewById(R.id.spPreset)

        setupPresetSpinner()
        setupSpinners()
        loadSettings()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)

        // Set Key buttons
        val setKeyMappings = setKeyButtonIds.zip(keySpinners)
        for ((btnId, spinner) in setKeyMappings) {
            findViewById<Button>(btnId).setOnClickListener { showSetKeyDialog(spinner) }
        }

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 25
                tvSize.text = "$size%"
                if (fromUser) saveIntPref("overlay_size", size)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = progress + 20
                tvOpacity.text = "$opacity%"
                if (fromUser) saveIntPref("overlay_opacity", opacity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

        findViewById<Button>(R.id.btnSaveMappings).setOnClickListener {
            saveKeyMappings()
            Toast.makeText(this, R.string.mappings_saved, Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnCoffee).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/codevoid")))
            } catch (_: android.content.ActivityNotFoundException) {
                Toast.makeText(this, "No browser app found", Toast.LENGTH_SHORT).show()
            }
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

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, KeyEventCodes.displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val paint = android.graphics.Paint()
        paint.textSize = resources.displayMetrics.scaledDensity * 14f
        val widestPx = KeyEventCodes.displayNames.maxOfOrNull { paint.measureText(it) }?.toInt() ?: 0
        val dropDownWidth = widestPx + (32 * resources.displayMetrics.density).toInt()

        keySpinners.forEach {
            it.adapter = adapter
            it.setDropDownWidth(dropDownWidth)
        }
    }

    private fun setupPresetSpinner() {
        val presets = listOf(getString(R.string.preset_custom), getString(R.string.preset_dmd_remote_2))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPreset.adapter = adapter
        spPreset.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val isDmdPreset = position == PRESET_DMD_REMOTE_2
                keySpinners.forEach { it.isEnabled = !isDmdPreset }
                setKeyButtonIds.forEach { findViewById<Button>(it).isEnabled = !isDmdPreset }
                if (isDmdPreset) {
                    spJoystickUp.setSelection(KeyEventCodes.indexOfCode(DEFAULT_JOYSTICK_UP))
                    spJoystickDown.setSelection(KeyEventCodes.indexOfCode(DEFAULT_JOYSTICK_DOWN))
                    spJoystickLeft.setSelection(KeyEventCodes.indexOfCode(DEFAULT_JOYSTICK_LEFT))
                    spJoystickRight.setSelection(KeyEventCodes.indexOfCode(DEFAULT_JOYSTICK_RIGHT))
                    spButtonTop.setSelection(KeyEventCodes.indexOfCode(DEFAULT_BUTTON_TOP))
                    spButtonBottom.setSelection(KeyEventCodes.indexOfCode(DEFAULT_BUTTON_BOTTOM))
                    spLeverUp.setSelection(KeyEventCodes.indexOfCode(DEFAULT_LEVER_UP))
                    spLeverDown.setSelection(KeyEventCodes.indexOfCode(DEFAULT_LEVER_DOWN))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showSetKeyDialog(spinner: Spinner) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(R.string.press_key_to_configure)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val idx = KeyEventCodes.entries.indexOfFirst { it.code == keyCode }
                if (idx >= 0) {
                    spinner.setSelection(idx)
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
        seekBarSize.progress = size - 25
        seekBarOpacity.progress = opacity - 20
        tvSize.text = "$size%"
        tvOpacity.text = "$opacity%"

        spJoystickUp.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_joystick_up", DEFAULT_JOYSTICK_UP)))
        spJoystickDown.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_joystick_down", DEFAULT_JOYSTICK_DOWN)))
        spJoystickLeft.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_joystick_left", DEFAULT_JOYSTICK_LEFT)))
        spJoystickRight.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_joystick_right", DEFAULT_JOYSTICK_RIGHT)))
        spButtonTop.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_button_top", DEFAULT_BUTTON_TOP)))
        spButtonBottom.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_button_bottom", DEFAULT_BUTTON_BOTTOM)))
        spLeverUp.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_lever_up", DEFAULT_LEVER_UP)))
        spLeverDown.setSelection(KeyEventCodes.indexOfCode(prefs.getInt("keycode_lever_down", DEFAULT_LEVER_DOWN)))
    }

    private fun saveKeyMappings() {
        saveIntPref("keycode_joystick_up", KeyEventCodes.codeAtIndex(spJoystickUp.selectedItemPosition))
        saveIntPref("keycode_joystick_down", KeyEventCodes.codeAtIndex(spJoystickDown.selectedItemPosition))
        saveIntPref("keycode_joystick_left", KeyEventCodes.codeAtIndex(spJoystickLeft.selectedItemPosition))
        saveIntPref("keycode_joystick_right", KeyEventCodes.codeAtIndex(spJoystickRight.selectedItemPosition))
        saveIntPref("keycode_button_top", KeyEventCodes.codeAtIndex(spButtonTop.selectedItemPosition))
        saveIntPref("keycode_button_bottom", KeyEventCodes.codeAtIndex(spButtonBottom.selectedItemPosition))
        saveIntPref("keycode_lever_up", KeyEventCodes.codeAtIndex(spLeverUp.selectedItemPosition))
        saveIntPref("keycode_lever_down", KeyEventCodes.codeAtIndex(spLeverDown.selectedItemPosition))
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
