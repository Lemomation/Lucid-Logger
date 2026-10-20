package com.lemomation.lucidlogger

import com.lemomation.lucidlogger.data.markdown.MarkdownCodec
import com.lemomation.lucidlogger.data.model.DreamNote
import com.lemomation.lucidlogger.data.model.DreamSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class MarkdownCodecTest {
    private fun note(): DreamNote = DreamNote(
        id = "2026-10-20_0345_the-train",
        fileName = "2026-10-20_0345_the-train.md",
        title = "The train that became a river",
        body = "I boarded a [[night train]] beside the [[ocean]]. A #lucid feeling arrived.\n\n![[audio/2026-10-20_0345_the-train.m4a]]",
        created = OffsetDateTime.parse("2026-10-20T03:45:00+02:00"),
        updated = OffsetDateTime.parse("2026-10-20T08:15:00+02:00"),
        tags = listOf("lucid"),
        links = listOf("night train", "ocean"),
        audioFile = "audio/2026-10-20_0345_the-train.m4a",
        audioDuration = 57,
        waveform = listOf(12, 20, 55, 91),
        lucid = true,
        source = DreamSource.MIXED,
    )

    @Test
    fun roundTripPreservesDream() {
        val original = note()
        val encoded = MarkdownCodec.encode(original)
        val decoded = MarkdownCodec.decode(encoded, original.fileName)

        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(original.id, decoded.id)
        assertEquals(original.title, decoded.title)
        assertEquals(original.body, decoded.body)
        assertEquals(original.created.toInstant(), decoded.created.toInstant())
        assertEquals(original.audioFile, decoded.audioFile)
        assertEquals(original.audioDuration, decoded.audioDuration)
        assertEquals(original.waveform, decoded.waveform)
        assertEquals(DreamSource.MIXED, decoded.source)
        assertTrue(decoded.lucid)
        assertEquals(listOf("lucid"), decoded.tags)
        assertEquals(listOf("night train", "ocean"), decoded.links)
    }

    @Test
    fun parseVoiceOnlyLegacyNote() {
        val text = """
            ---
            date: 2026-10-19
            time: "05:12"
            audio: "audio/voice-note.m4a"
            source: voice
            ---

            # Dream at 05:12

            _Voice memo recorded. Add details in the morning._

            ![[audio/voice-note.m4a]]
        """.trimIndent()

        val decoded = MarkdownCodec.decode(text, "2026-10-19_0512_dream.md")
        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(DreamSource.VOICE, decoded.source)
        assertEquals("audio/voice-note.m4a", decoded.audioFile)
        assertTrue(decoded.body.contains("_Voice memo recorded"))
    }

    @Test
    fun extractsMetadataFromMarkdown() {
        val meta = MarkdownCodec.extractMetadata(
            "The #ocean returned. #lucid and [[my mother]] was there beside [[the tower]].",
        )
        assertEquals(listOf("ocean", "lucid"), meta.tags)
        assertEquals(listOf("my mother", "the tower"), meta.links)
        assertTrue(meta.lucid)
    }
}
