package com.iptvplayer.tv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.domain.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String = DEFAULT_PLAYLIST_ID,
    val name: String,
    val sourceUrl: String,
    val lastSuccessfulRefreshEpochMillis: Long? = null,
) {
    companion object {
        const val DEFAULT_PLAYLIST_ID = "default"
    }
}

@Entity(tableName = "playlist_content")
data class PlaylistContentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val streamUrl: String,
    val tvgId: String?,
    val tvgName: String?,
    val logoUrl: String?,
    val groupTitle: String?,
    val rawExtInf: String,
    val category: String,
)

fun PlaylistEntity.toDomain(): Playlist =
    Playlist(id = id, name = name, sourceUrl = sourceUrl, lastSyncedAtEpochMillis = lastSuccessfulRefreshEpochMillis)

fun PlaylistContentEntity.toDomain(): M3uEntry =
    M3uEntry(
        id = id,
        title = title,
        streamUrl = streamUrl,
        tvgId = tvgId,
        tvgName = tvgName,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        rawExtInf = rawExtInf,
        category = ContentCategory.entries.firstOrNull { it.name == category }
            ?: ContentCategory.UNCATEGORIZED,
    )

fun M3uEntry.toEntity(): PlaylistContentEntity =
    PlaylistContentEntity(
        id = id,
        title = title,
        streamUrl = streamUrl,
        tvgId = tvgId,
        tvgName = tvgName,
        logoUrl = logoUrl,
        groupTitle = groupTitle,
        rawExtInf = rawExtInf,
        category = category.name,
    )
