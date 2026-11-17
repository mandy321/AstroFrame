package com.astroframe.tv.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.astroframe.tv.update.UpdateManager
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
        lifecycleScope.launch {
            UpdateManager.checkForUpdates(this@MainActivity, silentIfUpToDate = true)
        }
        observeWorkManager()
        refreshUiState()

        // Focus the primary button by default on TV
        binding.btnRefreshNow.requestFocus()
    }

    private fun setupButtons() {
        // 1. Refresh Now (Updates screen immediately and continues rotation)
        binding.btnRefreshNow.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvStatus.text = "Fetching and applying new artwork immediately..."
            binding.btnRefreshNow.isEnabled = false
            ApodWallpaperWorker.triggerImmediate(this)
            // Reschedule periodic worker so interval restarts from now
            ApodWallpaperWorker.schedulePeriodic(this, prefs.intervalMinutes, forceReplace = true)
        }

        // Set as TV Wallpaper Shortcut
        binding.btnWallpaperSettings.setOnClickListener {
            openSystemWallpaperSettings()
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

        // Check for Updates Button
        binding.btnCheckUpdates.setOnClickListener {
            Toast.makeText(this, "Checking for latest AstroFrame updates...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                UpdateManager.checkForUpdates(this@MainActivity, silentIfUpToDate = false)
            }
        }

        // About & Version Info Dialog
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
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

        // Apply live mat style to preview
        binding.previewMuseumFrame.setMatStyle(prefs.matStyle)

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
            val badge = if (prefs.lastSourceBadge.isNotBlank()) {
                prefs.lastSourceBadge.replace("✦", "").trim()
            } else {
                prefs.artSource.badge.replace("✦", "").trim()
            }
            val artDate = prefs.lastDate.trim()
            val creditStr = prefs.lastCopyright.trim()
            val subtitle = when {
                creditStr.isNotBlank() && artDate.isNotBlank() -> "$badge • $artDate • $creditStr"
                artDate.isNotBlank() -> "$badge • $artDate"
                creditStr.isNotBlank() -> "$badge • $creditStr"
                else -> badge
            }
            binding.tvCurrentDateCredit.text = subtitle
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
        val labels = sources.map { "${it.label}\n(${it.description})" }.toTypedArray()
        val currentIndex = sources.indexOf(prefs.artSource).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Curated Artwork Collection")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                val selectedSource = sources[which]
                prefs.artSource = selectedSource
                updateArtSourceButtonText()

                if (selectedSource == ArtSource.CUSTOM_USER_FOLDER) {
                    dialog.dismiss()
                    showCustomFolderInstructionsDialog()
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRefreshNow.isEnabled = false
                    binding.tvStatus.text = "Loading initial artwork for " + selectedSource.label + "..."
                    ApodWallpaperWorker.triggerImmediate(this)
                    Toast.makeText(this, "Collection set to ${selectedSource.label}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomFolderInstructionsDialog() {
        val customDir = com.astroframe.tv.data.CustomArtManager.getCustomArtDirectory()
        val currentCount = com.astroframe.tv.data.CustomArtManager.getCustomPhotoCount()
        val currentUrl = prefs.customApiUrl

        val urlInfo = if (currentUrl.isNotBlank()) "\n\nActive Custom URL Feed:\n$currentUrl" else ""

        val msg = """
📁 Custom Art & Personal Gallery is active!

Local Photos Detected: $currentCount$urlInfo

Choose an option below:
• 'Enter Custom URL': Stream from any JSON feed, Google Drive direct link, Imgur album, or image endpoint.
• 'Folder Instructions': Learn how to transfer photos via USB, phone, or Wi-Fi to:
  ${customDir.absolutePath}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📁 Custom Art & Photos Setup")
            .setMessage(msg)
            .setPositiveButton("Enter Custom URL") { dialog, _ ->
                dialog.dismiss()
                showCustomUrlInputDialog()
            }
            .setNeutralButton("Folder Guide") { dialog, _ ->
                dialog.dismiss()
                showDetailedFolderGuide()
            }
            .setNegativeButton("Done") { dialog, _ ->
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRefreshNow.isEnabled = false
                ApodWallpaperWorker.triggerImmediate(this)
                dialog.dismiss()
            }
            .show()
    }

    private fun showCustomUrlInputDialog() {
        val input = android.widget.EditText(this).apply {
            setText(prefs.customApiUrl)
            hint = "https://example.com/art.json or direct .jpg/.png"
            setSingleLine(true)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Custom URL Feed / Endpoint")
            .setMessage("""
Enter a custom image URL or JSON API endpoint:

Supported Formats:
1. Direct Image URL (.jpg, .png, .webp)
2. JSON Object:
   {"url": "https://...", "title": "My Art", "explanation": "..."}
3. JSON Array of items:
   [{"url": "https://...", "title": "Art 1"}, {"url": "https://...", "title": "Art 2"}]

AstroFrame will automatically pull and rotate wallpapers from this feed!
            """.trimIndent())
            .setView(input)
            .setPositiveButton("Save & Fetch") { _, _ ->
                val newUrl = input.text.toString().trim()
                prefs.customApiUrl = newUrl
                Toast.makeText(this, if (newUrl.isNotBlank()) "Custom URL Feed saved" else "Custom URL cleared", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRefreshNow.isEnabled = false
                ApodWallpaperWorker.triggerImmediate(this)
            }
            .setNeutralButton("Clear URL") { _, _ ->
                prefs.customApiUrl = ""
                Toast.makeText(this, "Cleared custom URL. Using local folder.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDetailedFolderGuide() {
        val customDir = com.astroframe.tv.data.CustomArtManager.getCustomArtDirectory()
        val guide = """
How to transfer your personal photos to TV:

1. Target Directory on TV:
   ${customDir.absolutePath}

2. Transfer Methods:
   • USB Flash Drive: Plug into TV USB port and copy files using FX File Explorer or TV File Commander.
   • Send Files to TV (SFTTV): Free app on Google Play TV store. Send files from your Android phone or PC directly.
   • Local Network / SMB: Access your TV over Wi-Fi network storage.
   • Computer (ADB Command):
     adb push /path/to/photos/* ${customDir.absolutePath}/

Supported formats: JPG, PNG, WEBP (up to 4K / 8K).
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📁 TV Folder Transfer Guide")
            .setMessage(guide)
            .setPositiveButton("Got It") { dialog, _ ->
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRefreshNow.isEnabled = false
                ApodWallpaperWorker.triggerImmediate(this)
                dialog.dismiss()
            }
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
        binding.btnMatStyle.text = "Frame: " + prefs.matStyle.label
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
                binding.previewMuseumFrame.setMatStyle(selectedStyle)
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

        
    private fun showAboutDialog() {
        val version = com.astroframe.tv.BuildConfig.VERSION_NAME
        val versionCode = com.astroframe.tv.BuildConfig.VERSION_CODE
        val buildType = com.astroframe.tv.BuildConfig.BUILD_TYPE

        val msg = """
✨ AstroFrame for Android TV & Google TV
Version: v$version (Build $versionCode)
Channel: Production ($buildType)

Transform your Living Room TV into a world-class gallery & deep space observatory:
• Realistic Wood Molding Frames (Teak, Walnut, Ebony, Gilded Gold Leaf, Birch)
• NASA Deep Space Observatory & JWST
• World Museum Masterpieces (The Met, Rijksmuseum, Art Institute of Chicago)
• Earth from Orbit 4K ISS Astronaut Views
• Custom Personal Photo & 4K Wallpaper Feeds
• OLED Burn-in Protection & Ken Burns Parallax

Created with ❤️ for Android TV & Projectivy Launcher.
GitHub: github.com/mandy321/AstroFrame
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("ℹ️ About AstroFrame")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setNeutralButton("Check for Updates") { _, _ ->
                lifecycleScope.launch {
                    UpdateManager.checkForUpdates(this@MainActivity, silentIfUpToDate = false)
                }
            }
            .show()
    }

    private fun openSystemScreensaverSettings() {
        val intents = listOf(
            // 1. Android TV / Google TV Direct Daydream Chooser
            android.content.Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.display.daydream.DaydreamActivity"),
            // 2. Google TV Settings Screensaver Deep Link
            android.content.Intent("android.settings.DREAM_SETTINGS"),
            // 3. Fallback Display Settings
            android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS),
            // 4. Fallback Main Settings
            android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return
            } catch (_: Exception) {
                // Continue to next fallback
            }
        }
        Toast.makeText(this, "Open TV Settings > System > Ambient Mode / Screensaver", Toast.LENGTH_LONG).show()
    }


    private fun openSystemWallpaperSettings() {
        // Attempt to apply current image immediately to system wallpaper
        val lastPath = prefs.lastImagePath
        if (lastPath.isNotBlank()) {
            val imgFile = java.io.File(lastPath)
            if (imgFile.exists()) {
                try {
                    val bmp = BitmapFactory.decodeFile(imgFile.absolutePath)
                    if (bmp != null) {
                        android.app.WallpaperManager.getInstance(this).setBitmap(bmp)
                        Toast.makeText(this, "Applied current artwork to system wallpaper!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainActivity", "Failed direct wallpaper set: ${e.message}")
                }
            }
        }

        val intents = listOf(
            android.content.Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
            android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER),
            android.content.Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.display.daydream.DaydreamActivity"),
            android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
        Toast.makeText(this, "Wallpaper active! Open TV Settings > Display / Wallpaper", Toast.LENGTH_LONG).show()
    }

}
