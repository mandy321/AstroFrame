package com.astroframe.tv.graphics

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.astroframe.tv.data.ApodItem
import com.astroframe.tv.data.OverlayPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageProcessor(private val context: Context) {

    companion object {
        private const val TAG = "ImageProcessor"
        const val CANVAS_WIDTH = 1920
        const val CANVAS_HEIGHT = 1080
    }

    private val client = OkHttpClient()

    suspend fun processAndSaveLocalWallpaper(
        localFilePath: String,
        apod: ApodItem,
        position: OverlayPosition
    ): File? = withContext(Dispatchers.IO) {
        val rawBitmap = try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(localFilePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding local photo: $localFilePath", e)
            null
        } ?: return@withContext null

        val finalBitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        drawScaledCenterCrop(canvas, rawBitmap)
        rawBitmap.recycle()

        if (position != OverlayPosition.NONE) {
            drawInfoOverlay(canvas, apod, position)
        }

        val savedFile = saveBitmapToStorage(finalBitmap, apod.date)
        try {
            WallpaperManager.getInstance(context).setBitmap(finalBitmap)
        } catch (_: Exception) {}

        finalBitmap.recycle()
        savedFile
    }

    suspend fun processAndSaveWallpaper(
        apod: ApodItem,
        position: OverlayPosition
    ): File? = withContext(Dispatchers.IO) {
        val imageUrl = apod.bestImageUrl
        Log.d(TAG, "Downloading image: $imageUrl")

        val rawBitmap = downloadBitmap(imageUrl) ?: run {
            Log.e(TAG, "Failed to download image from $imageUrl")
            return@withContext null
        }

        val finalBitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        // 1. Draw scaled / center-cropped background
        drawScaledCenterCrop(canvas, rawBitmap)
        rawBitmap.recycle()

        // 2. Render Overlay if requested
        if (position != OverlayPosition.NONE) {
            drawInfoOverlay(canvas, apod, position)
        }

        // 3. Save to Pictures/AstroFrame
        val savedFile = saveBitmapToStorage(finalBitmap, apod.date)

        // 4. Try updating system wallpaper for launchers that track system wallpaper
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.setBitmap(finalBitmap)
            Log.d(TAG, "System WallpaperManager updated successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to set system wallpaper (normal on some TV ROMs): ${e.message}")
        }

        finalBitmap.recycle()
        savedFile
    }

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bytes = response.body?.bytes() ?: return null

                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading bitmap", e)
            null
        }
    }

    private fun drawScaledCenterCrop(canvas: Canvas, source: Bitmap) {
        val srcWidth = source.width.toFloat()
        val srcHeight = source.height.toFloat()
        val dstWidth = CANVAS_WIDTH.toFloat()
        val dstHeight = CANVAS_HEIGHT.toFloat()

        val scale = maxOf(dstWidth / srcWidth, dstHeight / srcHeight)
        val scaledW = srcWidth * scale
        val scaledH = srcHeight * scale

        val dx = (dstWidth - scaledW) / 2f
        val dy = (dstHeight - scaledH) / 2f

        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)
    }

    private fun drawInfoOverlay(
        canvas: Canvas,
        apod: ApodItem,
        position: OverlayPosition
    ) {
        if (position == OverlayPosition.NONE) return

        // 100% Floating Gallery Typography (Zero Opaque Box, Pure Art Immersion)
        val textWidth = 860
        val marginX = 64f
        val marginY = 56f
        val blockHeight = 130f

        val left: Float
        val top: Float

        when (position) {
            OverlayPosition.TOP_LEFT -> {
                left = marginX
                top = marginY
            }
            OverlayPosition.TOP_RIGHT -> {
                left = CANVAS_WIDTH - textWidth - marginX
                top = marginY
            }
            OverlayPosition.BOTTOM_LEFT -> {
                left = marginX
                top = CANVAS_HEIGHT - blockHeight - marginY
            }
            OverlayPosition.BOTTOM_RIGHT -> {
                left = CANVAS_WIDTH - textWidth - marginX
                top = CANVAS_HEIGHT - blockHeight - marginY
            }
            OverlayPosition.NONE -> return
        }

        var currentY = top

        // 1. Artwork Title (Crisp white with studio drop shadow)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 1.5f, 1.5f, Color.argb(235, 0, 0, 0))
        }
        val titleLayout = StaticLayout.Builder.obtain(
            apod.title,
            0,
            apod.title.length,
            titlePaint,
            textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(left, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 4f

        // 2. Description Summary & Attribution (Elegant, subtle soft silver with drop shadow)
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E6ED")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(4f, 1.2f, 1.2f, Color.argb(220, 0, 0, 0))
        }

        val credit = if (!apod.copyright.isNullOrBlank()) {
            "${apod.sourceBadge} • ${apod.date} • ${apod.copyright.replace("\n", " ").trim()}"
        } else {
            "${apod.sourceBadge} • ${apod.date}"
        }
        val summary = apod.summaryExplanation
        val combinedDesc = if (summary.isNotBlank() && !summary.equals(apod.title, ignoreCase = true)) {
            "$credit — $summary"
        } else {
            credit
        }

        val descLayout = StaticLayout.Builder.obtain(
            combinedDesc,
            0,
            combinedDesc.length,
            descPaint,
            textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(left, currentY)
        descLayout.draw(canvas)
        canvas.restore()
    }

    private fun saveBitmapToStorage(bitmap: Bitmap, date: String): File? {
        return try {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val astroDir = File(picturesDir, "AstroFrame")
            if (!astroDir.exists()) {
                astroDir.mkdirs()
            }

            // Always write current_wallpaper.jpg (single static target for Projectivy)
            val currentFile = File(astroDir, "current_wallpaper.jpg")
            FileOutputStream(currentFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Also keep rolling history in the same directory (so Projectivy slideshow can cycle images)
            val safeDate = date.ifBlank {
                SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
            }
            val historyFile = File(astroDir, "apod_$safeDate.jpg")
            FileOutputStream(historyFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            // Prune older files: max 10 images, and auto-delete anything older than 2 days
            cleanOldFiles(astroDir, maxFiles = 10, maxAgeHours = 48)

            Log.d(TAG, "Saved wallpaper successfully to ${currentFile.absolutePath}")
            currentFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save wallpaper to storage", e)
            // Fallback to app external files dir
            val fallbackDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "AstroFrame")
            fallbackDir.mkdirs()
            val fallbackFile = File(fallbackDir, "current_wallpaper.jpg")
            FileOutputStream(fallbackFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            fallbackFile
        }
    }

    private fun cleanOldFiles(dir: File, maxFiles: Int, maxAgeHours: Long = 48) {
        try {
            val files = dir.listFiles { file ->
                file.name.startsWith("apod_") && file.name.endsWith(".jpg")
            } ?: return

            val cutoffMs = System.currentTimeMillis() - (maxAgeHours * 60 * 60 * 1000L)

            // 1. Delete files older than max age
            val remaining = mutableListOf<File>()
            for (file in files) {
                if (file.lastModified() < cutoffMs) {
                    file.delete()
                    Log.d(TAG, "Auto-deleted old wallpaper past $maxAgeHours hours: ${file.name}")
                } else {
                    remaining.add(file)
                }
            }

            // 2. Also cap total count to maxFiles
            if (remaining.size > maxFiles) {
                remaining.sortBy { it.lastModified() }
                val toDelete = remaining.size - maxFiles
                for (i in 0 until toDelete) {
                    remaining[i].delete()
                    Log.d(TAG, "Pruned excess wallpaper: ${remaining[i].name}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning old files: ${e.message}")
        }
    }
}

