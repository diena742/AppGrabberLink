package com.appgrabberlink.engine

import com.appgrabberlink.download.FormatInfo

object FormatSelector {

    fun selectBestFormat(formats: List<FormatInfo>): FormatInfo? {
        if (formats.isEmpty()) return null

        val scored = formats.map { format ->
            val score = calculateScore(format)
            format to score
        }

        return scored.maxByOrNull { it.second }?.first
    }

    fun selectFormatsByQuality(formats: List<FormatInfo>, quality: String): List<FormatInfo> {
        return when (quality) {
            "best" -> listOfNotNull(selectBestFormat(formats))
            "1080p" -> formats.filter { it.resolution?.contains("1080") == true }
            "720p" -> formats.filter { it.resolution?.contains("720") == true }
            "480p" -> formats.filter { it.resolution?.contains("480") == true }
            "audio" -> formats.filter { it.resolution == null && it.extension in listOf("m4a", "mp3", "aac") }
            else -> formats
        }
    }

    private fun calculateScore(format: FormatInfo): Int {
        var score = 0

        if (format.extension == "mp4") score += 50
        if (format.extension == "mkv") score += 40
        if (format.extension == "webm") score += 30

        val resolution = format.resolution
        if (resolution != null) {
            val height = resolution.substringAfter("x").toIntOrNull() ?: 0
            score += when {
                height >= 2160 -> 100
                height >= 1440 -> 80
                height >= 1080 -> 60
                height >= 720 -> 40
                height >= 480 -> 20
                else -> 10
            }
        }

        val sizeMb = format.filesize?.let { it / (1024 * 1024) } ?: 0
        if (sizeMb > 0 && sizeMb < 500) score += 10

        return score
    }

    fun summarizeFormat(format: FormatInfo): String {
        val parts = mutableListOf<String>()
        parts.add(format.extension)
        if (format.resolution != null) parts.add(format.resolution)
        if (format.note != null) parts.add(format.note)
        if (format.filesize != null) {
            val sizeMb = format.filesize / (1024 * 1024)
            parts.add("${sizeMb}MB")
        }
        return parts.joinToString(" - ")
    }
}
