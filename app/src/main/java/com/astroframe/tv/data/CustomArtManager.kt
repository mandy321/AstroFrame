package com.astroframe.tv.data

import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CustomArtManager {
    private const val TAG = "CustomArtManager"

    fun getCustomArtDirectory(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val customDir = File(picturesDir, "AstroFrame_Custom")
        if (!customDir.exists()) {
            customDir.mkdirs()
        }
        return customDir
    }

    suspend fun fetchRandomCustomArtwork(): ApodItem? = withContext(Dispatchers.IO) {
        val customDir = getCustomArtDirectory()
        val files = customDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                    file.name.endsWith(".png", ignoreCase = true)) &&
                    file.length() > 5000
        }?.toList() ?: emptyList()

        if (files.isEmpty()) {
            Log.d(TAG, "No user photos found in ${customDir.absolutePath}")
            return@withContext null
        }

        val chosenFile = files.random()
        val cleanTitle = chosenFile.nameWithoutExtension.replace("_", " ").trim()

        ApodItem(
            title = cleanTitle.ifBlank { "Personal Gallery Photo" },
            explanation = "Imported from your custom TV album: ${chosenFile.name}",
            date = "Custom Gallery",
            mediaType = "image",
            url = chosenFile.absolutePath,
            hdUrl = chosenFile.absolutePath,
            copyright = "Personal Collection",
            sourceBadge = "✦ PERSONAL GALLERY",
            sourceColor = "#81C784"
        )
    }

    fun getCustomPhotoCount(): Int {
        val customDir = getCustomArtDirectory()
        return customDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) ||
                    file.name.endsWith(".jpeg", ignoreCase = true) ||
                    file.name.endsWith(".png", ignoreCase = true)) &&
                    file.length() > 5000
        }?.size ?: 0
    }

    suspend fun fetchFromCustomUrl(customUrl: String): ApodItem? = withContext(Dispatchers.IO) {
        if (customUrl.isBlank()) return@withContext null
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            // If it directly ends with an image extension, treat it as direct image URL
            val lower = customUrl.lowercase()
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
                return@withContext ApodItem(
                    title = "Custom Feed Artwork",
                    explanation = "Curated via custom remote endpoint: $customUrl",
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
                    mediaType = "image",
                    url = customUrl,
                    hdUrl = customUrl,
                    copyright = "Custom Remote Source",
                    sourceBadge = "✦ CUSTOM URL FEED",
                    sourceColor = "#81C784"
                )
            }

            // Otherwise, attempt to parse JSON response (APOD schema or array of artworks)
            val request = okhttp3.Request.Builder()
                .url(customUrl)
                .header("User-Agent", "Mozilla/5.0 AstroFrame/1.3.1 (Android TV)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val trimmed = bodyStr.trim()

                if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    if (arr.length() == 0) return@withContext null
                    val obj = arr.getJSONObject(kotlin.random.Random.nextInt(arr.length()))
                    parseJsonItem(obj)
                } else if (trimmed.startsWith("{")) {
                    val obj = org.json.JSONObject(trimmed)
                    parseJsonItem(obj)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from custom URL: $customUrl", e)
            null
        }
    }

    private fun parseJsonItem(obj: org.json.JSONObject): ApodItem? {
        val imageUrl = when {
            obj.has("hdurl") && !obj.isNull("hdurl") -> obj.optString("hdurl")
            obj.has("url") && !obj.isNull("url") -> obj.optString("url")
            obj.has("image") && !obj.isNull("image") -> obj.optString("image")
            obj.has("imageUrl") && !obj.isNull("imageUrl") -> obj.optString("imageUrl")
            else -> null
        } ?: return null

        val title = obj.optString("title", obj.optString("name", "Custom Remote Artwork"))
        val explanation = obj.optString("explanation", obj.optString("description", "Custom remote feed item"))
        val date = obj.optString("date", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()))
        val copyright = obj.optString("copyright", obj.optString("credit", obj.optString("author", "Custom Feed")))

        return ApodItem(
            title = title,
            explanation = explanation,
            date = date,
            mediaType = "image",
            url = imageUrl,
            hdUrl = imageUrl,
            copyright = copyright,
            sourceBadge = "✦ CUSTOM URL FEED",
            sourceColor = "#81C784"
        )
    }

}
