package com.iptvplayer.tv.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvplayer.tv.core.player.PlayerViewModel
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.feature.player.IPTVPlayerTheme
import com.iptvplayer.tv.feature.player.PlayerScreen
import com.iptvplayer.tv.feature.home.components.ContentGrid
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
    viewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val selectedEntry = state.selectedEntry
    val errorMessage = state.errorMessage
    val playlist = state.playlist

    if (selectedEntry != null) {
        LaunchedEffect(selectedEntry.id) {
            playerViewModel.play(selectedEntry.title, selectedEntry.streamUrl)
        }
        IPTVPlayerTheme {
            PlayerScreen(
                viewModel = playerViewModel,
                title = selectedEntry.title,
                onBack = viewModel::closePlayer,
            )
        }
        return
    }

    var selectedCategory by remember { mutableStateOf(ContentCategory.LIVE_TV) }

    IPTVPlayerTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080D16))
                .padding(horizontal = 40.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("IPTV Player", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            PlaylistUrlInput(
                url = state.url,
                isRefreshing = state.isRefreshing,
                hasSavedPlaylist = playlist != null,
                onUrlChanged = viewModel::onUrlChanged,
                onLoad = viewModel::refreshPlaylist,
            )
            when {
                state.isRefreshing -> {
                    val progress = state.progress
                    Text(
                        text = if (progress?.totalBytes != null && progress.totalBytes > 0) {
                            "Downloading playlist: ${(progress.bytesRead * 100 / progress.totalBytes).toInt()}%"
                        } else {
                            "Downloading and parsing playlist…"
                        },
                        color = Color(0xFFB8C5D8),
                    )
                }
                errorMessage != null -> Text(errorMessage, color = Color(0xFFFF8A80))
                playlist != null -> Text(
                    text = buildString {
                        append("${state.entries.size} items · ${playlist.name}")
                        playlist.lastSyncedAtEpochMillis?.let {
                            append(" · Updated ${DateFormat.getDateTimeInstance().format(Date(it))}")
                        }
                    },
                    color = Color(0xFFB8C5D8),
                )
                state.entries.isEmpty() -> Text(
                    "Enter a playlist URL to load your channels and videos.",
                    color = Color(0xFFB8C5D8),
                )
            }

            CategorySelector(
                selected = selectedCategory,
                onSelected = { selectedCategory = it },
            )

            when (selectedCategory) {
                ContentCategory.LIVE_TV -> LiveTvScreen(
                    items = viewModel.entriesFor(ContentCategory.LIVE_TV),
                    onSelect = viewModel::selectContent,
                    modifier = Modifier.weight(1f),
                )
                ContentCategory.MOVIE -> MoviesScreen(
                    items = viewModel.entriesFor(ContentCategory.MOVIE),
                    onSelect = viewModel::selectContent,
                    modifier = Modifier.weight(1f),
                )
                ContentCategory.SERIES, ContentCategory.VIDEO -> SeriesScreen(
                    items = viewModel.entriesFor(ContentCategory.SERIES),
                    onSelect = viewModel::selectContent,
                    modifier = Modifier.weight(1f),
                )
                ContentCategory.UNCATEGORIZED -> UncategorizedScreen(
                    items = viewModel.entriesFor(ContentCategory.UNCATEGORIZED),
                    onSelect = viewModel::selectContent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlaylistUrlInput(
    url: String,
    isRefreshing: Boolean,
    hasSavedPlaylist: Boolean,
    onUrlChanged: (String) -> Unit,
    onLoad: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color(0xFF64B5F6) else Color(0xFF536174),
                    shape = RoundedCornerShape(8.dp),
                )
                .background(Color(0xFF172232), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (url.isEmpty()) {
                Text("HTTP/HTTPS M3U playlist URL", color = Color(0xFF9EADBF))
            }
            BasicTextField(
                value = url,
                onValueChange = onUrlChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { onLoad() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }
        Button(onClick = onLoad, enabled = !isRefreshing) {
            Text(
                when {
                    isRefreshing -> "Loading…"
                    hasSavedPlaylist -> "Refresh Playlist"
                    else -> "Load Playlist"
                },
            )
        }
    }
}

@Composable
private fun CategorySelector(
    selected: ContentCategory,
    onSelected: (ContentCategory) -> Unit,
) {
    val categories = listOf(
        ContentCategory.LIVE_TV to "Live TV",
        ContentCategory.MOVIE to "Movies",
        ContentCategory.SERIES to "Series / Videos",
        ContentCategory.UNCATEGORIZED to "Uncategorized",
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(categories) { (category, label) ->
            Button(
                onClick = { onSelected(category) },
                enabled = selected != category,
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun UncategorizedScreen(
    items: List<com.iptvplayer.tv.domain.model.M3uEntry>,
    onSelect: (com.iptvplayer.tv.domain.model.M3uEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentGrid(
        items = items,
        emptyMessage = "Nothing needs classification yet.",
        onSelect = onSelect,
        modifier = modifier,
    )
}
