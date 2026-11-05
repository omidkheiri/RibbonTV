package com.iptvplayer.tv.core.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.core.net.toUri
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
    val title: String = "",
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val errorMessage: String? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    private var currentUri: String? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val current = _uiState.value
                _uiState.value = current.copy(
                    playbackState = when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                        Player.STATE_READY -> if (player.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
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
                    errorMessage = error.message
                        ?.let { "Playback failed: $it" }
                        ?: "Playback failed. This media format or codec may not be supported.",
                )
            }
        })
    }

    fun play(title: String, uri: String) {
        val parsedUri = uri.toUri()
        if (parsedUri.scheme !in SUPPORTED_SCHEMES) {
            _uiState.value = PlayerUiState(
                title = title,
                playbackState = PlaybackState.ERROR,
                errorMessage = "Unsupported stream URL. Playback requires an HTTP or HTTPS stream.",
            )
            return
        }

        currentUri = uri
        _uiState.value = PlayerUiState(title = title, playbackState = PlaybackState.BUFFERING)
        player.setMediaItem(createMediaItem(uri))
        player.prepare()
        player.play()
    }

    fun togglePlayback() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun pause() {
        player.pause()
    }

    fun retry() {
        currentUri?.let { play(_uiState.value.title, it) }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentUri = null
        _uiState.value = PlayerUiState()
    }

    private fun createMediaItem(uri: String): MediaItem {
        val builder = MediaItem.Builder().setUri(uri)
        if (uri.toUri().lastPathSegment.orEmpty().endsWith(".m3u8", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        return builder.build()
    }

    override fun onCleared() {
        player.release()
    }

    private companion object {
        val SUPPORTED_SCHEMES = setOf("http", "https")
    }
}
