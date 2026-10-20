package com.lemomation.lucidlogger.data.markdown

import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import com.lemomation.lucidlogger.util.TextUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MarkdownCodec {
    private val TAG_REGEX = Regex("(?<![\\w#])#([A-Za-z0-9_/-]+)")
    private val WIKILINK_REGEX = Regex("\\[\\[([^]]+)]]")
    private val AUDIO_EMBED_REGEX = Regex("!\\[\\[([^]]+\\.(?:m4a|mp3|wav|ogg|aac))]]", RegexOption.IGNORE_CASE)
    private val HEADING_REGEX = Regex("^#\\s+(.+)$")
    private val FILE_TIME_REGEX = Regex("(\\d{4}-\\d{2}-\\d{2})[_-](\\d{2})(\\d{2})")

    fun encode(note: DreamNote): String {
        val title = note.title.ifBlank { "Dream" }
        val tags = note.tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct()
        val waveform = note.waveform.take(64).joinToString(", ")
        return buildString {
            appendLine("---")
            appendLine("id: ${quote(note.id)}")
            appendLine("title: ${quote(title)}")
            appendLine("date: ${note.created.toLocalDate()}")
            appendLine("time: ${quote(note.created.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))}")
            appendLine("created: ${quote(note.created.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))}")
            note.updated?.let {
                appendLine("updated: ${quote(it.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))}")
            }
            appendLine("tags: [${tags.joinToString(", ") { quote(it) }}]")
            appendLine("lucid: ${note.lucid}")
            note.audioFile?.let { appendLine("audio: ${quote(it)}") }
            note.audioDuration?.let { appendLine("audio_duration: $it") }
            if (waveform.isNotBlank()) appendLine("waveform: [$waveform]")
            appendLine("source: ${note.source.name.lowercase()}")
            appendLine("schema: 1")
            appendLine("---")
            appendLine()
            appendLine("# $title")
            appendLine()
            append(note.body.trim())
            if (!endsWith("\n")) appendLine()
            if (note.audioFile != null && !note.body.contains(note.audioFile)) {
                appendLine()
                appendLine("![[${note.audioFile}]]")
            }
        }
    }

    fun decode(text: String, fileName: String): DreamNote? {
        val normalized = text.replace("\r\n", "\n")
        val (frontmatter, rawBody) = splitFrontmatter(normalized)
        val fallback = createdAtFromFileName(fileName)
        val created = parseCreated(frontmatter, fallback) ?: return null
        val bodyWithoutHeading = stripTitleHeading(rawBody)
        val title = frontmatter["title"].orEmpty().ifBlank {
            rawBody.lineSequence().firstOrNull { it.startsWith("# ") }
                ?.removePrefix("# ")?.trim().orEmpty()
        }.ifBlank { "Dream at ${created.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}" }

        val inlineTags = TAG_REGEX.findAll(rawBody)
            .map { it.groupValues[1].lowercase() }
            .toList()
        val frontmatterTags = parseList(frontmatter["tags"]).map { it.lowercase() }
        val tags = (frontmatterTags + inlineTags).distinct().filter { it.isNotBlank() }

        val links = WIKILINK_REGEX.findAll(rawBody)
            .map { TextUtils.normalizeLinkTarget(it.groupValues[1]) }
            .filter { it.isNotBlank() && !TextUtils.isAttachment(it) }
            .distinct()
            .toList()

        val audioFile = frontmatter["audio"]?.let(::unquote)?.takeIf { it.isNotBlank() }
            ?: AUDIO_EMBED_REGEX.find(rawBody)?.groupValues?.get(1)
        val audioDuration = frontmatter["audio_duration"]?.toIntOrNull()
        val waveform = parseList(frontmatter["waveform"]).mapNotNull { it.toIntOrNull() }
        val lucid = frontmatter["lucid"]?.toBooleanStrictOrNull() == true || tags.contains("lucid")
        val source = when (frontmatter["source"]?.lowercase()) {
            "voice" -> DreamSource.VOICE
            "mixed", "voice_note" -> DreamSource.MIXED
            "note" -> DreamSource.NOTE
            else -> when {
                audioFile != null && TextUtils.hasWrittenText(bodyWithoutHeading) -> DreamSource.MIXED
                audioFile != null -> DreamSource.VOICE
                else -> DreamSource.NOTE
            }
        }
        val updated = frontmatter["updated"]?.let { runCatching { OffsetDateTime.parse(unquote(it)) }.getOrNull() }

        return DreamNote(
            id = frontmatter["id"]?.let(::unquote)?.ifBlank { null } ?: fileName.removeSuffix(".md"),
            fileName = fileName,
            title = unquote(title),
            body = bodyWithoutHeading.trim(),
            created = created,
            updated = updated,
            tags = tags,
            links = links,
            audioFile = audioFile,
            audioDuration = audioDuration,
            waveform = waveform,
            lucid = lucid,
            source = source,
        )
    }

    data class NoteMetadata(
        val tags: List<String>,
        val links: List<String>,
        val lucid: Boolean,
    )

    fun extractMetadata(body: String): NoteMetadata {
        val tags = TAG_REGEX.findAll(body)
            .map { it.groupValues[1].lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        val links = WIKILINK_REGEX.findAll(body)
            .map { TextUtils.normalizeLinkTarget(it.groupValues[1]) }
            .filter { it.isNotBlank() && !TextUtils.isAttachment(it) }
            .distinct()
            .toList()
        return NoteMetadata(tags = tags, links = links, lucid = tags.contains("lucid"))
    }

    fun rewriteTitle(text: String, title: String): String {
        val normalized = text.replace("\r\n", "\n")
        val (frontmatter, body) = splitFrontmatter(normalized)
        val lines = body.lines().toMutableList()
        val headingIndex = lines.indexOfFirst { it.startsWith("# ") }
        if (headingIndex >= 0) lines[headingIndex] = "# $title" else lines.add(0, "# $title")
        val fm = LinkedHashMap(frontmatter)
        fm["title"] = quote(title)
        val fmText = fm.entries.joinToString("\n") { (k, v) -> "$k: $v" }
        return "---\n$fmText\n---\n\n" + lines.joinToString("\n").trimStart('\n')
    }

    private fun splitFrontmatter(text: String): Pair<Map<String, String>, String> {
        if (!text.startsWith("---\n")) return emptyMap<String, String>() to text
        val end = text.indexOf("\n---", startIndex = 4)
        if (end < 0) return emptyMap<String, String>() to text
        val block = text.substring(4, end)
        val bodyStart = text.indexOf('\n', end + 1)
        val body = if (bodyStart >= 0) text.substring(bodyStart + 1) else ""
        val map = LinkedHashMap<String, String>()
        block.lineSequence().forEach { line ->
            val idx = line.indexOf(':')
            if (idx <= 0) return@forEach
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim()
            if (key.isNotBlank() && !key.startsWith("#")) map[key] = value
        }
        return map to body
    }

    private fun stripTitleHeading(body: String): String {
        val trimmed = body.trimStart('\n')
        val lines = trimmed.lines().toMutableList()
        val firstNonBlank = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlank >= 0 && HEADING_REGEX.matches(lines[firstNonBlank].trim())) {
            lines.removeAt(firstNonBlank)
            while (firstNonBlank < lines.size && lines[firstNonBlank].isBlank()) {
                lines.removeAt(firstNonBlank)
            }
        }
        return lines.joinToString("\n").trim()
    }

    private fun parseCreated(fm: Map<String, String>, fallback: OffsetDateTime?): OffsetDateTime? {
        fm["created"]?.let { raw ->
            runCatching { OffsetDateTime.parse(unquote(raw)) }.getOrNull()?.let { return it }
        }
        val date = fm["date"]?.let(::unquote)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: fallback?.toLocalDate()
        val time = fm["time"]?.let(::unquote)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: fallback?.toLocalTime()
        if (date == null || time == null) return fallback
        return date.atTime(time).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    }

    private fun createdAtFromFileName(fileName: String): OffsetDateTime? {
        val match = FILE_TIME_REGEX.find(fileName) ?: return null
        val date = runCatching { LocalDate.parse(match.groupValues[1]) }.getOrNull() ?: return null
        val hour = match.groupValues[2].toIntOrNull() ?: return null
        val minute = match.groupValues[3].toIntOrNull() ?: return null
        return date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    }

    private fun parseList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val content = raw.trim().removePrefix("[").removeSuffix("]")
        if (content.isBlank()) return emptyList()
        return content.split(',').map { unquote(it.trim()) }.filter { it.isNotBlank() }
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun unquote(value: String): String {
        val trimmed = value.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length - 1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        return trimmed
    }
}
