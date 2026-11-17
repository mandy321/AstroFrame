package com.astroframe.tv.data

enum class ArtSource(
    val label: String,
    val badge: String,
    val accentColor: String,
    val description: String
) {
    NASA_COSMOS(
        "🚀 NASA Cosmos & Webb",
        "✦ NASA APOD & JWST",
        "#64B5F6",
        "Deep-space nebulas, James Webb infrared observations & Hubble galaxies."
    ),
    WORLD_MASTERPIECES(
        "🎨 World Museum Masterpieces",
        "✦ WORLD MASTERPIECE",
        "#FFD54F",
        "Iconic paintings from The Met, Rijksmuseum & Art Institute of Chicago."
    ),
    EARTH_ORBIT(
        "🌍 Earth from Orbit & ISS",
        "✦ ISS ASTRONAUT VIEW",
        "#4DD0E1",
        "Breathtaking 4K astronaut photography, glowing night cities & auroras."
    ),
    CUSTOM_USER_FOLDER(
        "📁 Custom Personal Photos & Art",
        "✦ USER GALLERY",
        "#81C784",
        "Stream your personal photos, family albums or 4K art from device storage or USB."
    ),
    ULTIMATE_MIX(
        "🌌 The Ultimate Canvas (Mix All)",
        "✦ CURATED MIX",
        "#BA68C8",
        "A continuous intelligent shuffle across space, fine art, Earth & user galleries."
    );

    fun next(): ArtSource {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }
}
