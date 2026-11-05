package com.iptvplayer.tv.data.remote

import com.iptvplayer.tv.domain.model.Channel

/**
 * Parses channel-list M3U input. HLS manifests are playback inputs for Media3, not channel playlists.
 */
fun interface M3uPlaylistParser {
    fun parse(playlistText: String): List<Channel>
}

class DefaultM3uPlaylistParser : M3uPlaylistParser {
    override fun parse(playlistText: String): List<Channel> {
        val lines = playlistText.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.any { it.startsWith(HLS_TAG_PREFIX) }) return emptyList()

        val channels = mutableListOf<Channel>()
        var pendingEntry: EntryMetadata? = null

        for (line in lines) {
            when {
                line.startsWith(EXTINF_PREFIX) -> {
                    pendingEntry = parseEntryMetadata(line)
                }
                line.startsWith('#') -> Unit
                pendingEntry != null -> {
                    val metadata = pendingEntry
                    channels += Channel(
                        id = metadata.id ?: line,
                        name = metadata.name.ifBlank { "Channel ${channels.size + 1}" },
                        streamUrl = line,
                        groupId = metadata.groupName,
                        logoUrl = metadata.logoUrl,
                    )
                    pendingEntry = null
                }
            }
        }

        return channels
    }

    private fun parseEntryMetadata(line: String): EntryMetadata? {
        val descriptor = line.substringAfter(':', missingDelimiterValue = "")
        val titleSeparator = findUnquotedComma(descriptor)
        if (titleSeparator < 0) return null

        val attributes = ATTRIBUTE_PATTERN.findAll(descriptor.substring(0, titleSeparator))
            .associate { match ->
                val value = match.groups[2]?.value
                    ?: match.groups[3]?.value
                    ?: match.groups[4]?.value.orEmpty()
                match.groups[1]!!.value.lowercase() to value
            }
        val title = descriptor.substring(titleSeparator + 1).trim()

        return EntryMetadata(
            id = attributes["tvg-id"]?.takeIf(String::isNotBlank),
            name = title.ifBlank { attributes["tvg-name"].orEmpty() },
            groupName = attributes["group-title"]?.takeIf(String::isNotBlank),
            logoUrl = attributes["tvg-logo"]?.takeIf(String::isNotBlank),
        )
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

    private data class EntryMetadata(
        val id: String?,
        val name: String,
        val groupName: String?,
        val logoUrl: String?,
    )

    private companion object {
        const val EXTINF_PREFIX = "#EXTINF:"
        const val HLS_TAG_PREFIX = "#EXT-X-"
        val ATTRIBUTE_PATTERN = Regex("""([A-Za-z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")
    }
}
