package de.codevoid.andremote2

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var btnToggleOverlay: MaterialButton
    private lateinit var sliderSize: Slider
    private lateinit var sliderOpacity: Slider
    private lateinit var tvSize: TextView
    private lateinit var tvOpacity: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("andremote2", MODE_PRIVATE)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        sliderSize = findViewById(R.id.sliderSize)
        sliderOpacity = findViewById(R.id.sliderOpacity)
        tvSize = findViewById(R.id.tvSize)
        tvOpacity = findViewById(R.id.tvOpacity)

        loadSettings()

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
            if (OverlayService.isRunning) {
                stopService(Intent(this, OverlayService::class.java))
                btnToggleOverlay.text = getString(R.string.start_overlay)
            } else {
                startForegroundService(Intent(this, OverlayService::class.java))
                btnToggleOverlay.text = getString(R.string.stop_overlay)
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

    private fun loadSettings() {
        val size = prefs.getInt(PrefKeys.OVERLAY_SIZE, 75)
        val opacity = prefs.getInt(PrefKeys.OVERLAY_OPACITY, 80)
        sliderSize.value = size.toFloat()
        sliderOpacity.value = opacity.toFloat()
        tvSize.text = "$size%"
        tvOpacity.text = "$opacity%"
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
