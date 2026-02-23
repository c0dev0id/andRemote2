package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("andremote2", MODE_PRIVATE)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
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

        setupSpinners()
        loadSettings()

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress + 50
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
            if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
                return@setOnClickListener
            }
            if (!hasInjectEventsPermission()) {
                showInjectEventsDialog()
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
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, KeyEventCodes.displayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        listOf(spJoystickUp, spJoystickDown, spJoystickLeft, spJoystickRight,
            spButtonTop, spButtonBottom, spLeverUp, spLeverDown).forEach {
            it.adapter = adapter
        }
    }

    private fun loadSettings() {
        val size = prefs.getInt("overlay_size", 100)
        val opacity = prefs.getInt("overlay_opacity", 80)
        seekBarSize.progress = size - 50
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

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "$packageName/${KeyInjectionService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedComponent, ignoreCase = true) }
    }

    private fun requestAccessibilityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isInstalledFromTrustedSource()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.restricted_settings_title)
                .setMessage(R.string.restricted_settings_message)
                .setPositiveButton(R.string.open_app_info) { _, _ ->
                    openAppInfo()
                }
                .setNeutralButton(R.string.open_accessibility_settings) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            Toast.makeText(this, R.string.enable_accessibility_service, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isInstalledFromTrustedSource(): Boolean {
        val trustedInstallers = setOf(
            "com.android.vending",
            "com.amazon.venezia",
            "com.huawei.appmarket",
            "com.sec.android.app.samsungapps"
        )
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
            installer in trustedInstallers
        } catch (_: Exception) {
            false
        }
    }

    private fun openAppInfo() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })
    }

    private fun hasInjectEventsPermission(): Boolean {
        return checkSelfPermission("android.permission.INJECT_EVENTS") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun showInjectEventsDialog() {
        val adbCommand = "adb shell pm grant $packageName android.permission.INJECT_EVENTS"
        AlertDialog.Builder(this)
            .setTitle(R.string.inject_permission_title)
            .setMessage(getString(R.string.inject_permission_message, adbCommand))
            .setPositiveButton(R.string.copy_command) { _, _ ->
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB command", adbCommand))
                Toast.makeText(this, R.string.command_copied, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
