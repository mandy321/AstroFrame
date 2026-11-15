package com.astroframe.tv.data

enum class ArtSource(val label: String, val badge: String, val accentColor: String) {
    NASA_APOD("NASA Cosmos", "✦ NASA APOD", "#64B5F6"),
    THE_MET("The Met Museum", "✦ THE MET MUSEUM", "#FFD54F"),
    MIXED("Cosmos + Museum Mix", "✦ ART & SPACE", "#81C784");

    fun next(): ArtSource {
        val values = entries.toTypedArray()
        return values[(ordinal + 1) % values.size]
    }
}

