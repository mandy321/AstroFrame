package com.astroframe.tv.analytics

import android.content.Context
import android.os.Build
import android.util.Log
import com.astroframe.tv.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

object AnalyticsTracker {

    private const val TAG = "AnalyticsTracker"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            val config = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST
            ).apply {
                captureApplicationLifecycleEvents = true
                captureScreenViews = true
                flushAt = 1 // Flush immediately for TV events
                flushIntervalSeconds = 15
            }
            PostHogAndroid.setup(context, config)

            // Register global super properties for every event
            PostHog.register("app_name", "AstroFrame")
            PostHog.register("app_version", BuildConfig.VERSION_NAME)
            PostHog.register("version_code", BuildConfig.VERSION_CODE)
            PostHog.register("device_brand", Build.BRAND)
            PostHog.register("device_model", Build.MODEL)
            PostHog.register("android_version", Build.VERSION.RELEASE)
            PostHog.register("android_sdk", Build.VERSION.SDK_INT)

            isInitialized = true
            Log.d(TAG, "PostHog analytics initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize PostHog analytics: ${e.message}")
        }
    }

    fun track(event: String, properties: Map<String, Any> = emptyMap()) {
        try {
            PostHog.capture(event, properties = properties)
            PostHog.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to capture event '$event': ${e.message}")
        }
    }

    fun trackAppOpen(source: String) {
        track("app_opened", mapOf("source" to source))
    }

    fun trackWallpaperUpdated(artSource: String, title: String, dateOrPeriod: String) {
        track(
            "wallpaper_updated",
            mapOf(
                "art_source" to artSource,
                "title" to title,
                "period" to dateOrPeriod
            )
        )
    }

    fun trackScreensaverStarted(rotationSpeedSec: Int) {
        track(
            "screensaver_started",
            mapOf(
                "rotation_speed_sec" to rotationSpeedSec,
                "mode" to if (rotationSpeedSec == 0) "static" else "rotating"
            )
        )
    }
}
