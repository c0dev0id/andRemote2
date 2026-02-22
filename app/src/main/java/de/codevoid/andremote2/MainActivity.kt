package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
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

    private lateinit var etJoystickUp: EditText
    private lateinit var etJoystickDown: EditText
    private lateinit var etJoystickLeft: EditText
    private lateinit var etJoystickRight: EditText
    private lateinit var etButtonTop: EditText
    private lateinit var etButtonBottom: EditText
    private lateinit var etLeverUp: EditText
    private lateinit var etLeverDown: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("andremote2", MODE_PRIVATE)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        seekBarSize = findViewById(R.id.seekBarSize)
        seekBarOpacity = findViewById(R.id.seekBarOpacity)
        tvSize = findViewById(R.id.tvSize)
        tvOpacity = findViewById(R.id.tvOpacity)

        etJoystickUp = findViewById(R.id.etJoystickUp)
        etJoystickDown = findViewById(R.id.etJoystickDown)
        etJoystickLeft = findViewById(R.id.etJoystickLeft)
        etJoystickRight = findViewById(R.id.etJoystickRight)
        etButtonTop = findViewById(R.id.etButtonTop)
        etButtonBottom = findViewById(R.id.etButtonBottom)
        etLeverUp = findViewById(R.id.etLeverUp)
        etLeverDown = findViewById(R.id.etLeverDown)

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
    }

    override fun onResume() {
        super.onResume()
        btnToggleOverlay.text = if (OverlayService.isRunning)
            getString(R.string.stop_overlay)
        else
            getString(R.string.start_overlay)
    }

    private fun loadSettings() {
        val size = prefs.getInt("overlay_size", 100)
        val opacity = prefs.getInt("overlay_opacity", 80)
        seekBarSize.progress = size - 50
        seekBarOpacity.progress = opacity - 20
        tvSize.text = "$size%"
        tvOpacity.text = "$opacity%"

        etJoystickUp.setText(prefs.getInt("keycode_joystick_up", DEFAULT_JOYSTICK_UP).toString())
        etJoystickDown.setText(prefs.getInt("keycode_joystick_down", DEFAULT_JOYSTICK_DOWN).toString())
        etJoystickLeft.setText(prefs.getInt("keycode_joystick_left", DEFAULT_JOYSTICK_LEFT).toString())
        etJoystickRight.setText(prefs.getInt("keycode_joystick_right", DEFAULT_JOYSTICK_RIGHT).toString())
        etButtonTop.setText(prefs.getInt("keycode_button_top", DEFAULT_BUTTON_TOP).toString())
        etButtonBottom.setText(prefs.getInt("keycode_button_bottom", DEFAULT_BUTTON_BOTTOM).toString())
        etLeverUp.setText(prefs.getInt("keycode_lever_up", DEFAULT_LEVER_UP).toString())
        etLeverDown.setText(prefs.getInt("keycode_lever_down", DEFAULT_LEVER_DOWN).toString())
    }

    private fun saveKeyMappings() {
        saveIntPref("keycode_joystick_up", etJoystickUp.text.toString().toIntOrNull() ?: DEFAULT_JOYSTICK_UP)
        saveIntPref("keycode_joystick_down", etJoystickDown.text.toString().toIntOrNull() ?: DEFAULT_JOYSTICK_DOWN)
        saveIntPref("keycode_joystick_left", etJoystickLeft.text.toString().toIntOrNull() ?: DEFAULT_JOYSTICK_LEFT)
        saveIntPref("keycode_joystick_right", etJoystickRight.text.toString().toIntOrNull() ?: DEFAULT_JOYSTICK_RIGHT)
        saveIntPref("keycode_button_top", etButtonTop.text.toString().toIntOrNull() ?: DEFAULT_BUTTON_TOP)
        saveIntPref("keycode_button_bottom", etButtonBottom.text.toString().toIntOrNull() ?: DEFAULT_BUTTON_BOTTOM)
        saveIntPref("keycode_lever_up", etLeverUp.text.toString().toIntOrNull() ?: DEFAULT_LEVER_UP)
        saveIntPref("keycode_lever_down", etLeverDown.text.toString().toIntOrNull() ?: DEFAULT_LEVER_DOWN)
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
        Toast.makeText(this, R.string.enable_accessibility_service, Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
