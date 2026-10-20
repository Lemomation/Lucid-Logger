package com.lemomation.lucidlogger.util

import java.text.Normalizer
import java.util.Locale

object TextUtils {
    const val VOICE_PLACEHOLDER = "_Voice memo recorded. Add details in the morning._"

    private val AUDIO_EMBED = Regex("!\\[\\[[^]]+]]")
    private val IMAGE_EMBED = Regex("!\\[\\[[^]]+]]")
    private val WIKILINK = Regex("\\[\\[([^]|#]+)(?:\\|([^]]+))?]]")
    private val INLINE_TAG = Regex("(?<![\\w#])#([A-Za-z0-9_/-]+)")
    private val HEADING = Regex("^#{1,6}\\s+", RegexOption.MULTILINE)
    private val MARKDOWN_SYMBOLS = Regex("[*_>`~]")
    private val MULTI_SPACE = Regex("\\s+")

    fun writtenCharCount(text: String): Int {
        val cleaned = text
            .replace(AUDIO_EMBED, " ")
            .replace(VOICE_PLACEHOLDER, " ")
        return cleaned.count { !it.isWhitespace() }
    }

    fun hasWrittenText(body: String): Boolean = writtenCharCount(body) >= 8

    fun plainExcerpt(body: String, maxChars: Int = 220): String {
        val text = body
            .replace(AUDIO_EMBED, " ")
            .replace(VOICE_PLACEHOLDER, " ")
            .replace(WIKILINK) { match ->
                match.groupValues[2].ifBlank { match.groupValues[1] }
            }
            .replace(HEADING, "")
            .replace(INLINE_TAG) { it.groupValues[1] }
            .replace(MARKDOWN_SYMBOLS, "")
            .replace(MULTI_SPACE, " ")
            .trim()
        if (text.length <= maxChars) return text
        val cut = text.take(maxChars)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > maxChars * 0.65) cut.substring(0, lastSpace) else cut).trimEnd() + "..."
    }

    fun firstLine(text: String, maxChars: Int = 80): String {
        val first = text.lineSequence()
            .map { it.replace(WIKILINK) { m -> m.groupValues[2].ifBlank { m.groupValues[1] } } }
            .map { it.replace(Regex("^#{1,6}\\s+"), "").replace(MARKDOWN_SYMBOLS, "").trim() }
            .firstOrNull { it.isNotBlank() }
            ?: ""
        return if (first.length > maxChars) first.take(maxChars).trimEnd() + "..." else first
    }

    fun autoTitle(body: String, fallback: String): String {
        val line = firstLine(body)
        return if (line.isBlank()) fallback else line.take(64).trimEnd()
    }

    fun slugify(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(48)
            .trim('-')
        return normalized.ifBlank { "dream" }
    }

    fun formatDuration(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return if (s >= 60) "%d:%02d".format(s / 60, s % 60) else "0:%02d".format(s)
    }

    fun formatRemaining(millis: Long): String {
        val totalMinutes = (millis / 60000).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "In $hours hr${if (hours == 1L) "" else "s"} $minutes min"
            hours > 0 -> "In $hours hr${if (hours == 1L) "" else "s"}"
            else -> "In $minutes min"
        }
    }

    fun normalizeLinkTarget(raw: String): String = raw.substringBefore('|').substringBefore('#').trim()

    fun isAttachment(target: String): Boolean {
        val lower = target.lowercase(Locale.US)
        return lower.startsWith("audio/") || lower.startsWith("_audio/") ||
            ATTACHMENT_EXTENSIONS.any { lower.endsWith(it) }
    }

    private val ATTACHMENT_EXTENSIONS = listOf(
        ".m4a", ".mp3", ".wav", ".ogg", ".aac", ".flac",
        ".png", ".jpg", ".jpeg", ".webp", ".gif", ".pdf",
    )
}
