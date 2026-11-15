package com.astroframe.tv.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MetApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "MetApiClient"
        private const val OBJECT_URL = "https://collectionapi.metmuseum.org/public/collection/v1/objects"
        private const val SEARCH_URL = "https://collectionapi.metmuseum.org/public/collection/v1.1/search"

        // Curated iconic masterpieces from The Met that are guaranteed public domain with 4K scans:
        // Van Gogh, Monet, Rembrandt, Vermeer, Hokusai, Cézanne, Degas, Renoir, Velázquez, El Greco, etc.
        private val CURATED_MASTERPIECE_IDS = listOf(
            436535, // Van Gogh: Wheat Field with Cypresses
            436532, // Van Gogh: Self-Portrait with a Straw Hat
            436528, // Van Gogh: Irises
            437980, // Monet: Water Lilies
            437984, // Monet: Bridge over a Pond of Water Lilies
            437998, // Monet: The Four Trees
            437397, // Rembrandt: Aristotle with a Bust of Homer
            437394, // Rembrandt: Self-Portrait
            437881, // Vermeer: Young Woman with a Water Pitcher
            45434,  // Hokusai: The Great Wave off Kanagawa
            435882, // Cézanne: Mont Sainte-Victoire
            435876, // Cézanne: The Card Players
            436105, // Degas: The Dance Class
            437430, // Renoir: By the Seashore
            437877, // Velázquez: Juan de Pareja
            436573, // El Greco: View of Toledo
            438821, // Turner: The Grand Canal, Venice
            438822, // Turner: Venice, from the Porch of Madonna della Salute
            437654, // Seurat: Circus Sideshow
            436575  // El Greco: The Opening of the Fifth Seal
        )
    }

    suspend fun fetchMasterpiece(): ApodItem? = withContext(Dispatchers.IO) {
        // Try random curated ID first for ultra-fast, guaranteed stunning 4K scans
        val randomCuratedId = CURATED_MASTERPIECE_IDS.random()
        val curatedItem = fetchObject(randomCuratedId)
        if (curatedItem != null) {
            return@withContext curatedItem
        }

        // Fallback: search open collection
        fetchFromCollectionSearch()
    }

    private fun fetchObject(objectId: Int): ApodItem? {
        val url = "$OBJECT_URL/$objectId"
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android TV; AstroFrame)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val obj = JSONObject(body)

                val image = obj.optString("primaryImage", "")
                if (image.isBlank()) return null

                val title = obj.optString("title", "Masterpiece")
                val artist = obj.optString("artistDisplayName", "Unknown Artist").ifBlank { "Unknown Artist" }
                val date = obj.optString("objectDate", "Fine Art")
                val medium = obj.optString("medium", "Artwork")
                val department = obj.optString("department", "The Met Collection")

                val desc = "$artist ($date). $medium. Department of $department."

                return ApodItem(
                    title = title,
                    explanation = desc,
                    date = date,
                    mediaType = "image",
                    url = image,
                    hdUrl = image,
                    copyright = "The Metropolitan Museum of Art, New York",
                    sourceBadge = "✦ THE MET MUSEUM",
                    sourceColor = "#FFD54F" // Amber / Museum Gold
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Met object $objectId: ${e.message}")
            return null
        }
    }

    private fun fetchFromCollectionSearch(): ApodItem? {
        val offset = Random.nextInt(0, 100)
        val url = "$SEARCH_URL?isHighlight=true&hasImages=true&q=paintings&offset=$offset&limit=3"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android TV; AstroFrame)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)
                val objectIds = json.optJSONArray("objectIDs") ?: return null

                for (i in 0 until objectIds.length()) {
                    val id = objectIds.getInt(i)
                    val item = fetchObject(id)
                    if (item != null) return item
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search Met collection failed: ${e.message}")
        }
        return null
    }
}

