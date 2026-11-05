package com.iptvplayer.tv.domain.repository

import com.iptvplayer.tv.data.remote.DownloadProgress
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun observePlaylist(): Flow<Playlist?>

    fun observeContent(): Flow<List<M3uEntry>>

    suspend fun savePlaylistUrl(sourceUrl: String)

    suspend fun refreshPlaylist(
        sourceUrl: String,
        onProgress: (DownloadProgress) -> Unit = {},
    )
}
