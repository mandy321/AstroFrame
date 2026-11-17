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
}
