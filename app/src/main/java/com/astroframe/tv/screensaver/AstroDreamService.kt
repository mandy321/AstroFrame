package com.astroframe.tv.screensaver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.astroframe.tv.R
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.worker.ApodWallpaperWorker
import java.io.File

class AstroDreamService : DreamService() {

    companion object {
        private const val TAG = "AstroDreamService"
        private const val CROSSFADE_DURATION_MS = 1200L
    }

    private lateinit var prefs: PreferencesManager
    private lateinit var imgFront: ImageView
    private lateinit var imgBack: ImageView

    private val handler = Looper.myLooper()?.let { Handler(it) } ?: Handler(Looper.getMainLooper())
    private var imageFiles: List<File> = emptyList()
    private var currentIndex = 0
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null

    private val rotationRunnable = object : Runnable {
        override fun run() {
            showNextImage()
            val rotationSec = prefs.screensaverRotationSeconds
            if (rotationSec > 0) {
                handler.postDelayed(this, rotationSec * 1000L)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isFullscreen = true
        isInteractive = false // Any user input (remote D-pad, click) immediately wakes the TV

        setContentView(R.layout.screensaver_view)
        imgFront = findViewById(R.id.imgScreensaverFront)
        imgBack = findViewById(R.id.imgScreensaverBack)

        prefs = PreferencesManager(this)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        Log.d(TAG, "AstroFrame Screensaver started dreaming")

        // Track live screensaver activation
        com.astroframe.tv.analytics.AnalyticsTracker.trackScreensaverStarted(prefs.screensaverRotationSeconds)

        reloadFiles()

        if (imageFiles.isEmpty()) {
            Log.w(TAG, "No wallpaper images found, triggering immediate fetch")
            ApodWallpaperWorker.triggerImmediate(this)
            // Retry scan in 4 seconds
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

        // Place current_wallpaper.jpg first if present, followed by history
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

        // 1. Initial display of first image
        val firstFile = imageFiles[0]
        val bmp = decodeImageFile(firstFile) ?: return
        currentBitmap = bmp
        imgFront.setImageBitmap(bmp)
        imgFront.alpha = 1f
        imgBack.visibility = View.GONE

        // 2. Schedule rotation if enabled
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

        // Smooth cross-fade animation
        imgFront.animate()
            .alpha(0f)
            .setDuration(CROSSFADE_DURATION_MS)
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

    private fun stopSlideshow() {
        handler.removeCallbacks(rotationRunnable)
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
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed decoding ${file.name}", e)
            null
        }
    }
}

