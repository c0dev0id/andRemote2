package de.codevoid.andremote2

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var btnToggleOverlay: MaterialButton
    private lateinit var sliderSize: Slider
    private lateinit var sliderOpacity: Slider
    private lateinit var tvSize: TextView
    private lateinit var tvOpacity: TextView
    private lateinit var btnCheckUpdates: MaterialButton

    private var activeDownloadId: Long = -1
    private var downloadCompleteReceiver: BroadcastReceiver? = null

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
        btnCheckUpdates = findViewById(R.id.btnCheckUpdates)

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

        btnCheckUpdates.setOnClickListener {
            checkForUpdates()
        }
    }

    override fun onResume() {
        super.onResume()
        btnToggleOverlay.text = if (OverlayService.isRunning)
            getString(R.string.stop_overlay)
        else
            getString(R.string.start_overlay)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadCompleteReceiver?.let { unregisterReceiver(it) }
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

    private fun checkForUpdates() {
        btnCheckUpdates.isEnabled = false
        btnCheckUpdates.text = getString(R.string.checking_for_updates)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { UpdateChecker.checkForUpdate() }
            }

            btnCheckUpdates.isEnabled = true
            btnCheckUpdates.text = getString(R.string.check_for_updates)

            result.fold(
                onSuccess = { release ->
                    if (release == null) {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.check_for_updates)
                            .setMessage(R.string.up_to_date)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    } else {
                        showUpdateDialog(release)
                    }
                },
                onFailure = {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.update_check_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun showUpdateDialog(release: ReleaseInfo) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_available, release.tagName))
            .setMessage(getString(R.string.update_dialog_message, release.tagName))
            .setPositiveButton(R.string.action_update) { _, _ ->
                if (packageManager.canRequestPackageInstalls()) {
                    downloadAndInstall(release)
                } else {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:$packageName")
                        )
                    )
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.allow_installs_prompt,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun downloadAndInstall(release: ReleaseInfo) {
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            isIndeterminate = true
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.downloading_update)
            .setView(progressBar)
            .setCancelable(false)
            .create()
        progressDialog.show()

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(release.downloadUrl))
            .setTitle(release.fileName)
            .setDescription(getString(R.string.downloading_update))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, release.fileName)

        activeDownloadId = downloadManager.enqueue(request)

        // Register receiver for download completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == activeDownloadId) {
                    unregisterReceiver(this)
                    downloadCompleteReceiver = null
                    progressDialog.dismiss()

                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusCol >= 0 && cursor.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                            val apkUri = downloadManager.getUriForDownloadedFile(id)
                            openPackageInstaller(apkUri)
                        } else {
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                R.string.update_download_failed,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                    cursor.close()
                }
            }
        }
        downloadCompleteReceiver = receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        // Poll progress
        lifecycleScope.launch {
            while (progressDialog.isShowing) {
                val query = DownloadManager.Query().setFilterById(activeDownloadId)
                val cursor = withContext(Dispatchers.IO) { downloadManager.query(query) }
                if (cursor.moveToFirst()) {
                    val downloadedCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    if (downloadedCol >= 0 && totalCol >= 0) {
                        val downloaded = cursor.getLong(downloadedCol)
                        val total = cursor.getLong(totalCol)
                        if (total > 0) {
                            if (progressBar.isIndeterminate) progressBar.isIndeterminate = false
                            progressBar.setProgress(((downloaded * 100) / total).toInt(), false)
                        }
                    }
                }
                cursor.close()
                delay(200)
            }
        }
    }

    private fun openPackageInstaller(apkUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            Snackbar.make(
                findViewById(android.R.id.content),
                R.string.update_download_failed,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}
