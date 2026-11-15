package com.astroframe.tv.data

import android.util.Log
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ApodApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ApodApiClient"
        // Official NASA Science WordPress JSON endpoint (powers science.nasa.gov/apod/)
        private const val SCIENCE_NASA_URL = "https://science.nasa.gov/wp-json/wp/v2/apod-basic"
        // Legacy endpoint fallback
        private const val LEGACY_NASA_URL = "https://api.nasa.gov/planetary/apod"
    }

    suspend fun fetchApod(apiKey: String, random: Boolean = true): ApodItem? = withContext(Dispatchers.IO) {
        // 1. Primary: Use the official science.nasa.gov endpoint
        val officialItem = fetchFromScienceNasa(random)
        if (officialItem != null) {
            Log.d(TAG, "Successfully fetched official APOD: ${officialItem.title} (${officialItem.date})")
            return@withContext officialItem
        }

        // 2. Fallback: Legacy api.nasa.gov endpoint
        Log.w(TAG, "Official science.nasa.gov failed, trying legacy api.nasa.gov fallback...")
        fetchFromLegacyApi(apiKey, random)
    }

    private fun fetchFromScienceNasa(random: Boolean): ApodItem? {
        val targetUrl = if (random) {
            val randomPage = Random.nextInt(1, 100)
            "$SCIENCE_NASA_URL?page=$randomPage&per_page=1"
        } else {
            "$SCIENCE_NASA_URL?per_page=1"
        }

        try {
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android TV; AstroFrame)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "science.nasa.gov returned ${response.code}")
                    return null
                }

                val body = response.body?.string() ?: return null
                val array = JSONArray(body)
                if (array.length() == 0) return null

                val obj = array.getJSONObject(0)
                return parseScienceNasaItem(obj)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed calling science.nasa.gov: ${e.message}")
            return null
        }
    }

    private fun parseScienceNasaItem(obj: JSONObject): ApodItem? {
        val hdUrl = obj.optString("hdurl", "").ifBlank { obj.optString("url", "") }
        // Ensure not a broken logo fallback
        if (hdUrl.isBlank() || hdUrl.contains("nasa-logo")) {
            return null
        }

        val rawTitle = obj.optString("title", "Astronomy Picture of the Day")
        val rawExplanation = obj.optString("explanation", "")
        val rawCredit = obj.optString("copyright").ifEmpty { obj.optString("credit", "") }

        return ApodItem(
            title = cleanHtml(rawTitle),
            explanation = cleanHtml(rawExplanation),
            date = obj.optString("date", ""),
            mediaType = obj.optString("media_type", "image"),
            url = hdUrl,
            hdUrl = hdUrl,
            copyright = cleanHtml(rawCredit).takeIf { it.isNotBlank() }
        )
    }

    private fun fetchFromLegacyApi(apiKey: String, random: Boolean): ApodItem? {
        val url = if (random) {
            "$LEGACY_NASA_URL?api_key=$apiKey&count=5"
        } else {
            "$LEGACY_NASA_URL?api_key=$apiKey"
        }

        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "AstroFrame-AndroidTV/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null

                if (random) {
                    val array = JSONArray(body)
                    var found: ApodItem? = null
                    for (i in 0 until array.length()) {
                        val item = parseLegacyItem(array.getJSONObject(i))
                        if (isValidImage(item)) {
                            found = item
                            break
                        }
                    }
                    found
                } else {
                    val item = parseLegacyItem(JSONObject(body))
                    if (isValidImage(item)) item else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Legacy API error", e)
            null
        }
    }

    private fun parseLegacyItem(obj: JSONObject): ApodItem {
        return ApodItem(
            title = obj.optString("title", "Astronomy Picture of the Day"),
            explanation = obj.optString("explanation", ""),
            date = obj.optString("date", ""),
            mediaType = obj.optString("media_type", "image"),
            url = obj.optString("url", ""),
            hdUrl = if (obj.has("hdurl") && !obj.isNull("hdurl")) obj.getString("hdurl") else null,
            copyright = if (obj.has("copyright") && !obj.isNull("copyright")) obj.getString("copyright") else null
        )
    }

    private fun isValidImage(item: ApodItem?): Boolean {
        if (item == null) return false
        if (item.mediaType != "image") return false
        val url = item.bestImageUrl
        return url.isNotBlank() && !url.contains("nasa-logo")
    }

    private fun cleanHtml(html: String): String {
        return try {
            val unescaped = HtmlCompat.fromHtml(
                html,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString()
            unescaped
                .replace("Explanation:", "")
                .replace("<strong>", "")
                .replace("</strong>", "")
                .replace("\n", " ")
                .trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "").replace("\n", " ").trim()
        }
    }
}
