package com.astroframe.tv.projectivy

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.worker.ApodWallpaperWorker
import tv.projectivy.plugin.wallpaperprovider.api.Event
import tv.projectivy.plugin.wallpaperprovider.api.IWallpaperProviderService
import tv.projectivy.plugin.wallpaperprovider.api.Wallpaper
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperDisplayMode
import tv.projectivy.plugin.wallpaperprovider.api.WallpaperType
import java.io.File

/**
 * Native Projectivy Launcher Wallpaper Provider plugin service.
 * Allows users to select "AstroFrame" directly inside Projectivy Settings -> Appearance -> Wallpaper
 * without manually entering folder URIs.
 */
class AstroWallpaperProviderService : Service() {

    companion object {
        private const val TAG = "AstroProjectivyPlugin"
    }

    override fun onBind(intent: Intent): IBinder = binder

    private val binder = object : IWallpaperProviderService.Stub() {
        override fun getWallpapers(event: Event?): List<Wallpaper> {
            return when (event) {
                is Event.TimeElapsed -> {
                    val files = loadCachedWallpaperFiles()
                    if (files.isEmpty()) {
                        Log.d(TAG, "No cached wallpapers yet; triggering immediate fetch")
                        ApodWallpaperWorker.triggerImmediate(applicationContext)
                        return emptyList()
                    }

                    val prefs = PreferencesManager(applicationContext)
                    val lastTitle = prefs.lastTitle.ifBlank { "AstroFrame Artwork" }
                    val sourceName = prefs.artSource.label

                    files.shuffled().map { file ->
                        val title = if (file.name == "current_wallpaper.jpg") lastTitle else file.nameWithoutExtension
                        Wallpaper(
                            uri = Uri.fromFile(file).toString(),
                            type = WallpaperType.IMAGE,
                            displayMode = WallpaperDisplayMode.CROP,
                            title = title,
                            source = sourceName
                        )
                    }
                }
                else -> emptyList()
            }
        }

        override fun getPreferences(): String? = null

        override fun setPreferences(params: String?) {
            // Preferences are managed inside AstroFrame's main TV UI
        }
    }

    private fun loadCachedWallpaperFiles(): List<File> {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "AstroFrame"
        )
        val fallbackDir = File(
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "AstroFrame"
        )

        val candidates = listOf(publicDir, fallbackDir)
            .flatMap { dir ->
                dir.listFiles { file ->
                    file.isFile && (file.name.endsWith(".jpg") || file.name.endsWith(".png")) && file.length() > 5000
                }?.toList() ?: emptyList()
            }
            .distinctBy { it.absolutePath }

        return candidates.sortedByDescending { it.lastModified() }
    }
}
