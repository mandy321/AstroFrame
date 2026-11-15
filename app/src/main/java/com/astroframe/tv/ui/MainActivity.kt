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

        // 6. Setup Guide Dialog
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
        updateArtSourceButtonText()
        updateIntervalButtonText()
        updateModeButtonText()

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
}
