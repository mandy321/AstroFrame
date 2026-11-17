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

        // High-end Gallery Minimalist Floating Pill (Whisper-light 10% translucent glass)
        val pillWidth = 580f
        val pillHeight = 110f
        val marginX = 54f
        val marginY = 46f

        val left: Float
        val top: Float

        when (position) {
            OverlayPosition.TOP_LEFT -> {
                left = marginX
                top = marginY
            }
            OverlayPosition.TOP_RIGHT -> {
                left = CANVAS_WIDTH - pillWidth - marginX
                top = marginY
            }
            OverlayPosition.BOTTOM_LEFT -> {
                left = marginX
                top = CANVAS_HEIGHT - pillHeight - marginY
            }
            OverlayPosition.BOTTOM_RIGHT -> {
                left = CANVAS_WIDTH - pillWidth - marginX
                top = CANVAS_HEIGHT - pillHeight - marginY
            }
            OverlayPosition.NONE -> return
        }

        val right = left + pillWidth
        val bottom = top + pillHeight

        // Whisper-light 10% dark glass tint with delicate hairline border
        val pillRect = RectF(left, top, right, bottom)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(32, 0, 0, 0)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRoundRect(pillRect, 22f, 22f, pillPaint)
        canvas.drawRoundRect(pillRect, 22f, 22f, borderPaint)

        val textLeft = left + 26f
        val contentWidth = (pillWidth - 52f).toInt()
        var currentY = top + 42f

        // 1. Artwork Title (Clean, elegant 24sp white with drop shadow for 100% legibility)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(4f, 1f, 1f, Color.argb(220, 0, 0, 0))
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
        currentY += titleLayout.height + 6f

        // 2. Curated Subtitle (Date & Source Badge)
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CFD8DC")
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(3f, 1f, 1f, Color.argb(180, 0, 0, 0))
        }
        val subText = "${apod.date}  •  ${apod.sourceBadge}"
        canvas.drawText(subText, textLeft, currentY + 18f, subPaint)
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

