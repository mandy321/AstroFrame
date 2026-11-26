package com.astroframe.tv.screensaver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.astroframe.tv.R
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.ui.MuseumFrameView
import com.astroframe.tv.worker.ApodWallpaperWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AstroDreamService : DreamService() {

    companion object {
        private const val TAG = "AstroDreamService"
        private const val CROSSFADE_DURATION_MS = 1400L
        private const val OLED_DRIFT_INTERVAL_MS = 90_000L
        private const val TELEMETRY_AUTO_HIDE_MS = 7000L
        private const val KEN_BURNS_SCALE_FACTOR = 1.065f
    }

    private lateinit var prefs: PreferencesManager
    private lateinit var museumFrameContainer: MuseumFrameView
    private lateinit var artCanvasContainer: View
    private lateinit var imgFront: ImageView
    private lateinit var imgBack: ImageView

    // Top Bar (Clock & Weather)
    private lateinit var topBarPill: View
    private lateinit var txtClock: TextView
    private lateinit var txtWeatherBadge: TextView

    // AuraMusic Island
    private lateinit var auraMusicPill: View
    private lateinit var txtMusicTrack: TextView
    private lateinit var txtMusicArtist: TextView

    // Bottom Telemetry & D-Pad Hints
    private lateinit var telemetryPill: View
    private lateinit var txtArtworkTitle: TextView
    private lateinit var txtArtworkSubtitle: TextView
    private lateinit var txtTelemetryBadge: TextView
    private lateinit var txtRemoteHint: TextView

    private val handler = Looper.myLooper()?.let { Handler(it) } ?: Handler(Looper.getMainLooper())
    private var imageFiles: List<File> = emptyList()
    private var currentIndex = 0
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var sessionStartTimeMs: Long = 0L

    // Playback State
    private var isPaused = false

    // OLED Drift State
    private var currentDriftX = 0f
    private var currentDriftY = 0f

    // AuraMusic Broadcast Receiver
    private val auraMusicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action = intent.action
            if (action == "com.music.auramusic.METADATA_CHANGED" || action == "com.android.music.metachanged") {
                val track = intent.getStringExtra("track") ?: intent.getStringExtra("title")
                val artist = intent.getStringExtra("artist")
                if (!track.isNullOrBlank()) {
                    txtMusicTrack.text = track
                    txtMusicArtist.text = artist ?: "AuraMusic Lossless Stream"
                    auraMusicPill.visibility = View.VISIBLE
                    auraMusicPill.alpha = 1f
                }
            } else if (action == "com.music.auramusic.PLAYBACK_STOPPED") {
                auraMusicPill.visibility = View.GONE
            }
        }
    }

    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) {
                showNextImage()
            }
            val rotationSec = prefs.screensaverRotationSeconds
            if (rotationSec > 0) {
                handler.postDelayed(this, rotationSec * 1000L)
            }
        }
    }

    private val oledCareRunnable = object : Runnable {
        override fun run() {
            if (prefs.oledCareEnabled) {
                applyOledMicroDrift()
                handler.postDelayed(this, OLED_DRIFT_INTERVAL_MS)
            }
        }
    }

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 30_000L)
        }
    }

    private val hideTelemetryRunnable = Runnable {
        telemetryPill.animate()
            .alpha(0f)
            .translationY(35f)
            .setDuration(700L)
            .start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        // Set interactive so TV remote D-Pad events (Left, Right, OK, Play/Pause) are received by AstroDreamService
        isInteractive = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                window?.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
                Log.d(TAG, "Hardware Wide Color Gamut enabled on display window")
            } catch (e: Exception) {
                Log.w(TAG, "Wide color gamut not supported: ${e.message}")
            }
        }

        setContentView(R.layout.screensaver_view)
        museumFrameContainer = findViewById(R.id.museumFrameContainer)
        artCanvasContainer = findViewById(R.id.artCanvasContainer)
        imgFront = findViewById(R.id.imgScreensaverFront)
        imgBack = findViewById(R.id.imgScreensaverBack)

        topBarPill = findViewById(R.id.topBarPill)
        txtClock = findViewById(R.id.txtClock)
        txtWeatherBadge = findViewById(R.id.txtWeatherBadge)

        auraMusicPill = findViewById(R.id.auraMusicPill)
        txtMusicTrack = findViewById(R.id.txtMusicTrack)
        txtMusicArtist = findViewById(R.id.txtMusicArtist)

        telemetryPill = findViewById(R.id.telemetryPill)
        txtArtworkTitle = findViewById(R.id.txtArtworkTitle)
        txtArtworkSubtitle = findViewById(R.id.txtArtworkSubtitle)
        txtTelemetryBadge = findViewById(R.id.txtTelemetryBadge)
        txtRemoteHint = findViewById(R.id.txtRemoteHint)

        prefs = PreferencesManager(this)
        museumFrameContainer.setMatStyle(prefs.matStyle)

        // Register AuraMusic synergy receiver
        val filter = IntentFilter().apply {
            addAction("com.music.auramusic.METADATA_CHANGED")
            addAction("com.music.auramusic.PLAYBACK_STOPPED")
            addAction("com.android.music.metachanged")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(auraMusicReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(auraMusicReceiver, filter)
        }
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "AstroFrame Screensaver started dreaming")

        sessionStartTimeMs = System.currentTimeMillis()
        com.astroframe.tv.analytics.AnalyticsTracker.trackScreensaverStarted(prefs.screensaverRotationSeconds)

        reloadFiles()
        updateClock()
        handler.post(clockRunnable)

        if (prefs.oledCareEnabled) {
            handler.postDelayed(oledCareRunnable, OLED_DRIFT_INTERVAL_MS)
        }

        if (imageFiles.isEmpty()) {
            Log.w(TAG, "No wallpaper images found on TV storage, triggering immediate fetch")
            ApodWallpaperWorker.triggerImmediate(this)
            pollForImagesAndStart(attempt = 1)
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
        try {
            unregisterReceiver(auraMusicReceiver)
        } catch (_: Exception) {}
    }

    /**
     * TV Remote D-Pad Navigation & Interactivity:
     * - Left: Previous artwork
     * - Right: Next artwork
     * - Center / OK: Toggle info pill
     * - Play/Pause / Down: Toggle Pause/Resume
     * - Back / Home: Wake TV
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    showNextImage()
                    revealTelemetry()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    showPreviousImage()
                    revealTelemetry()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_INFO -> {
                    toggleTelemetry()
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    togglePause()
                    revealTelemetry()
                    return true
                }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    finish() // Exit screensaver
                    return true
                }
            }
        }
        return try {
            super.dispatchKeyEvent(event)
        } catch (_: SecurityException) {
            true
        }
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        return try {
            super.dispatchTouchEvent(event)
        } catch (_: SecurityException) {
            true
        }
    }

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        return try {
            super.dispatchGenericMotionEvent(event)
        } catch (_: SecurityException) {
            false
        }
    }

    private fun pollForImagesAndStart(attempt: Int) {
        if (attempt > 6) {
            Log.e(TAG, "Max polling attempts reached waiting for wallpaper fetch")
            return
        }
        handler.postDelayed({
            reloadFiles()
            if (imageFiles.isNotEmpty()) {
                Log.d(TAG, "Wallpaper ready after poll attempt $attempt")
                startSlideshow()
            } else {
                Log.d(TAG, "Still waiting for wallpaper, polling attempt ${attempt + 1}...")
                pollForImagesAndStart(attempt + 1)
            }
        }, 3000L)
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

        startKenBurnsMotion(imgFront)
        updateTelemetryOverlay(firstFile)

        val rotationSec = prefs.screensaverRotationSeconds
        if (rotationSec > 0 && imageFiles.size > 1) {
            handler.postDelayed(rotationRunnable, rotationSec * 1000L)
        }
    }

    private fun showNextImage() {
        if (imageFiles.size <= 1) return
        currentIndex = (currentIndex + 1) % imageFiles.size
        transitionToImage(imageFiles[currentIndex])
    }

    private fun showPreviousImage() {
        if (imageFiles.size <= 1) return
        currentIndex = if (currentIndex - 1 < 0) imageFiles.size - 1 else currentIndex - 1
        transitionToImage(imageFiles[currentIndex])
    }

    private fun transitionToImage(file: File) {
        val bmp = decodeImageFile(file) ?: return
        nextBitmap = bmp
        imgBack.setImageBitmap(bmp)
        imgBack.visibility = View.VISIBLE

        updateTelemetryOverlay(file)
        startKenBurnsMotion(imgBack)

        imgFront.animate()
            .alpha(0f)
            .setDuration(CROSSFADE_DURATION_MS)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                imgFront.setImageBitmap(bmp)
                imgFront.alpha = 1f
                imgFront.scaleX = imgBack.scaleX
                imgFront.scaleY = imgBack.scaleY
                imgFront.translationX = imgBack.translationX
                imgFront.translationY = imgBack.translationY
                imgBack.visibility = View.GONE

                currentBitmap?.recycle()
                currentBitmap = nextBitmap
                nextBitmap = null
            }
            .start()
    }

    /**
     * Cinematic 60 FPS Ken Burns Parallax:
     * Slow, subtle zoom and pan across high-res deep-space photography.
     */
    private fun startKenBurnsMotion(view: ImageView) {
        view.animate().cancel()
        view.scaleX = 1.0f
        view.scaleY = 1.0f
        view.translationX = 0f
        view.translationY = 0f

        val targetScale = KEN_BURNS_SCALE_FACTOR
        val targetTranslateX = (Random.nextFloat() * 24f) - 12f
        val targetTranslateY = (Random.nextFloat() * 18f) - 9f

        val duration = ((prefs.screensaverRotationSeconds.coerceAtLeast(30)) * 1000L) + 2000L

        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationX(targetTranslateX)
            .translationY(targetTranslateY)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            txtRemoteHint.text = "⏸ Paused (Hold Artwork) • Press ▶ to Resume"
        } else {
            txtRemoteHint.text = "▶ Resumed  •  ◀ Previous  •  Next ▶"
        }
    }

    private fun revealTelemetry() {
        telemetryPill.animate().cancel()
        telemetryPill.alpha = 1f
        telemetryPill.translationY = 0f
        handler.removeCallbacks(hideTelemetryRunnable)
        handler.postDelayed(hideTelemetryRunnable, TELEMETRY_AUTO_HIDE_MS)
    }

    private fun toggleTelemetry() {
        if (telemetryPill.alpha > 0.5f) {
            handler.removeCallbacks(hideTelemetryRunnable)
            telemetryPill.animate().alpha(0f).translationY(35f).setDuration(500L).start()
        } else {
            revealTelemetry()
        }
    }

    private fun updateClock() {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        txtClock.text = sdf.format(Date())
    }

    private fun updateTelemetryOverlay(file: File) {
        val title = if (file.name.contains("current_wallpaper")) {
            prefs.lastTitle.ifBlank { "AstroFrame Masterpiece" }
        } else {
            file.nameWithoutExtension.replace("_", " ")
        }

        txtArtworkTitle.text = title

        val badge = if (prefs.lastSourceBadge.isNotBlank()) {
            prefs.lastSourceBadge.replace("✦", "").trim()
        } else {
            prefs.artSource.badge.replace("✦", "").trim()
        }
        val dateStr = prefs.lastDate.trim()
        val creditStr = prefs.lastCopyright.trim()

        val subtitle = when {
            creditStr.isNotBlank() && dateStr.isNotBlank() -> "$badge • $dateStr • $creditStr"
            dateStr.isNotBlank() -> "$badge • $dateStr"
            creditStr.isNotBlank() -> "$badge • $creditStr"
            else -> badge
        }

        txtArtworkSubtitle.text = subtitle

        revealTelemetry()
    }

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
        handler.removeCallbacks(clockRunnable)
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

    private fun decodeImageFile(file: File): Bitmap? {
        // Attempt 1: 10-bit wide color gamut (RGBA_F16 or ARGB_8888)
        try {
            val options = BitmapFactory.Options().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && prefs.wideColorGamut10Bit) {
                    inPreferredConfig = Bitmap.Config.RGBA_F16
                } else {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bmp != null) return bmp
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM decoding ${file.name} in 10-bit, falling back to RGB_565", e)
        } catch (e: Exception) {
            Log.w(TAG, "Failed 10-bit decode for ${file.name}: ${e.message}")
        }

        // Attempt 2: Fallback to RGB_565 (safe memory footprint for all Android TV devices)
        return try {
            val fallbackOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, fallbackOptions)
        } catch (e: Exception) {
            Log.e(TAG, "All decode attempts failed for ${file.name}", e)
            null
        }
    }
}
