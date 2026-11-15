package com.astroframe.tv

import android.app.Application
import android.util.Log
import com.astroframe.tv.analytics.AnalyticsTracker
import com.astroframe.tv.data.PreferencesManager
import com.astroframe.tv.worker.ApodWallpaperWorker

class AstroApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PostHog analytics for live user & install tracking
        AnalyticsTracker.init(this)

        val prefs = PreferencesManager(this)

        // Schedule periodic wallpaper worker on app startup if not scheduled
        ApodWallpaperWorker.schedulePeriodic(
            context = this,
            intervalMinutes = prefs.intervalMinutes,
            forceReplace = false
        )
        Log.d("AstroApplication", "AstroFrame application started. Worker active.")
    }
}

