package com.astroframe.tv.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.astroframe.tv.data.*
import com.astroframe.tv.graphics.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ApodWallpaperWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ApodWallpaperWorker"
        const val WORK_NAME_PERIODIC = "AstroFrameWallpaperWorker"
        const val WORK_NAME_IMMEDIATE = "AstroFrameImmediateWorker"

        fun schedulePeriodic(context: Context, intervalMinutes: Int, forceReplace: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<ApodWallpaperWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag("PERIODIC")
                .build()

            val policy = if (forceReplace) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                policy,
                periodicWork
            )
            Log.d(TAG, "Scheduled wallpaper rotation every $intervalMinutes mins")
        }

        fun triggerImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateWork = OneTimeWorkRequestBuilder<ApodWallpaperWorker>()
                .setConstraints(constraints)
                .addTag("IMMEDIATE")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                immediateWork
            )
            Log.d(TAG, "Triggered immediate wallpaper fetch")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(context)
        val apiKey = prefs.apiKey.ifBlank { com.astroframe.tv.BuildConfig.NASA_API_KEY }
        val apodClient = ApodApiClient()
        val worldMuseumClient = WorldMuseumApiClient()
        val earthOrbitClient = EarthOrbitApiClient()
        val imageProcessor = ImageProcessor(context)

        Log.d(TAG, "Executing wallpaper update for source: ${prefs.artSource}...")

        val apod: ApodItem? = when (prefs.artSource) {
            ArtSource.NASA_COSMOS -> {
                apodClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
            }

            ArtSource.WORLD_MASTERPIECES -> {
                worldMuseumClient.fetchMasterpiece() ?: apodClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
            }

            ArtSource.EARTH_ORBIT -> {
                earthOrbitClient.fetchEarthOrbitPhoto()
            }

            ArtSource.CUSTOM_USER_FOLDER -> {
                CustomArtManager.fetchRandomCustomArtwork() ?: run {
                    Log.w(TAG, "User folder empty, providing guide and fallback")
                    worldMuseumClient.fetchMasterpiece() ?: apodClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
                }
            }

            ArtSource.ULTIMATE_MIX -> {
                // Intelligent 4-way shuffle
                val customCount = CustomArtManager.getCustomPhotoCount()
                val roll = Random.nextInt(if (customCount > 0) 4 else 3)
                when (roll) {
                    0 -> apodClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
                    1 -> worldMuseumClient.fetchMasterpiece()
                    2 -> earthOrbitClient.fetchEarthOrbitPhoto()
                    else -> CustomArtManager.fetchRandomCustomArtwork() ?: worldMuseumClient.fetchMasterpiece()
                }
            }
        }

        if (apod == null) {
            Log.e(TAG, "Failed to retrieve artwork or photography item")
            return@withContext Result.retry()
        }

        // If it's a local user photo, copy/process directly
        val savedFile = if (apod.url.startsWith("/")) {
            // Local file on storage
            imageProcessor.processAndSaveLocalWallpaper(
                localFilePath = apod.url,
                apod = apod,
                position = prefs.overlayPosition
            )
        } else {
            imageProcessor.processAndSaveWallpaper(
                apod = apod,
                position = prefs.overlayPosition
            )
        }

        if (savedFile != null && savedFile.exists()) {
            prefs.lastTitle = apod.title
            prefs.lastDate = apod.date
            prefs.lastExplanation = apod.summaryExplanation
            prefs.lastUpdatedMs = System.currentTimeMillis()
            prefs.lastImagePath = savedFile.absolutePath

            com.astroframe.tv.analytics.AnalyticsTracker.trackWallpaperUpdated(
                artSource = apod.sourceBadge,
                title = apod.title,
                dateOrPeriod = apod.date
            )

            Log.d(TAG, "Successfully updated wallpaper: ${apod.title} (${savedFile.absolutePath})")
            Result.success()
        } else {
            Log.e(TAG, "Failed to generate or save wallpaper bitmap")
            Result.retry()
        }
    }
}
