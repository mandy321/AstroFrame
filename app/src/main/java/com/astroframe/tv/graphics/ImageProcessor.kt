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
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val stream: InputStream = response.body?.byteStream() ?: return null

                // Decode bounds first to prevent OOM
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeStream(stream, null, options)
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
        val cardWidth = 660f
        val cardHeight = 220f
        val margin = 50f

        val left: Float
        val top: Float

        when (position) {
            OverlayPosition.TOP_LEFT -> {
                left = margin
                top = margin
            }
            OverlayPosition.TOP_RIGHT -> {
                left = CANVAS_WIDTH - cardWidth - margin
                top = margin
            }
            OverlayPosition.BOTTOM_LEFT -> {
                left = margin
                top = CANVAS_HEIGHT - cardHeight - margin
            }
            OverlayPosition.BOTTOM_RIGHT -> {
                left = CANVAS_WIDTH - cardWidth - margin
                top = CANVAS_HEIGHT - cardHeight - margin
            }
            OverlayPosition.NONE -> return
        }

        val right = left + cardWidth
        val bottom = top + cardHeight

        // Background rounded translucent card with subtle border
        val cardRect = RectF(left, top, right, bottom)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(165, 10, 14, 24)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)
        canvas.drawRoundRect(cardRect, 20f, 20f, borderPaint)

        var currentY = top + 34f
        val textLeft = left + 28f
        val contentWidth = (cardWidth - 56f).toInt()

        // 1. Tag / Badge (NASA APOD • Date)
        val tagPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64B5F6") // Soft cyan/blue
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tagText = "✦ NASA APOD  •  ${apod.date}"
        canvas.drawText(tagText, textLeft, currentY, tagPaint)
        currentY += 28f

        // 2. Title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }
        val titleLayout = StaticLayout.Builder.obtain(
            apod.title,
            0,
            apod.title.length,
            titlePaint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(1)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(textLeft, currentY)
        titleLayout.draw(canvas)
        canvas.restore()
        currentY += titleLayout.height + 12f

        // 3. Explanation summary
        val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(3f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }
        val summary = apod.summaryExplanation
        val descLayout = StaticLayout.Builder.obtain(
            summary,
            0,
            summary.length,
            descPaint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        canvas.save()
        canvas.translate(textLeft, currentY)
        descLayout.draw(canvas)
        canvas.restore()
        currentY += descLayout.height + 14f

        // 4. Credit / Copyright (if available)
        if (!apod.copyright.isNullOrBlank()) {
            val creditPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#B0BEC5")
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            }
            val cleanCredit = apod.copyright.replace("\n", " ").trim()
            val creditText = "Credit: $cleanCredit"
            canvas.drawText(creditText, textLeft, currentY, creditPaint)
        }
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

