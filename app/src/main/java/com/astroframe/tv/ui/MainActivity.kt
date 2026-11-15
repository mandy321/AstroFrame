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
            binding.tvStatus.text = "Fetching new wallpaper from NASA..."
            binding.btnRefreshNow.isEnabled = false
            ApodWallpaperWorker.triggerImmediate(this)
        }

        // 2. Overlay Position Toggle
        binding.btnOverlayPos.setOnClickListener {
            val nextPos = prefs.overlayPosition.next()
            prefs.overlayPosition = nextPos
            updateOverlayButtonText()
            Toast.makeText(this, "Overlay set to ${nextPos.label}", Toast.LENGTH_SHORT).show()
        }

        // 3. Interval Toggle
        binding.btnInterval.setOnClickListener {
            val options = PreferencesManager.INTERVAL_OPTIONS
            val currentIndex = options.indexOf(prefs.intervalMinutes)
            val nextIndex = if (currentIndex != -1) (currentIndex + 1) % options.size else 1
            val nextMinutes = options[nextIndex]

            prefs.intervalMinutes = nextMinutes
            ApodWallpaperWorker.schedulePeriodic(this, nextMinutes, forceReplace = true)
            updateIntervalButtonText()
            Toast.makeText(this, "Periodic interval set to ${formatMinutes(nextMinutes)}", Toast.LENGTH_SHORT).show()
        }

        // 4. Photo Mode Toggle
        binding.btnMode.setOnClickListener {
            prefs.useRandom = !prefs.useRandom
            updateModeButtonText()
            val modeName = if (prefs.useRandom) "Random Archive" else "Today's APOD"
            Toast.makeText(this, "Source set to $modeName", Toast.LENGTH_SHORT).show()
        }

        // 5. Setup Guide Dialog
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
                        binding.tvStatus.text = "Status: Failed to fetch image. Check internet or API key."
                        Toast.makeText(this, "Failed to download wallpaper", Toast.LENGTH_SHORT).show()
                    }
                    else -> Unit
                }
            }
    }

    private fun refreshUiState() {
        updateOverlayButtonText()
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
            binding.tvCurrentDateCredit.text = "${prefs.lastDate} • NASA APOD"
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

    private fun updateIntervalButtonText() {
        binding.btnInterval.text = "Interval: ${formatMinutes(prefs.intervalMinutes)}"
    }

    private fun updateModeButtonText() {
        val label = if (prefs.useRandom) "Random Archive" else "Today's APOD"
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

