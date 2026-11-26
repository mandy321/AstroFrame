package com.astroframe.tv.screensaver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.astroframe.tv.R
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.ui.MuseumFrameView
import com.astroframe.tv.worker.ApodWallpaperWorker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fullscreen in-app screensaver preview activity using the same layout & wood frame
 * as [AstroDreamService] so users can test D-pad controls and framing immediately.
 */
class ScreensaverPreviewActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager
    private lateinit var imgFront: ImageView
    private lateinit var imgBack: ImageView
    private lateinit var txtClock: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtSubtitle: TextView
    private lateinit var telemetryPill: View

    private val handler = Handler(Looper.getMainLooper())
    private var imageFiles: List<File> = emptyList()
    private var currentIndex = 0
    private var currentBitmap: Bitmap? = null
    private var nextBitmap: Bitmap? = null
    private var isPaused = false

    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) stepImage(1)
            scheduleNext()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.screensaver_view)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        prefs = PreferencesManager(this)
        findViewById<MuseumFrameView>(R.id.museumFrameContainer).setMatStyle(prefs.matStyle)
        imgFront = findViewById(R.id.imgScreensaverFront)
        imgBack = findViewById(R.id.imgScreensaverBack)
        txtClock = findViewById(R.id.txtClock)
        txtTitle = findViewById(R.id.txtArtworkTitle)
        txtSubtitle = findViewById(R.id.txtArtworkSubtitle)
        telemetryPill = findViewById(R.id.telemetryPill)

        txtClock.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        reloadFiles()
        if (imageFiles.isEmpty()) {
            ApodWallpaperWorker.triggerImmediate(this)
            txtTitle.text = "Fetching 4K Artwork..."
            txtSubtitle.text = "Please wait a moment"
        } else {
            showInitial()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    stepImage(1)
                    scheduleNext()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    stepImage(-1)
                    scheduleNext()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    isPaused = !isPaused
                    txtSubtitle.text = if (isPaused) "Preview Paused" else "Preview Active"
                    telemetryPill.animate().alpha(1f).translationY(0f).setDuration(250L).start()
                    return true
                }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    finish()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun reloadFiles() {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AstroFrame")
        imageFiles = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png")) && file.length() > 5000
        }?.sortedWith { a, b ->
            when {
                a.name == "current_wallpaper.jpg" -> -1
                b.name == "current_wallpaper.jpg" -> 1
                else -> b.lastModified().compareTo(a.lastModified())
            }
        } ?: emptyList()
    }

    private fun showInitial() {
        if (imageFiles.isEmpty()) return
        val file = imageFiles[0]
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return
        currentBitmap = bmp
        imgFront.setImageBitmap(bmp)
        txtTitle.text = if (file.name.contains("current_wallpaper")) prefs.lastTitle.ifBlank { "AstroFrame Preview" } else file.nameWithoutExtension
        txtSubtitle.text = "Screensaver Preview • Left/Right to Skip • Back to Exit"
        scheduleNext()
    }

    private fun stepImage(delta: Int) {
        if (imageFiles.size <= 1) return
        currentIndex = ((currentIndex + delta) % imageFiles.size + imageFiles.size) % imageFiles.size
        val file = imageFiles[currentIndex]
        val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return
        nextBitmap = bmp
        imgBack.setImageBitmap(bmp)
        imgBack.visibility = View.VISIBLE
        imgFront.animate().alpha(0f).setDuration(1000L).withEndAction {
            imgFront.setImageBitmap(bmp)
            imgFront.alpha = 1f
            imgBack.visibility = View.GONE
            currentBitmap?.recycle()
            currentBitmap = nextBitmap
            nextBitmap = null
        }.start()
    }

    private fun scheduleNext() {
        handler.removeCallbacks(rotationRunnable)
        val sec = prefs.screensaverRotationSeconds
        if (!isPaused && sec > 0 && imageFiles.size > 1) {
            handler.postDelayed(rotationRunnable, sec * 1000L)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        currentBitmap?.recycle()
        nextBitmap?.recycle()
    }
}
