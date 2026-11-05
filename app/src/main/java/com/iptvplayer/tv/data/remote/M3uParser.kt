package com.iptvplayer.tv.data.remote

import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.domain.model.M3uPlaylist
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject

fun interface M3uParser {
    fun parse(playlistText: String): M3uPlaylist
}

class PlaylistFormatException(message: String) : IllegalArgumentException(message)

class DefaultM3uParser @Inject constructor() : M3uParser {
    override fun parse(playlistText: String): M3uPlaylist {
        val lines = playlistText.lineSequence()
            .map { it.trim().removePrefix("\uFEFF") }
            .filter(String::isNotEmpty)
            .toList()

        if (lines.any { it.startsWith(HLS_TAG_PREFIX) }) {
            throw PlaylistFormatException("This URL points to an HLS media manifest, not an IPTV channel playlist.")
        }
        val header = lines.firstOrNull()?.takeIf {
            it.equals(EXT_M3U, ignoreCase = true) || it.startsWith("$EXT_M3U ", ignoreCase = true)
        }
            ?: throw PlaylistFormatException("The downloaded file is not a valid M3U playlist.")

        val playlistName = parseAttributes(header.removePrefix(EXT_M3U))
            .let { it["name"] ?: it["tvg-name"] }
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_PLAYLIST_NAME

        val entries = mutableListOf<M3uEntry>()
        var pending: ParsedExtInf? = null

        for (line in lines.drop(1)) {
            when {
                line.startsWith(HLS_TAG_PREFIX) ->
                    throw PlaylistFormatException("HLS media manifests are not channel playlists.")
                line.startsWith(EXT_INF_PREFIX) ->
                    pending = parseExtInf(line)
                line.startsWith('#') -> Unit
                pending != null -> {
                    val metadata = pending
                    val title = metadata.title
                        .ifBlank { metadata.attributes["tvg-name"].orEmpty() }
                        .ifBlank { "Channel ${entries.size + 1}" }
                    val tvgId = metadata.attributes["tvg-id"]?.takeIf(String::isNotBlank)
                    val streamUrl = line
                    entries += M3uEntry(
                        id = stableId(metadata.rawLine, streamUrl, entries.size),
                        title = title,
                        streamUrl = streamUrl,
                        tvgId = tvgId,
                        tvgName = metadata.attributes["tvg-name"]?.takeIf(String::isNotBlank),
                        logoUrl = metadata.attributes["tvg-logo"]?.takeIf(String::isNotBlank),
                        groupTitle = metadata.attributes["group-title"]?.takeIf(String::isNotBlank),
                        rawExtInf = metadata.rawLine,
                    )
                    pending = null
                }
            }
        }

        return M3uPlaylist(name = playlistName, entries = entries)
    }

    private fun parseExtInf(line: String): ParsedExtInf? {
        val descriptor = line.substringAfter(':', missingDelimiterValue = "")
        val separator = findUnquotedComma(descriptor)
        if (separator < 0) return null

        return ParsedExtInf(
            attributes = parseAttributes(descriptor.substring(0, separator)),
            title = descriptor.substring(separator + 1).trim(),
            rawLine = line,
        )
    }

    private fun parseAttributes(value: String): Map<String, String> =
        ATTRIBUTE_PATTERN.findAll(value).associate { match ->
            val attributeValue = match.groups[2]?.value
                ?: match.groups[3]?.value
                ?: match.groups[4]?.value.orEmpty()
            match.groups[1]!!.value.lowercase(Locale.ROOT) to attributeValue
        }

    private fun findUnquotedComma(value: String): Int {
        var quote: Char? = null
        var escaped = false

        for (index in value.indices) {
            val character = value[index]
            when {
                escaped -> escaped = false
                character == '\\' && quote != null -> escaped = true
                quote != null && character == quote -> quote = null
                quote == null && (character == '"' || character == '\'') -> quote = character
                quote == null && character == ',' -> return index
            }
        }

        return -1
    }

    private fun stableId(rawExtInf: String, streamUrl: String, index: Int): String {
        val input = "$index\n$rawExtInf\n$streamUrl".toByteArray(Charsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(input)
            .take(ID_HASH_BYTES)
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private data class ParsedExtInf(
        val attributes: Map<String, String>,
        val title: String,
        val rawLine: String,
    )

    private companion object {
        const val EXT_M3U = "#EXTM3U"
        const val EXT_INF_PREFIX = "#EXTINF:"
        const val HLS_TAG_PREFIX = "#EXT-X-"
        const val DEFAULT_PLAYLIST_NAME = "IPTV Playlist"
        const val ID_HASH_BYTES = 16
        val ATTRIBUTE_PATTERN = Regex("""([A-Za-z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")
    }
}
