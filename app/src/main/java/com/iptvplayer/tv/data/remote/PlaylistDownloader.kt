package com.iptvplayer.tv.data.remote

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

data class DownloadProgress(val bytesRead: Long, val totalBytes: Long?)

class PlaylistDownloader @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    suspend fun download(
        sourceUrl: String,
        onProgress: (DownloadProgress) -> Unit = {},
    ): String = withContext(Dispatchers.IO) {
        val url = sourceUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid HTTP or HTTPS playlist URL.")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.apple.mpegurl, application/x-mpegURL, text/plain, */*")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Playlist server returned HTTP ${response.code}.")
            val body = response.body ?: throw IOException("Playlist server returned an empty response.")
            val contentLength = body.contentLength().takeIf { it >= 0L }
            if (contentLength != null && contentLength > MAX_PLAYLIST_BYTES) {
                throw IOException("Playlist is larger than the 32 MB download limit.")
            }

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            var totalRead = 0L
            body.byteStream().use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    totalRead += count
                    if (totalRead > MAX_PLAYLIST_BYTES) {
                        throw IOException("Playlist is larger than the 32 MB download limit.")
                    }
                    output.write(buffer, 0, count)
                    onProgress(DownloadProgress(totalRead, contentLength))
                }
            }
            String(output.toByteArray(), Charsets.UTF_8)
        }
    }

    fun validateUrl(sourceUrl: String): String {
        val normalized = sourceUrl.trim()
        val url: HttpUrl = normalized.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Enter a valid HTTP or HTTPS playlist URL.")
        return url.toString()
    }

    fun resolveStreamUrl(playlistUrl: String, streamUrl: String): String {
        val streamUri = try {
            URI(streamUrl)
        } catch (exception: URISyntaxException) {
            throw IllegalArgumentException("Playlist contains an invalid stream URL.", exception)
        }
        if (streamUri.isAbsolute) return streamUrl

        val playlistUri = try {
            URI(playlistUrl)
        } catch (exception: URISyntaxException) {
            throw IllegalArgumentException("Playlist URL is invalid.", exception)
        }
        return playlistUri.resolve(streamUri).toString()
    }

    private companion object {
        const val MAX_PLAYLIST_BYTES = 32L * 1024L * 1024L
        const val BUFFER_SIZE = 8 * 1024
    }
}
