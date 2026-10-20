package com.lemomation.lucidlogger

import com.lemomation.lucidlogger.data.markdown.WikiLinker
import org.junit.Assert.assertEquals
import org.junit.Test

class WikiLinkerTest {
    @Test
    fun linksKnownEntityOnlyOncePerMention() {
        val body = "The ocean was endless. I walked into the ocean and the ocean held me."
        val linked = WikiLinker.autoLink(body, listOf("ocean"))
        assertEquals("The [[ocean]] was endless. I walked into the ocean and the ocean held me.", linked)
    }

    @Test
    fun protectsExistingLinksCodeAndUrls() {
        val body = "See [[ocean]] and https://example.com/ocean and `ocean` and ocean."
        val linked = WikiLinker.autoLink(body, listOf("ocean"))
        assertEquals("See [[ocean]] and https://example.com/ocean and `ocean` and [[ocean]].", linked)
    }

    @Test
    fun longerNamesWin() {
        val linked = WikiLinker.autoLink("The night train arrived.", listOf("night", "night train"))
        assertEquals("The [[night train]] arrived.", linked)
    }
}
