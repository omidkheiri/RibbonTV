package com.iptvplayer.tv.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry

@Composable
fun ContentGrid(
    items: List<M3uEntry>,
    emptyMessage: String,
    onSelect: (M3uEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(emptyMessage, color = Color(0xFF9EADBF), style = MaterialTheme.typography.titleMedium)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 220.dp),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = M3uEntry::id) { entry ->
                Button(
                    onClick = { onSelect(entry) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .background(Color(0xFF172232), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = entry.title.take(1).uppercase(),
                                color = Color(0xFF64B5F6),
                                style = MaterialTheme.typography.headlineLarge,
                            )
                            val logo = entry.logoUrl
                            if (!logo.isNullOrBlank()) {
                                AsyncImage(
                                    model = logo,
                                    contentDescription = "${entry.title} logo",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                        Text(
                            entry.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            entry.groupTitle ?: entry.category.displayName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFFB8C5D8),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun ContentCategory.displayName(): String =
    when (this) {
        ContentCategory.LIVE_TV -> "Live TV"
        ContentCategory.MOVIE -> "Movie"
        ContentCategory.SERIES -> "Series"
        ContentCategory.VIDEO -> "Video"
        ContentCategory.UNCATEGORIZED -> "Uncategorized"
    }
