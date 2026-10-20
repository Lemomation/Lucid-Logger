package com.lemomation.lucidlogger

import com.lemomation.lucidlogger.util.TextUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {
    @Test
    fun ignoresAudioEmbedWhenCountingCharacters() {
        val body = "_Voice memo recorded. Add details in the morning._\n\n![[audio/dream.m4a]]"
        assertEquals(0, TextUtils.writtenCharCount(body))
        assertFalse(TextUtils.hasWrittenText(body))
    }

    @Test
    fun countsOnlyRealDreamText() {
        assertTrue(TextUtils.hasWrittenText("I was flying above a dark ocean."))
        assertEquals(26, TextUtils.writtenCharCount("I was flying above a dark ocean."))
    }

    @Test
    fun slugifyIsSafeForFileNames() {
        assertEquals("the-train-became-a-river", TextUtils.slugify("The train became a river"))
        assertEquals("dream", TextUtils.slugify("!!!"))
        assertTrue(TextUtils.slugify("Café / Ocean").startsWith("cafe-ocean"))
    }

    @Test
    fun plainExcerptRemovesMarkdownNoise() {
        val body = "I saw [[my mother]] by the #ocean.\n\n![[audio/dream.m4a]]"
        assertEquals("I saw my mother by the ocean.", TextUtils.plainExcerpt(body))
    }
}
