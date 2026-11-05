package com.iptvplayer.tv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultM3uParserTest {
    private val parser = DefaultM3uParser()

    @Test
    fun `parses standard IPTV metadata and preserves raw EXTINF`() {
        val playlist = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-id="news.one" tvg-name="News One" tvg-logo="https://example.com/logo.png" group-title="News",News One HD
                https://example.com/live/news.m3u8
            """.trimIndent(),
        )

        assertEquals(1, playlist.entries.size)
        val entry = playlist.entries.single()
        assertEquals("news.one", entry.tvgId)
        assertEquals("News One", entry.tvgName)
        assertEquals("News One HD", entry.title)
        assertEquals("News", entry.groupTitle)
        assertEquals("https://example.com/logo.png", entry.logoUrl)
        assertEquals("https://example.com/live/news.m3u8", entry.streamUrl)
        assertTrue(entry.rawExtInf.startsWith("#EXTINF:"))
    }

    @Test
    fun `parses quoted commas and playlist title`() {
        val playlist = parser.parse(
            """
                #EXTM3U name="International, Channels"
                #EXTINF:-1 group-title="News, International" tvg-name='World, Today',World, Today HD
                https://example.com/world
            """.trimIndent(),
        )

        assertEquals("International, Channels", playlist.name)
        assertEquals("World, Today HD", playlist.entries.single().title)
        assertEquals("News, International", playlist.entries.single().groupTitle)
    }

    @Test
    fun `uses tvg name then generated title when display title is empty`() {
        val playlist = parser.parse(
            """
                #EXTM3U
                #EXTINF:-1 tvg-name="Fallback Name",
                https://example.com/fallback
                #EXTINF:-1,
                https://example.com/unnamed
            """.trimIndent(),
        )

        assertEquals("Fallback Name", playlist.entries[0].title)
        assertEquals("Channel 2", playlist.entries[1].title)
    }

    @Test
    fun `ignores standalone URLs and incomplete entries`() {
        val playlist = parser.parse(
            """
                #EXTM3U
                https://example.com/no-metadata
                #EXTINF:-1,Missing stream
                #EXTINF:-1,Valid entry
                https://example.com/valid
            """.trimIndent(),
        )

        assertEquals(1, playlist.entries.size)
        assertEquals("Valid entry", playlist.entries.single().title)
    }

    @Test(expected = PlaylistFormatException::class)
    fun `rejects HLS media manifests as channel playlists`() {
        parser.parse(
            """
                #EXTM3U
                #EXT-X-VERSION:3
                #EXTINF:6.0,
                segment-1.ts
            """.trimIndent(),
        )
    }

    @Test(expected = PlaylistFormatException::class)
    fun `rejects content without an M3U header`() {
        parser.parse("#EXTINF:-1,Channel\nhttps://example.com/channel")
    }
}
