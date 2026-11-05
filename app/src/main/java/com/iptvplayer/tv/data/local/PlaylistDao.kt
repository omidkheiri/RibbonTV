package com.iptvplayer.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun observePlaylist(id: String = PlaylistEntity.DEFAULT_PLAYLIST_ID): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylist(id: String = PlaylistEntity.DEFAULT_PLAYLIST_ID): PlaylistEntity?

    @Query("SELECT * FROM playlist_content ORDER BY title COLLATE NOCASE")
    fun observeContent(): Flow<List<PlaylistContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlist_content")
    suspend fun deleteContent()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContent(content: List<PlaylistContentEntity>)

}
