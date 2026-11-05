package com.iptvplayer.tv.data.repository

import com.iptvplayer.tv.data.local.PlaylistDao
import com.iptvplayer.tv.data.local.PlaylistEntity
import com.iptvplayer.tv.data.local.IptvDatabase
import com.iptvplayer.tv.data.local.toDomain
import com.iptvplayer.tv.data.local.toEntity
import com.iptvplayer.tv.data.remote.DownloadProgress
import com.iptvplayer.tv.data.remote.M3uParser
import com.iptvplayer.tv.data.remote.PlaylistDownloader
import com.iptvplayer.tv.domain.classification.ContentClassifier
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.domain.model.Playlist
import com.iptvplayer.tv.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultPlaylistRepository @Inject constructor(
    private val database: IptvDatabase,
    private val playlistDao: PlaylistDao,
    private val downloader: PlaylistDownloader,
    private val parser: M3uParser,
    private val classifier: ContentClassifier,
) : PlaylistRepository {
    override fun observePlaylist(): Flow<Playlist?> =
        playlistDao.observePlaylist().map { it?.toDomain() }

    override fun observeContent(): Flow<List<M3uEntry>> =
        playlistDao.observeContent().map { items -> items.map { it.toDomain() } }

    override suspend fun savePlaylistUrl(sourceUrl: String) {
        val normalizedUrl = downloader.validateUrl(sourceUrl)
        val existing = playlistDao.getPlaylist()
        playlistDao.insertPlaylist(
            (existing ?: PlaylistEntity(name = DEFAULT_PLAYLIST_NAME, sourceUrl = normalizedUrl))
                .copy(sourceUrl = normalizedUrl),
        )
    }

    override suspend fun refreshPlaylist(
        sourceUrl: String,
        onProgress: (DownloadProgress) -> Unit,
    ) {
        val normalizedUrl = downloader.validateUrl(sourceUrl)
        val text = downloader.download(normalizedUrl, onProgress)
        val parsed = parser.parse(text)
        if (parsed.entries.isEmpty()) {
            throw IllegalArgumentException("The playlist contains no channel entries; the saved catalog was not changed.")
        }

        val existing = playlistDao.getPlaylist()
        val entries = parsed.entries.map { entry ->
            entry.copy(
                streamUrl = downloader.resolveStreamUrl(normalizedUrl, entry.streamUrl),
                category = classifier.classify(entry),
            )
        }
        val playlist = PlaylistEntity(
            name = parsed.name,
            sourceUrl = normalizedUrl,
            lastSuccessfulRefreshEpochMillis = System.currentTimeMillis(),
        )

        database.withTransaction {
            playlistDao.deleteContent()
            playlistDao.insertContent(entries.map(M3uEntry::toEntity))
            playlistDao.insertPlaylist(
                playlist.copy(name = parsed.name.ifBlank { existing?.name ?: DEFAULT_PLAYLIST_NAME }),
            )
        }
    }

    private companion object {
        const val DEFAULT_PLAYLIST_NAME = "IPTV Playlist"
    }
}
