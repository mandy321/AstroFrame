package com.astroframe.tv.data

import android.content.Context
import android.content.SharedPreferences
import com.astroframe.tv.BuildConfig

enum class MatStyle(val label: String, val paddingDp: Int) {
    NONE("Bezel-to-Bezel (Full Screen)", 0),
    SLIM_MODERN("Modern Slim Mat (24dp)", 24),
    MUSEUM_CLASSIC("Museum Classic Bevel (48dp)", 48),
    GALLERY_DEEP("Gallery Deep Shadowbox (72dp)", 72);

    fun next(): MatStyle {
        val values = entries.toTypedArray()
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}

enum class OverlayPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    NONE("Hidden / Off");

    fun next(): OverlayPosition {
        val values = entries.toTypedArray()
        val nextIndex = (ordinal + 1) % values.size
        return values[nextIndex]
    }
}

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("astroframe_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_INTERVAL_MINUTES = "interval_minutes"
        private const val KEY_OVERLAY_POSITION = "overlay_position"
        private const val KEY_USE_RANDOM = "use_random"
        private const val KEY_LAST_TITLE = "last_title"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_EXPLANATION = "last_explanation"
        private const val KEY_LAST_UPDATED_MS = "last_updated_ms"
        private const val KEY_LAST_IMAGE_PATH = "last_image_path"
        private const val KEY_ART_SOURCE = "art_source"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_SCREENSAVER_ROTATION = "screensaver_rotation_sec"
        private const val KEY_OLED_CARE_ENABLED = "oled_care_enabled"
        private const val KEY_MAT_STYLE = "mat_style"
        private const val KEY_COLOR_GAMUT_10BIT = "color_gamut_10bit"

        val INTERVAL_OPTIONS = listOf(15, 30, 60, 180, 360, 1440)
        val SCREENSAVER_ROTATION_OPTIONS = listOf(0, 30, 60, 120, 300)
    }

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, BuildConfig.NASA_API_KEY) ?: BuildConfig.NASA_API_KEY
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var intervalMinutes: Int
        get() = prefs.getInt(KEY_INTERVAL_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_INTERVAL_MINUTES, value).apply()

    var overlayPosition: OverlayPosition
        get() {
            val name = prefs.getString(KEY_OVERLAY_POSITION, OverlayPosition.TOP_LEFT.name)
            return try {
                OverlayPosition.valueOf(name ?: OverlayPosition.TOP_LEFT.name)
            } catch (e: Exception) {
                OverlayPosition.TOP_LEFT
            }
        }
        set(value) = prefs.edit().putString(KEY_OVERLAY_POSITION, value.name).apply()

    var useRandom: Boolean
        get() = prefs.getBoolean(KEY_USE_RANDOM, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_RANDOM, value).apply()

    var lastTitle: String
        get() = prefs.getString(KEY_LAST_TITLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_TITLE, value).apply()

    var lastDate: String
        get() = prefs.getString(KEY_LAST_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_DATE, value).apply()

    var lastExplanation: String
        get() = prefs.getString(KEY_LAST_EXPLANATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_EXPLANATION, value).apply()

    var lastUpdatedMs: Long
        get() = prefs.getLong(KEY_LAST_UPDATED_MS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED_MS, value).apply()

    var lastImagePath: String
        get() = prefs.getString(KEY_LAST_IMAGE_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_IMAGE_PATH, value).apply()

    var artSource: ArtSource
        get() {
            val name = prefs.getString(KEY_ART_SOURCE, ArtSource.NASA_COSMOS.name)
            return try {
                ArtSource.valueOf(name ?: ArtSource.NASA_COSMOS.name)
            } catch (e: Exception) {
                ArtSource.NASA_COSMOS
            }
        }
        set(value) = prefs.edit().putString(KEY_ART_SOURCE, value.name).apply()

    var oledCareEnabled: Boolean
        get() = prefs.getBoolean(KEY_OLED_CARE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OLED_CARE_ENABLED, value).apply()

    var matStyle: MatStyle
        get() {
            val name = prefs.getString(KEY_MAT_STYLE, MatStyle.NONE.name)
            return try {
                MatStyle.valueOf(name ?: MatStyle.NONE.name)
            } catch (e: Exception) {
                MatStyle.NONE
            }
        }
        set(value) = prefs.edit().putString(KEY_MAT_STYLE, value.name).apply()

    var wideColorGamut10Bit: Boolean
        get() = prefs.getBoolean(KEY_COLOR_GAMUT_10BIT, true)
        set(value) = prefs.edit().putBoolean(KEY_COLOR_GAMUT_10BIT, value).apply()

    var screensaverRotationSeconds: Int
        get() = prefs.getInt(KEY_SCREENSAVER_ROTATION, 60)
        set(value) = prefs.edit().putInt(KEY_SCREENSAVER_ROTATION, value).apply()

    var customApiUrl: String
        get() = prefs.getString(KEY_CUSTOM_API_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_URL, value.trim()).apply()
}
