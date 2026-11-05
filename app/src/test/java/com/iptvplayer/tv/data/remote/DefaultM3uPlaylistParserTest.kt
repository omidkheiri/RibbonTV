package com.iptvplayer.tv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultM3uPlaylistParserTest {
    private val parser = DefaultM3uPlaylistParser()

    @Test
    fun `parses channel metadata and stream URL`() {
        val channels = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-id="news.one" tvg-name="News One" tvg-logo="https://example.com/logo.png" group-title="News",News One HD
                https://example.com/live/news.m3u8
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("news.one", channels.single().id)
        assertEquals("News One HD", channels.single().name)
        assertEquals("https://example.com/live/news.m3u8", channels.single().streamUrl)
        assertEquals("News", channels.single().groupId)
        assertEquals("https://example.com/logo.png", channels.single().logoUrl)
    }

    @Test
    fun `keeps commas inside quoted metadata and channel titles`() {
        val channels = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1 group-title="News, International" tvg-name='World, Today',World, Today HD
                https://example.com/world
            """.trimIndent(),
        )

        assertEquals("World, Today HD", channels.single().name)
        assertEquals("News, International", channels.single().groupId)
    }

    @Test
    fun `uses tvg name when display title is empty and URL when ID is absent`() {
        val channels = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-name="Fallback Name",
                https://example.com/fallback
            """.trimIndent(),
        )

        assertEquals("https://example.com/fallback", channels.single().id)
        assertEquals("Fallback Name", channels.single().name)
    }

    @Test
    fun `ignores incomplete entries and standalone URLs`() {
        val channels = parser.parse(
            """
                #EXTM3U
                https://example.com/no-metadata
                #EXTINF:-1,Missing stream
                #EXTINF:-1,Valid entry
                https://example.com/valid
            """.trimIndent(),
        )

        assertEquals(1, channels.size)
        assertEquals("Valid entry", channels.single().name)
        assertEquals("https://example.com/valid", channels.single().streamUrl)
    }

    @Test
    fun `does not parse HLS manifests as channel playlists`() {
        val channels = parser.parse(
            """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXTINF:6.0,
                segment-1.ts
            """.trimIndent(),
        )

        assertTrue(channels.isEmpty())
    }

    @Test
    fun `uses numbered name when title and tvg name are absent`() {
        val channels = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1,
                https://example.com/unnamed
            """.trimIndent(),
        )

        assertEquals("Channel 1", channels.single().name)
        assertNull(channels.single().groupId)
        assertNull(channels.single().logoUrl)
    }
}
