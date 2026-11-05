package com.iptvplayer.tv.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.iptvplayer.tv.core.player.PlayerViewModel
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.feature.home.components.ContentGrid
import com.iptvplayer.tv.feature.home.components.ContentRow
import com.iptvplayer.tv.feature.home.components.EmptyContentState
import com.iptvplayer.tv.feature.home.components.FeaturedHero
import com.iptvplayer.tv.feature.home.components.HomeTab
import com.iptvplayer.tv.feature.home.components.TvColors
import com.iptvplayer.tv.feature.home.components.TvTopNavigationBar
import com.iptvplayer.tv.feature.home.components.tvFocusModifier
import com.iptvplayer.tv.feature.player.PlayerScreen
import java.text.DateFormat
import java.util.Date

private enum class SecondaryScreen { NONE, SEARCH, SETTINGS, UNCATEGORIZED, DETAILS }

@Composable
fun HomeScreen(viewModel: PlaylistViewModel, playerViewModel: PlayerViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedEntry = state.selectedEntry
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.FOR_YOU) }
    var secondary by rememberSaveable { mutableStateOf(SecondaryScreen.NONE) }
    var detailsEntry by remember { mutableStateOf<M3uEntry?>(null) }
    val stateHolder = rememberSaveableStateHolder()

    if (selectedEntry != null) {
        LaunchedEffect(selectedEntry.id) { playerViewModel.play(selectedEntry.title, selectedEntry.streamUrl) }
        PlayerScreen(viewModel = playerViewModel, title = selectedEntry.title, onBack = viewModel::closePlayer)
        return
    }

    BackHandler(enabled = secondary != SecondaryScreen.NONE) { secondary = SecondaryScreen.NONE }

    Box(Modifier.fillMaxSize().background(TvColors.Background)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF0D0B16), TvColors.Background), endY = 650f),
            ).padding(horizontal = 28.dp),
        ) {
            when (secondary) {
                SecondaryScreen.SEARCH -> SearchScreen(state.entries, viewModel::selectContent)
                SecondaryScreen.SETTINGS -> PlaylistSettingsScreen(
                    state, viewModel::onUrlChanged, viewModel::refreshPlaylist,
                    onUncategorized = { secondary = SecondaryScreen.UNCATEGORIZED },
                )
                SecondaryScreen.UNCATEGORIZED -> CatalogScreen(
                    "Uncategorized", "Items that could not be classified from playlist metadata.",
                    state.entries.filter { it.category == ContentCategory.UNCATEGORIZED }, viewModel::selectContent,
                )
                SecondaryScreen.DETAILS -> DetailsScreen(detailsEntry, viewModel::selectContent)
                SecondaryScreen.NONE -> stateHolder.SaveableStateProvider(selectedTab.name) {
                    when (selectedTab) {
                        HomeTab.FOR_YOU -> ForYouScreen(
                            state.entries,
                            viewModel::selectContent,
                            onDetails = { detailsEntry = it; secondary = SecondaryScreen.DETAILS },
                            onAddPlaylist = { secondary = SecondaryScreen.SETTINGS },
                        )
                        HomeTab.LIVE -> LiveCatalogScreen(state.entries, viewModel::selectContent)
                        HomeTab.MOVIES -> CatalogScreen(
                            "Movies", "Browse movies from your playlist.",
                            state.entries.filter { it.category == ContentCategory.MOVIE }, viewModel::selectContent,
                        )
                        HomeTab.SERIES -> CatalogScreen(
                            "Series & Videos", "Items are shown exactly as provided by your playlist.",
                            state.entries.filter { it.category == ContentCategory.SERIES || it.category == ContentCategory.VIDEO }, viewModel::selectContent,
                        )
                    }
                }
            }
        }

        TvTopNavigationBar(
            selected = selectedTab,
            onSelected = { selectedTab = it; secondary = SecondaryScreen.NONE },
            onSearch = { secondary = SecondaryScreen.SEARCH },
            onSettings = { secondary = SecondaryScreen.SETTINGS },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun ForYouScreen(entries: List<M3uEntry>, onSelect: (M3uEntry) -> Unit, onDetails: (M3uEntry) -> Unit, onAddPlaylist: () -> Unit) {
    if (entries.isEmpty()) {
        EmptyContentState("Your library is ready for a playlist", "Open Settings to add an authorized M3U playlist.", actionLabel = "Open Playlist Settings", onAction = onAddPlaylist)
        return
    }
    val live = remember(entries) { entries.filter { it.category == ContentCategory.LIVE_TV } }
    val movies = remember(entries) { entries.filter { it.category == ContentCategory.MOVIE } }
    val series = remember(entries) { entries.filter { it.category == ContentCategory.SERIES || it.category == ContentCategory.VIDEO } }
    val featured = movies.firstOrNull() ?: series.firstOrNull() ?: live.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 54.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(34.dp),
    ) {
        item { FeaturedHero(featured, onSelect, featured?.let { { onDetails(it) } }) }
        if (live.isNotEmpty()) item { ContentRow("Live Channels", live.take(20), live = true, onSelect = onSelect) }
        if (movies.isNotEmpty()) item { ContentRow("Recently Added Movies", movies.take(20), onSelect = onSelect) }
        if (series.isNotEmpty()) item { ContentRow("Series", series.take(20), onSelect = onSelect) }
    }
}

@Composable
private fun LiveCatalogScreen(entries: List<M3uEntry>, onSelect: (M3uEntry) -> Unit) {
    val live = remember(entries) { entries.filter { it.category == ContentCategory.LIVE_TV } }
    val groups = remember(live) { live.mapNotNull { it.groupTitle?.takeIf(String::isNotBlank) }.distinct().sorted() }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(live, selectedGroup, query) {
        live.filter { entry ->
            (selectedGroup == null || entry.groupTitle == selectedGroup) &&
                (query.isBlank() || entry.title.contains(query, true) || entry.groupTitle.orEmpty().contains(query, true))
        }
    }
    Column(Modifier.fillMaxSize().padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeading("Live TV", "Fast access to ${live.size} live channels")
        TvTextField(query, "Filter live channels", { query = it })
        if (groups.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item { FilterChip("All", selectedGroup == null) { selectedGroup = null } }
                items(groups) { group -> FilterChip(group, selectedGroup == group) { selectedGroup = group } }
            }
        }
        ContentGrid(filtered, "No live channels match this category.", onSelect, Modifier.weight(1f), live = true)
    }
}

@Composable
private fun DetailsScreen(entry: M3uEntry?, onPlay: (M3uEntry) -> Unit) {
    if (entry == null) {
        EmptyContentState("Details unavailable", "Return to For You and choose another item.")
        return
    }
    Column(Modifier.fillMaxSize().padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(entry.category.name.replace('_', ' '), color = TvColors.PurpleBright, fontWeight = FontWeight.Bold)
        Text(entry.title, color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        entry.groupTitle?.let { Text(it, color = TvColors.TextSecondary, style = MaterialTheme.typography.titleMedium) }
        Text("Metadata is provided by your imported playlist.", color = TvColors.TextSecondary)
        ActionButton("▶  Watch Now", true) { onPlay(entry) }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.tvFocusModifier(focused, 20, 1.04f)
            .background(if (selected) TvColors.Purple else TvColors.SurfaceRaised, RoundedCornerShape(20.dp))
            .onFocusChanged { focused = it.isFocused }.clickable(onClick = onClick).focusable()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) { Text(label, color = Color.White, maxLines = 1) }
}

@Composable
private fun CatalogScreen(title: String, subtitle: String, entries: List<M3uEntry>, onSelect: (M3uEntry) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeading(title, subtitle)
        ContentGrid(entries, "No $title are available in this playlist.", onSelect, Modifier.weight(1f))
    }
}

@Composable
private fun SearchScreen(entries: List<M3uEntry>, onSelect: (M3uEntry) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(entries, query) {
        if (query.isBlank()) emptyList() else entries.filter { it.title.contains(query, true) || it.groupTitle.orEmpty().contains(query, true) }
    }
    Column(Modifier.fillMaxSize().padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeading("Search", "Search your locally stored Live TV, Movies, and Series catalog.")
        TvTextField(query, "Search titles or categories", { query = it })
        when {
            query.isBlank() -> EmptyContentState("Start typing to search", "Results appear instantly from your saved catalog.", Modifier.weight(1f))
            results.isEmpty() -> EmptyContentState("No results", "Try a different title or category.", Modifier.weight(1f))
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ContentCategory.entries.filter { it != ContentCategory.UNCATEGORIZED && it != ContentCategory.VIDEO }.forEach { category ->
                    val grouped = results.filter { if (category == ContentCategory.SERIES) it.category == ContentCategory.SERIES || it.category == ContentCategory.VIDEO else it.category == category }
                    if (grouped.isNotEmpty()) item(category.name) {
                        ContentRow(category.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase), grouped, live = category == ContentCategory.LIVE_TV, onSelect = onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistSettingsScreen(state: PlaylistUiState, onUrlChanged: (String) -> Unit, onRefresh: () -> Unit, onUncategorized: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        ScreenHeading("Settings · Playlists", "Add or refresh an authorized HTTP/HTTPS M3U playlist.")
        Column(Modifier.fillMaxWidth().background(TvColors.Surface, RoundedCornerShape(22.dp)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Playlist URL", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TvTextField(state.url, "HTTP/HTTPS M3U playlist URL", onUrlChanged, onRefresh)
            ActionButton(if (state.isRefreshing) "Loading…" else if (state.playlist == null) "Add Playlist" else "Refresh Playlist", !state.isRefreshing, onRefresh)
            when {
                state.isRefreshing -> Text(downloadStatus(state), color = TvColors.TextSecondary)
                state.errorMessage != null -> Text(state.errorMessage, color = TvColors.Error)
                state.playlist != null -> Text(
                    "${state.entries.size} items · ${state.playlist.name}" + state.playlist.lastSyncedAtEpochMillis?.let { " · Updated ${DateFormat.getDateTimeInstance().format(Date(it))}" }.orEmpty(),
                    color = TvColors.TextSecondary,
                )
            }
        }
        val unclassified = state.entries.count { it.category == ContentCategory.UNCATEGORIZED }
        if (unclassified > 0) ActionButton("Browse Uncategorized ($unclassified)", true, onUncategorized)
    }
}

private fun downloadStatus(state: PlaylistUiState): String {
    val progress = state.progress
    return if (progress?.totalBytes != null && progress.totalBytes > 0) "Downloading playlist: ${(progress.bytesRead * 100 / progress.totalBytes).toInt()}%" else "Downloading and parsing playlist…"
}

@Composable
private fun ScreenHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = TvColors.TextPrimary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TvColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TvTextField(value: String, placeholder: String, onValueChanged: (String) -> Unit, onGo: (() -> Unit)? = null) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.fillMaxWidth().height(60.dp)
            .border(if (focused) 3.dp else 1.dp, if (focused) TvColors.PurpleBright else Color(0xFF4A4658), RoundedCornerShape(12.dp))
            .background(TvColors.SurfaceRaised, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isBlank()) Text(placeholder, color = TvColors.TextSecondary)
        BasicTextField(
            value, onValueChanged, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = if (onGo != null) ImeAction.Go else ImeAction.Search),
            keyboardActions = KeyboardActions(onGo = { onGo?.invoke() }),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
    }
}

@Composable
private fun ActionButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.tvFocusModifier(focused, 12, 1.04f)
            .background(if (enabled) TvColors.Purple else Color.DarkGray, RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }.clickable(enabled = enabled, onClick = onClick).focusable(enabled)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}
