package com.astroframe.tv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EarthOrbitApiClient {

    companion object {
        // High-resolution astronaut ISS captures, glowing night city grids, atmospheric auroras & orbital views
        private val CURATED_EARTH_ORBIT_PHOTOS = listOf(
            ApodItem(
                title = "Aurora Australis from the ISS",
                explanation = "Spectacular green and red auroral curtains dancing above the southern hemisphere, photographed by Expedition 65 crew.",
                date = "Earth from Space",
                mediaType = "image",
                url = "https://images-assets.nasa.gov/image/iss065e094406/iss065e094406~orig.jpg",
                hdUrl = "https://images-assets.nasa.gov/image/iss065e094406/iss065e094406~orig.jpg",
                copyright = "NASA Astronaut Photography",
                sourceBadge = "✦ ISS ASTRONAUT VIEW",
                sourceColor = "#4DD0E1"
            ),
            ApodItem(
                title = "City Lights of Paris and Western Europe",
                explanation = "Illuminated nighttime street grid of Paris and neighboring cities, captured 250 miles above the Earth by the ISS cupola.",
                date = "Earth from Space",
                mediaType = "image",
                url = "https://images-assets.nasa.gov/image/iss043e093251/iss043e093251~orig.jpg",
                hdUrl = "https://images-assets.nasa.gov/image/iss043e093251/iss043e093251~orig.jpg",
                copyright = "NASA Earth Observatory",
                sourceBadge = "✦ ISS ASTRONAUT VIEW",
                sourceColor = "#4DD0E1"
            ),
            ApodItem(
                title = "The Nile River Delta at Night",
                explanation = "Brilliant ribbon of population following the Nile River to the Mediterranean coast.",
                date = "Earth from Space",
                mediaType = "image",
                url = "https://images-assets.nasa.gov/image/iss025e009840/iss025e009840~orig.jpg",
                hdUrl = "https://images-assets.nasa.gov/image/iss025e009840/iss025e009840~orig.jpg",
                copyright = "NASA Astronaut Photography",
                sourceBadge = "✦ ISS ASTRONAUT VIEW",
                sourceColor = "#4DD0E1"
            ),
            ApodItem(
                title = "Eye of Typhoon Trami",
                explanation = "Alexander Gerst's iconic astronaut view peering straight down into the vortex eye of a super-typhoon over the Pacific.",
                date = "Earth from Space",
                mediaType = "image",
                url = "https://images-assets.nasa.gov/image/iss056e180844/iss056e180844~orig.jpg",
                hdUrl = "https://images-assets.nasa.gov/image/iss056e180844/iss056e180844~orig.jpg",
                copyright = "ESA / NASA",
                sourceBadge = "✦ ISS ASTRONAUT VIEW",
                sourceColor = "#4DD0E1"
            ),
            ApodItem(
                title = "Atmospheric Limb and Sunburst",
                explanation = "The fragile blue haze of Earth's atmosphere backlit by the rising Sun over the Pacific Ocean.",
                date = "Earth from Space",
                mediaType = "image",
                url = "https://images-assets.nasa.gov/image/iss047e008453/iss047e008453~orig.jpg",
                hdUrl = "https://images-assets.nasa.gov/image/iss047e008453/iss047e008453~orig.jpg",
                copyright = "NASA Astronaut Photography",
                sourceBadge = "✦ ISS ASTRONAUT VIEW",
                sourceColor = "#4DD0E1"
            )
        )
    }

    suspend fun fetchEarthOrbitPhoto(): ApodItem = withContext(Dispatchers.IO) {
        CURATED_EARTH_ORBIT_PHOTOS.random()
    }
}
