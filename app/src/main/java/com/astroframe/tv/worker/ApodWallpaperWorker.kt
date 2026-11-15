package com.astroframe.tv.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.astroframe.tv.data.ApodApiClient
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.graphics.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ApodWallpaperWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "ApodWallpaperWorker"
        private const val UNIQUE_WORK_NAME = "astroframe_periodic_wallpaper"
        private const val IMMEDIATE_WORK_NAME = "astroframe_immediate_wallpaper"

        fun schedulePeriodic(
            context: Context,
            intervalMinutes: Int,
            forceReplace: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // WorkManager requires minimum 15 minutes for periodic work
            val clampedInterval = maxOf(intervalMinutes.toLong(), 15L)

            val workRequest = PeriodicWorkRequestBuilder<ApodWallpaperWorker>(
                clampedInterval,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            val existingPolicy = if (forceReplace) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                existingPolicy,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic APOD wallpaper update every $clampedInterval minutes")
        }

        fun triggerImmediate(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<ApodWallpaperWorker>()
                .setConstraints(constraints)
                .addTag("IMMEDIATE")
                .build()

            return WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = PreferencesManager(context)
        val apiKey = prefs.apiKey.ifBlank { com.astroframe.tv.BuildConfig.NASA_API_KEY }
        val apiClient = ApodApiClient()
        val metClient = com.astroframe.tv.data.MetApiClient()
        val imageProcessor = ImageProcessor(context)

        Log.d(TAG, "Executing wallpaper update for source: ${prefs.artSource}...")

        val apod = when (prefs.artSource) {
            com.astroframe.tv.data.ArtSource.NASA_APOD -> {
                apiClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
            }
            com.astroframe.tv.data.ArtSource.THE_MET -> {
                metClient.fetchMasterpiece() ?: apiClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
            }
            com.astroframe.tv.data.ArtSource.MIXED -> {
                if (kotlin.random.Random.nextBoolean()) {
                    metClient.fetchMasterpiece() ?: apiClient.fetchApod(apiKey = apiKey, random = prefs.useRandom)
                } else {
                    apiClient.fetchApod(apiKey = apiKey, random = prefs.useRandom) ?: metClient.fetchMasterpiece()
                }
            }
        }

        if (apod == null) {
            Log.e(TAG, "Failed to retrieve artwork or astronomy photo")
            return@withContext Result.retry()
        }

        val savedFile = imageProcessor.processAndSaveWallpaper(
            apod = apod,
            position = prefs.overlayPosition
        )

        if (savedFile != null && savedFile.exists()) {
            prefs.lastTitle = apod.title
            prefs.lastDate = apod.date
            prefs.lastExplanation = apod.summaryExplanation
            prefs.lastUpdatedMs = System.currentTimeMillis()
            prefs.lastImagePath = savedFile.absolutePath

            Log.d(TAG, "Successfully updated wallpaper: ${apod.title} (${savedFile.absolutePath})")
            Result.success()
        } else {
            Log.e(TAG, "Failed to generate or save wallpaper bitmap")
            Result.retry()
        }
    }
}

