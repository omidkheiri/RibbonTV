package com.iptvplayer.tv.core.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.iptvplayer.tv.domain.model.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState.BUFFERING,
    val errorMessage: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val sampleStream = MediaItem.Builder()
        .setUri(SAMPLE_HLS_URL)
        .setMimeType(MimeTypes.APPLICATION_M3U8)
        .build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val current = _uiState.value
                _uiState.value = current.copy(
                    playbackState = when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                        Player.STATE_READY -> if (player.isPlaying) {
                            PlaybackState.PLAYING
                        } else {
                            PlaybackState.PAUSED
                        }
                        Player.STATE_ENDED -> PlaybackState.ENDED
                        else -> PlaybackState.IDLE
                    },
                    errorMessage = null,
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(
                    playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED,
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = _uiState.value.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = error.message ?: "Unable to play the test stream.",
                )
            }
        })

        prepareAndPlay()
    }

    fun togglePlayback() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun pause() {
        player.pause()
    }

    fun retry() {
        prepareAndPlay()
    }

    private fun prepareAndPlay() {
        _uiState.value = PlayerUiState(playbackState = PlaybackState.BUFFERING)
        player.setMediaItem(sampleStream)
        player.prepare()
        player.play()
    }

    override fun onCleared() {
        player.release()
    }

    private companion object {
        const val SAMPLE_HLS_URL = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
    }
}
