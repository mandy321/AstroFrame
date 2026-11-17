package com.astroframe.tv.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.astroframe.tv.R
import com.astroframe.tv.data.ArtSource
import com.astroframe.tv.data.MatStyle
import com.astroframe.tv.data.OverlayPosition
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.databinding.ActivityMainBinding
import com.astroframe.tv.worker.ApodWallpaperWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // Track live app open
        com.astroframe.tv.analytics.AnalyticsTracker.trackAppOpen("main_activity")

        setupButtons()
        observeWorkManager()
        refreshUiState()

        // Focus the primary button by default on TV
        binding.btnRefreshNow.requestFocus()
    }

    private fun setupButtons() {
        // 1. Refresh Now
        binding.btnRefreshNow.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvStatus.text = "Fetching new wallpaper from ${prefs.artSource.label}..."
            binding.btnRefreshNow.isEnabled = false
            ApodWallpaperWorker.triggerImmediate(this)
        }

        // 2. Overlay Position Dialog
        binding.btnOverlayPos.setOnClickListener {
            showOverlayPositionDialog()
        }

        // 3. Collection / Art Source Dialog (NASA, The Met, Mixed)
        binding.btnArtSource.setOnClickListener {
            showArtSourceDialog()
        }

        // 4. Interval Dialog (Quick direct selection)
        binding.btnInterval.setOnClickListener {
            showIntervalDialog()
        }

        // 5. Photo Mode Toggle (Random Archive vs Latest)
        binding.btnMode.setOnClickListener {
            prefs.useRandom = !prefs.useRandom
            updateModeButtonText()
            val modeName = if (prefs.useRandom) "Random Archive" else "Latest Daily"
            Toast.makeText(this, "Mode set to $modeName", Toast.LENGTH_SHORT).show()
        }

        // Museum Frame Matting Style Dialog
        binding.btnMatStyle.setOnClickListener {
            showMatStyleDialog()
        }

        // OLED Care Toggle
        binding.btnOledCare.setOnClickListener {
            prefs.oledCareEnabled = !prefs.oledCareEnabled
            updateOledCareButtonText()
            val state = if (prefs.oledCareEnabled) "Enabled (Anti-Burn-In Pixel Shift Active)" else "Disabled"
            Toast.makeText(this, "OLED Care " + state, Toast.LENGTH_SHORT).show()
        }

        // 6. NASA API Key Dialog
        binding.btnApiKey.setOnClickListener {
            showApiKeyDialog()
        }

        // 7. Screensaver Rotation Dialog
        binding.btnScreensaverRotation.setOnClickListener {
            showScreensaverRotationDialog()
        }

        // 8. Open TV Screensaver Settings Shortcut
        binding.btnScreensaverSettings.setOnClickListener {
            openSystemScreensaverSettings()
        }

        // 9. Setup Guide Dialog
        binding.btnSetupGuide.setOnClickListener {
            showProjectivyGuideDialog()
        }
    }

    private fun observeWorkManager() {
        WorkManager.getInstance(this)
            .getWorkInfosByTagLiveData("IMMEDIATE")
            .observe(this) { workInfos ->
                if (workInfos.isNullOrEmpty()) return@observe

                val latestInfo = workInfos.last()
                when (latestInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnRefreshNow.isEnabled = false
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRefreshNow.isEnabled = true
                        refreshUiState()
                        Toast.makeText(this, R.string.update_success, Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.FAILED -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRefreshNow.isEnabled = true
                        binding.tvStatus.text = "Status: Failed to fetch image. Check internet connection."
                        Toast.makeText(this, "Failed to download wallpaper", Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
    }

    private fun refreshUiState() {
        updateOverlayButtonText()
        updateMatStyleButtonText()
        updateOledCareButtonText()
        updateArtSourceButtonText()
        updateIntervalButtonText()
        updateModeButtonText()
        updateScreensaverButtonText()

        val lastPath = prefs.lastImagePath
        if (lastPath.isNotBlank()) {
            val imgFile = File(lastPath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                if (bitmap != null) {
                    binding.imgPreview.setImageBitmap(bitmap)
                    binding.tvEmptyState.visibility = View.GONE
                }
            }
        }

        if (prefs.lastTitle.isNotBlank()) {
            binding.tvCurrentTitle.text = prefs.lastTitle
            binding.tvCurrentDateCredit.text = "${prefs.lastDate} • ${prefs.artSource.badge}"
            binding.tvCurrentExplanation.text = prefs.lastExplanation

            val dateStr = if (prefs.lastUpdatedMs > 0) {
                SimpleDateFormat("HH:mm, MMM d", Locale.getDefault()).format(Date(prefs.lastUpdatedMs))
            } else "N/A"
            binding.tvStatus.text = "Last updated: $dateStr"
        }
    }

    private fun updateOverlayButtonText() {
        binding.btnOverlayPos.text = "Overlay: ${prefs.overlayPosition.label}"
    }

    private fun updateArtSourceButtonText() {
        binding.btnArtSource.text = "Collection: ${prefs.artSource.label}"
    }

    private fun updateIntervalButtonText() {
        binding.btnInterval.text = "Interval: ${formatMinutes(prefs.intervalMinutes)}"
    }

    private fun updateModeButtonText() {
        val label = if (prefs.useRandom) "Random Archive" else "Latest Daily"
        binding.btnMode.text = "Mode: $label"
    }

    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes Mins"
            minutes == 60 -> "1 Hour"
            minutes == 1440 -> "24 Hours (Daily)"
            else -> "${minutes / 60} Hours"
        }
    }

    private fun showIntervalDialog() {
        val options = PreferencesManager.INTERVAL_OPTIONS
        val labels = options.map { formatMinutes(it) }.toTypedArray()
        val currentIndex = options.indexOf(prefs.intervalMinutes).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Wallpaper Update Interval")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedMinutes = options[which]
                prefs.intervalMinutes = selectedMinutes
                ApodWallpaperWorker.schedulePeriodic(this, selectedMinutes, forceReplace = true)
                updateIntervalButtonText()
                Toast.makeText(this, "Interval set to ${labels[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showArtSourceDialog() {
        val sources = ArtSource.entries.toTypedArray()
        val labels = arrayOf(
            "🚀 NASA Cosmos (Astronomy Picture of the Day)",
            "🎨 The Met Museum (Famous Masterpieces & Fine Art)",
            "🌌 Cosmos + Museum Mix (Random Shuffle)"
        )
        val currentIndex = sources.indexOf(prefs.artSource).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Artwork Collection")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedSource = sources[which]
                prefs.artSource = selectedSource
                updateArtSourceButtonText()
                Toast.makeText(this, "Collection set to ${selectedSource.label}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showOverlayPositionDialog() {
        val positions = OverlayPosition.entries.toTypedArray()
        val labels = positions.map { it.label }.toTypedArray()
        val currentIndex = positions.indexOf(prefs.overlayPosition).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Overlay Position on TV")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedPos = positions[which]
                prefs.overlayPosition = selectedPos
                updateOverlayButtonText()
                Toast.makeText(this, "Overlay set to ${selectedPos.label}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProjectivyGuideDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_setup_guide, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCloseGuide).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showApiKeyDialog() {
        val input = android.widget.EditText(this).apply {
            setText(prefs.apiKey)
            hint = "Enter NASA API key (or leave blank)"
            setSingleLine(true)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("NASA APOD API Key")
            .setMessage("The app works out of the box without any key! If you have your own personal key from api.nasa.gov, you can enter it below:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newKey = input.text.toString().trim()
                prefs.apiKey = newKey
                Toast.makeText(this, "API Key saved", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Reset Default") { _, _ ->
                prefs.apiKey = com.astroframe.tv.BuildConfig.NASA_API_KEY
                Toast.makeText(this, "Reset to default configuration", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateMatStyleButtonText() {
        binding.btnMatStyle.text = "Mat Frame: " + prefs.matStyle.label
    }

    private fun updateOledCareButtonText() {
        val label = if (prefs.oledCareEnabled) "OLED Care: Active 🛡️" else "OLED Care: Off"
        binding.btnOledCare.text = label
    }

    private fun showMatStyleDialog() {
        val styles = MatStyle.entries.toTypedArray()
        val labels = styles.map { it.label }.toTypedArray()
        val currentIndex = styles.indexOf(prefs.matStyle).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Architectural Mat Framing (The Frame Mode)")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedStyle = styles[which]
                prefs.matStyle = selectedStyle
                updateMatStyleButtonText()
                Toast.makeText(this, "Mat style: " + selectedStyle.label, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateScreensaverButtonText() {
        val sec = prefs.screensaverRotationSeconds
        val label = if (sec == 0) "None (Static)" else formatSeconds(sec)
        binding.btnScreensaverRotation.text = "Photo Rotation: $label"
    }

    private fun formatSeconds(sec: Int): String {
        return when {
            sec < 60 -> "$sec Sec"
            sec == 60 -> "1 Min"
            else -> "${sec / 60} Mins"
        }
    }

    private fun showScreensaverRotationDialog() {
        val options = PreferencesManager.SCREENSAVER_ROTATION_OPTIONS
        val labels = options.map {
            if (it == 0) "None (Static - Hold Current Artwork)" else formatSeconds(it)
        }.toTypedArray()
        val currentIndex = options.indexOf(prefs.screensaverRotationSeconds).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Screensaver Photo Rotation Speed")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedSec = options[which]
                prefs.screensaverRotationSeconds = selectedSec
                updateScreensaverButtonText()
                val text = if (selectedSec == 0) "Screensaver will hold the current wallpaper" else "Screensaver will cycle every ${labels[which]}"
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openSystemScreensaverSettings() {
        try {
            val intent = android.content.Intent("android.settings.DREAM_SETTINGS").apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Open TV Settings > Device Preferences > Screensaver", Toast.LENGTH_LONG).show()
            }
        }
    }
}
