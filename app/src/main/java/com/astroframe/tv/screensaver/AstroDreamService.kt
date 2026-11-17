package com.astroframe.tv.screensaver

import android.animation.TimeInterpolator
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.astroframe.tv.R
import com.astroframe.tv.data.MatStyle
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.ui.MuseumFrameView
import com.astroframe.tv.worker.ApodWallpaperWorker
import java.io.File
import kotlin.random.Random

class AstroDreamService : DreamService() {

    companion object {
        private const val TAG = "AstroDreamService"
        private const val CROSSFADE_DURATION_MS = 1500L
        private const val OLED_DRIFT_INTERVAL_MS = 90_000L // Micro-shift canvas every 90s to prevent burn-in
        private const val TELEMETRY_AUTO_HIDE_MS = 8000L
    }

    private lateinit var prefs: PreferencesManager
    private lateinit var museumFrameContainer: MuseumFrameView
    private lateinit var artCanvasContainer: View
    private lateinit var imgFront: ImageView
    private lateinit var imgBack: ImageView
    private lateinit var telemetryPill: View
    private lateinit var txtArtworkTitle: TextView
    private lateinit var txtArtworkSubtitle: TextView
    private lateinit var txtTelemetryBadge: TextView

    private val handler = Looper.myLooper()?.let { Handler(it) } ?: Handler(Looper.getMainLooper())
    private var imageFiles: List<File> = emptyList()
    private var currentIndex = 0
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var sessionStartTimeMs: Long = 0L

    // OLED Care Organic Drift State
    private var currentDriftX = 0f
    private var currentDriftY = 0f

    private val rotationRunnable = object : Runnable {
        override fun run() {
            showNextImage()
            val rotationSec = prefs.screensaverRotationSeconds
            if (rotationSec > 0) {
                handler.postDelayed(this, rotationSec * 1000L)
            }
        }
    }

    // OLED Care Micro-drift: shifts canvas by -3 to +3 pixels imperceptibly
    private val oledCareRunnable = object : Runnable {
        override fun run() {
            if (prefs.oledCareEnabled) {
                applyOledMicroDrift()
                handler.postDelayed(this, OLED_DRIFT_INTERVAL_MS)
            }
        }
    }

    private val hideTelemetryRunnable = Runnable {
        telemetryPill.animate()
            .alpha(0f)
            .translationY(30f)
            .setDuration(800L)
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false // Any remote interaction wakes the display

        // Enable 10-bit Wide Color Gamut (Display P3 / BT.2020) on supported Android TV hardware
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                window?.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
                Log.d(TAG, "Hardware Wide Color Gamut enabled on display window")
            } catch (e: Exception) {
                Log.w(TAG, "Wide color gamut not supported on this TV: ${e.message}")
            }
        }

        setContentView(R.layout.screensaver_view)
        museumFrameContainer = findViewById(R.id.museumFrameContainer)
        artCanvasContainer = findViewById(R.id.artCanvasContainer)
        imgFront = findViewById(R.id.imgScreensaverFront)
        imgBack = findViewById(R.id.imgScreensaverBack)
        telemetryPill = findViewById(R.id.telemetryPill)
        txtArtworkTitle = findViewById(R.id.txtArtworkTitle)
        txtArtworkSubtitle = findViewById(R.id.txtArtworkSubtitle)
        txtTelemetryBadge = findViewById(R.id.txtTelemetryBadge)

        prefs = PreferencesManager(this)

        // Apply museum framing preference
        museumFrameContainer.setMatStyle(prefs.matStyle)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "AstroFrame Screensaver started dreaming")

        sessionStartTimeMs = System.currentTimeMillis()
        com.astroframe.tv.analytics.AnalyticsTracker.trackScreensaverStarted(prefs.screensaverRotationSeconds)

        reloadFiles()

        // Start OLED care burn-in prevention loop
        if (prefs.oledCareEnabled) {
            handler.postDelayed(oledCareRunnable, OLED_DRIFT_INTERVAL_MS)
        }

        if (imageFiles.isEmpty()) {
            Log.w(TAG, "No wallpaper images found, triggering immediate fetch")
            ApodWallpaperWorker.triggerImmediate(this)
            handler.postDelayed({
                reloadFiles()
                startSlideshow()
            }, 4000L)
            return
        }

        startSlideshow()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        Log.d(TAG, "AstroFrame Screensaver stopped dreaming")
        if (sessionStartTimeMs > 0L) {
            val durationSec = (System.currentTimeMillis() - sessionStartTimeMs) / 1000L
            com.astroframe.tv.analytics.AnalyticsTracker.trackScreensaverStopped(
                durationSeconds = durationSec,
                rotationSpeedSec = prefs.screensaverRotationSeconds
            )
            sessionStartTimeMs = 0L
        }
        stopSlideshow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopSlideshow()
        recycleBitmaps()
    }

    private fun reloadFiles() {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val astroDir = File(picturesDir, "AstroFrame")

        val files = astroDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png")) && file.length() > 5000
        }?.toList() ?: emptyList()

        val sorted = files.sortedWith { a, b ->
            if (a.name == "current_wallpaper.jpg") -1
            else if (b.name == "current_wallpaper.jpg") 1
            else b.lastModified().compareTo(a.lastModified())
        }

        imageFiles = sorted
        Log.d(TAG, "Loaded ${imageFiles.size} images for screensaver")
    }

    private fun startSlideshow() {
        if (imageFiles.isEmpty()) return

        val firstFile = imageFiles[0]
        val bmp = decodeImageFile(firstFile) ?: return
        currentBitmap = bmp
        imgFront.setImageBitmap(bmp)
        imgFront.alpha = 1f
        imgBack.visibility = View.GONE

        updateTelemetryOverlay(firstFile)

        val rotationSec = prefs.screensaverRotationSeconds
        if (rotationSec > 0 && imageFiles.size > 1) {
            handler.postDelayed(rotationRunnable, rotationSec * 1000L)
        }
    }

    private fun showNextImage() {
        if (imageFiles.size <= 1) return

        currentIndex = (currentIndex + 1) % imageFiles.size
        val nextFile = imageFiles[currentIndex]
        val bmp = decodeImageFile(nextFile) ?: return

        nextBitmap = bmp
        imgBack.setImageBitmap(bmp)
        imgBack.visibility = View.VISIBLE

        updateTelemetryOverlay(nextFile)

        // Apple-style fluid crossfade
        imgFront.animate()
            .alpha(0f)
            .setDuration(CROSSFADE_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                imgFront.setImageBitmap(bmp)
                imgFront.alpha = 1f
                imgBack.visibility = View.GONE

                currentBitmap?.recycle()
                currentBitmap = nextBitmap
                nextBitmap = null
            }
            .start()
    }

    private fun updateTelemetryOverlay(file: File) {
        val cleanName = file.nameWithoutExtension
            .replace("current_wallpaper", prefs.lastTitle.ifBlank { "Deep Space Art" })
            .replace("_", " ")

        txtArtworkTitle.text = cleanName
        txtArtworkSubtitle.text = prefs.artSource.label

        txtTelemetryBadge.text = if (prefs.oledCareEnabled) {
            "✦ 10-BIT WIDE GAMUT • OLED PIXEL-SHIFT ACTIVE"
        } else {
            "✦ 10-BIT HARDWARE DEPTH • THE FRAME MODE"
        }

        // Show Apple TV glassmorphism telemetry pill, then gently slide down after 8s
        telemetryPill.animate().cancel()
        telemetryPill.alpha = 1f
        telemetryPill.translationY = 0f
        handler.removeCallbacks(hideTelemetryRunnable)
        handler.postDelayed(hideTelemetryRunnable, TELEMETRY_AUTO_HIDE_MS)
    }

    /**
     * Organic Pixel-Shift Engine:
     * Displaces the canvas by +/- 3 pixels so static elements never cause image retention.
     */
    private fun applyOledMicroDrift() {
        val deltaX = (Random.nextInt(7) - 3).toFloat()
        val deltaY = (Random.nextInt(7) - 3).toFloat()

        currentDriftX = deltaX
        currentDriftY = deltaY

        artCanvasContainer.animate()
            .translationX(currentDriftX)
            .translationY(currentDriftY)
            .setDuration(2500L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        telemetryPill.animate()
            .translationX(currentDriftX)
            .translationY(currentDriftY)
            .setDuration(2500L)
            .start()

        Log.d(TAG, "OLED Care drift applied: dx=$deltaX, dy=$deltaY")
    }

    private fun stopSlideshow() {
        handler.removeCallbacks(rotationRunnable)
        handler.removeCallbacks(oledCareRunnable)
        handler.removeCallbacks(hideTelemetryRunnable)
        handler.removeCallbacksAndMessages(null)
    }

    private fun recycleBitmaps() {
        try {
            currentBitmap?.recycle()
            nextBitmap?.recycle()
            currentBitmap = null
            nextBitmap = null
        } catch (e: Exception) {
            Log.w(TAG, "Error recycling bitmaps: ${e.message}")
        }
    }

    /**
     * High-Precision 10-bit / RGBA_F16 / ARGB_8888 Decoder:
     * Prefers RGBA_F16 on Android 8.0+ for maximum gradient smoothness without banding.
     */
    private fun decodeImageFile(file: File): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && prefs.wideColorGamut10Bit) {
                    inPreferredConfig = Bitmap.Config.RGBA_F16
                } else {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed decoding ${file.name}", e)
            null
        }
    }
}
