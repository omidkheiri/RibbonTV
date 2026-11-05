package com.iptvplayer.tv.domain.repository

import com.iptvplayer.tv.domain.model.Channel
import com.iptvplayer.tv.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<List<Channel>>

    suspend fun synchronize(playlist: Playlist)
}
