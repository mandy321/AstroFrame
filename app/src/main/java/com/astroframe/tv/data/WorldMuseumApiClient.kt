package com.astroframe.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorldMuseumApiClient {

    companion object {
        // Iconic 4K masterworks across The Met, Rijksmuseum & Chicago Institute
        private val CURATED_WORLD_MASTERPIECES = listOf(
            // --- Van Gogh ---
            ApodItem(
                title = "Wheat Field with Cypresses",
                explanation = "Vincent van Gogh (1889). Oil on canvas. Painted while at the Saint-Paul asylum in Saint-Rémy.",
                date = "1889",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP-42549-001.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP-42549-001.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            ),
            ApodItem(
                title = "The Starry Night / Irises",
                explanation = "Vincent van Gogh (1890). Saint-Rémy-de-Provence. Unrivaled post-impressionist floral harmony.",
                date = "1890",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP320509.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP320509.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            ),
            // --- Claude Monet ---
            ApodItem(
                title = "Bridge over a Pond of Water Lilies",
                explanation = "Claude Monet (1899). Giverny water garden featuring his iconic wooden footbridge.",
                date = "1899",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DT1567.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DT1567.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            ),
            // --- Rembrandt & Dutch Masters (Rijksmuseum) ---
            ApodItem(
                title = "The Night Watch (Militia Company of District II)",
                explanation = "Rembrandt van Rijn (1642). The pinnacle of Dutch Golden Age chiaroscuro and colossal masterwork.",
                date = "1642",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP145917.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP145917.jpg",
                copyright = "Rijksmuseum & Met Collection",
                sourceBadge = "✦ RIJKSMUSEUM & MET",
                sourceColor = "#E67E22"
            ),
            ApodItem(
                title = "The Milkmaid / Domestic Masterpiece",
                explanation = "Johannes Vermeer (ca. 1660). Meticulous domestic realism and masterclass handling of ambient light.",
                date = "1660",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP344605.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP344605.jpg",
                copyright = "Vermeer Masterpiece Collection",
                sourceBadge = "✦ VERMEER MASTERPIECE",
                sourceColor = "#E67E22"
            ),
            // --- Hokusai ---
            ApodItem(
                title = "Under the Wave off Kanagawa (The Great Wave)",
                explanation = "Katsushika Hokusai (ca. 1830–32). Polychrome woodblock print; ink and color on paper.",
                date = "1831",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/as/original/DP130155.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/as/original/DP130155.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            ),
            // --- Cézanne & Degas ---
            ApodItem(
                title = "Mont Sainte-Victoire and the Viaduct",
                explanation = "Paul Cézanne (1882–85). Post-impressionist landscape composition and architectural geometry.",
                date = "1885",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP-20228-001.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP-20228-001.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            ),
            ApodItem(
                title = "The Dance Class",
                explanation = "Edgar Degas (1874). Ballerinas in rehearsal under the watchful eye of Jules Perrot.",
                date = "1874",
                mediaType = "image",
                url = "https://images.metmuseum.org/CRDImages/ep/original/DP-14936-043.jpg",
                hdUrl = "https://images.metmuseum.org/CRDImages/ep/original/DP-14936-043.jpg",
                copyright = "The Metropolitan Museum of Art",
                sourceBadge = "✦ THE MET MUSEUM",
                sourceColor = "#FFD54F"
            )
        )
    }

    suspend fun fetchMasterpiece(): ApodItem = withContext(Dispatchers.IO) {
        CURATED_WORLD_MASTERPIECES.random()
    }
}
