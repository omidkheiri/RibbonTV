package com.iptvplayer.tv.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.iptvplayer.tv.domain.model.M3uEntry
import com.iptvplayer.tv.feature.home.components.ContentGrid

@Composable
fun SeriesScreen(items: List<M3uEntry>, onSelect: (M3uEntry) -> Unit, modifier: Modifier = Modifier) {
    ContentGrid(items = items, emptyMessage = "No series or videos in this playlist.", onSelect = onSelect, modifier = modifier)
}
