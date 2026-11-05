package com.iptvplayer.tv.domain.model

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val groupId: String? = null,
    val logoUrl: String? = null,
)

data class ChannelGroup(
    val id: String,
    val name: String,
    val channelIds: List<String> = emptyList(),
)

data class Playlist(
    val id: String,
    val name: String,
    val sourceUrl: String,
    val lastSyncedAtEpochMillis: Long? = null,
)

data class M3uPlaylist(
    val name: String,
    val entries: List<M3uEntry>,
)

data class M3uEntry(
    val id: String,
    val title: String,
    val streamUrl: String,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val rawExtInf: String,
    val category: ContentCategory = ContentCategory.UNCATEGORIZED,
)

enum class ContentCategory {
    LIVE_TV,
    MOVIE,
    SERIES,
    VIDEO,
    UNCATEGORIZED,
}

data class EPGProgram(
    val id: String,
    val channelId: String,
    val title: String,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val description: String? = null,
)

data class FavoriteChannel(
    val channelId: String,
    val addedAtEpochMillis: Long,
)

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR,
}
