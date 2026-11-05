package com.iptvplayer.tv.feature.refresh

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iptvplayer.tv.domain.repository.PlaylistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class PlaylistRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val repository: PlaylistRepository,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val sourceUrl = inputData.getString(KEY_SOURCE_URL)
            ?: return Result.failure()

        return try {
            repository.refreshPlaylist(sourceUrl)
            Result.success()
        } catch (exception: IOException) {
            Result.retry()
        } catch (exception: IllegalArgumentException) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_SOURCE_URL = "playlist_source_url"
        const val UNIQUE_WORK_NAME = "iptv_playlist_refresh"
    }
}
