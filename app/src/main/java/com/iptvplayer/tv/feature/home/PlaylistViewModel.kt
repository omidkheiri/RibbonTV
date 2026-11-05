package com.iptvplayer.tv.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.iptvplayer.tv.data.remote.DownloadProgress
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.domain.model.Playlist
import com.iptvplayer.tv.domain.repository.PlaylistRepository
import com.iptvplayer.tv.feature.refresh.PlaylistRefreshWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val url: String = "",
    val playlist: Playlist? = null,
    val entries: List<M3uEntry> = emptyList(),
    val isRefreshing: Boolean = false,
    val progress: DownloadProgress? = null,
    val errorMessage: String? = null,
    val selectedEntry: M3uEntry? = null,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val workManager = WorkManager.getInstance(context)
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            combine(repository.observePlaylist(), repository.observeContent(), ::Pair)
                .collect { (playlist, entries) ->
                    _uiState.update { state ->
                        state.copy(
                            url = state.url.ifBlank { playlist?.sourceUrl.orEmpty() },
                            playlist = playlist,
                            entries = entries,
                        )
                    }
                }
        }
    }

    fun onUrlChanged(url: String) {
        _uiState.update { it.copy(url = url, errorMessage = null) }
    }

    fun refreshPlaylist() {
        if (refreshJob?.isActive == true) return
        val sourceUrl = _uiState.value.url.trim()
        if (sourceUrl.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter an HTTP or HTTPS playlist URL.") }
            return
        }

        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, progress = DownloadProgress(0, null), errorMessage = null) }
            try {
                repository.savePlaylistUrl(sourceUrl)
                schedulePeriodicRefresh(sourceUrl)
                repository.refreshPlaylist(sourceUrl) { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
                _uiState.update { it.copy(progress = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: IOException) {
                _uiState.update { it.copy(errorMessage = exception.message ?: "Playlist download failed.") }
            } catch (exception: IllegalArgumentException) {
                _uiState.update { it.copy(errorMessage = exception.message ?: "The playlist URL or content is invalid.") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, progress = null) }
            }
        }
    }

    fun selectContent(entry: M3uEntry) {
        _uiState.update { it.copy(selectedEntry = entry) }
    }

    fun closePlayer() {
        _uiState.update { it.copy(selectedEntry = null) }
    }

    fun entriesFor(category: ContentCategory): List<M3uEntry> =
        when (category) {
            ContentCategory.SERIES, ContentCategory.VIDEO ->
                _uiState.value.entries.filter { it.category == ContentCategory.SERIES || it.category == ContentCategory.VIDEO }
            else -> _uiState.value.entries.filter { it.category == category }
        }

    private fun schedulePeriodicRefresh(sourceUrl: String) {
        val request = PeriodicWorkRequestBuilder<PlaylistRefreshWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(workDataOf(PlaylistRefreshWorker.KEY_SOURCE_URL to sourceUrl))
            .build()

        workManager.enqueueUniquePeriodicWork(
            PlaylistRefreshWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
