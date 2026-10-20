package com.lemomation.lucidlogger.data.markdown

object WikiLinker {
    private val PROTECTED = Regex(
        "!?\\[\\[[^]]+]]|```[\\s\\S]*?```|`[^`]*`|https?://\\S+",
    )

    fun autoLink(body: String, entities: Collection<String>): String {
        val names = entities
            .map { it.trim() }
            .filter { it.length >= 3 && it.none { c -> c == '[' || c == ']' } }
            .distinctBy { it.lowercase() }
            .sortedByDescending { it.length }
        if (names.isEmpty()) return body

        val out = StringBuilder()
        var last = 0
        PROTECTED.findAll(body).forEach { match ->
            out.append(linkPlain(body.substring(last, match.range.first), names))
            out.append(match.value)
            last = match.range.last + 1
        }
        out.append(linkPlain(body.substring(last), names))
        return out.toString()
    }

    private fun linkPlain(segment: String, names: List<String>): String {
        if (segment.isBlank()) return segment
        data class Span(val start: Int, val end: Int, val canonical: String)
        val spans = mutableListOf<Span>()
        names.forEach { name ->
            val pattern = Regex(
                "(?<![\\p{L}\\p{N}_\\[])" + Regex.escape(name) +
                    "(?![\\p{L}\\p{N}_\\]])",
                RegexOption.IGNORE_CASE,
            )
            val match = pattern.find(segment) ?: return@forEach
            if (spans.none { match.range.first < it.end && match.range.last >= it.start }) {
                spans += Span(match.range.first, match.range.last + 1, name)
            }
        }
        if (spans.isEmpty()) return segment
        val result = StringBuilder(segment)
        spans.sortedByDescending { it.start }.forEach { span ->
            result.replace(span.start, span.end, "[[${span.canonical}]]")
        }
        return result.toString()
    }
}
