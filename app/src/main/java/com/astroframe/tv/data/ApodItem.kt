package com.astroframe.tv.data

data class ApodItem(
    val title: String,
    val explanation: String,
    val date: String,
    val mediaType: String,
    val url: String,
    val hdUrl: String?,
    val copyright: String? = null,
    val sourceBadge: String = "✦ NASA APOD",
    val sourceColor: String = "#64B5F6"
) {
    val bestImageUrl: String
        get() = hdUrl?.takeIf { it.isNotBlank() } ?: url

    val summaryExplanation: String
        get() {
            val clean = explanation.replace("\n", " ").trim()
            val sentences = clean.split(". ")
            return when {
                sentences.size >= 2 -> "${sentences[0]}. ${sentences[1]}."
                clean.length > 180 -> clean.substring(0, 177) + "..."
                else -> clean
            }
        }
}

