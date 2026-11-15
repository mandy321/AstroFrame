package com.astroframe.tv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApodApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ApodApiClient"
        private const val BASE_URL = "https://api.nasa.gov/planetary/apod"
    }

    suspend fun fetchApod(apiKey: String, random: Boolean = true): ApodItem? = withContext(Dispatchers.IO) {
        val url = if (random) {
            // Request 3 items in case some are videos
            "$BASE_URL?api_key=$apiKey&count=3"
        } else {
            "$BASE_URL?api_key=$apiKey"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "AstroFrame-AndroidTV/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null

                if (random) {
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val item = parseJsonObject(obj)
                        if (item.mediaType == "image" && item.bestImageUrl.isNotBlank()) {
                            return@withContext item
                        }
                    }
                    null
                } else {
                    val obj = JSONObject(body)
                    val item = parseJsonObject(obj)
                    if (item.mediaType == "image") item else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch APOD", e)
            null
        }
    }

    private fun parseJsonObject(obj: JSONObject): ApodItem {
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
}

