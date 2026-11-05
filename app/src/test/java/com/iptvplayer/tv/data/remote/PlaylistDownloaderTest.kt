package com.iptvplayer.tv.data.remote

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDownloaderTest {
    private val downloader = PlaylistDownloader(OkHttpClient())

    @Test
    fun `accepts HTTP and HTTPS playlist URLs and normalizes whitespace`() {
        assertEquals(
            "http://example.com/playlist.m3u",
            downloader.validateUrl("  http://example.com/playlist.m3u  "),
        )
        assertEquals(
            "https://example.com/playlist.m3u",
            downloader.validateUrl("https://example.com/playlist.m3u"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects unsupported URL schemes`() {
        downloader.validateUrl("ftp://example.com/playlist.m3u")
    }

    @Test
    fun `downloads playlist text and reports byte progress`() = runBlocking {
        val playlistText = "#EXTM3U\n#EXTINF:-1,Test Channel\nhttps://example.com/live\n"
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/playlist.m3u") { exchange ->
            val bytes = playlistText.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()

        try {
            val progress = mutableListOf<DownloadProgress>()
            val downloaded = downloader.download(
                "http://127.0.0.1:${server.address.port}/playlist.m3u",
                progress::add,
            )

            assertEquals(playlistText, downloaded)
            assertTrue(progress.isNotEmpty())
            assertEquals(playlistText.toByteArray(Charsets.UTF_8).size.toLong(), progress.last().bytesRead)
        } finally {
            server.stop(0)
        }
    }
}
