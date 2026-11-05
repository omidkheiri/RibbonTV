package com.iptvplayer.tv.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.iptvplayer.tv.R
import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry

object TvColors {
    val Background = Color(0xFF07070D)
    val Surface = Color(0xFF12111B)
    val SurfaceRaised = Color(0xFF1B1928)
    val Purple = Color(0xFF8B5CF6)
    val PurpleBright = Color(0xFFA78BFA)
    val TextPrimary = Color(0xFFF7F5FF)
    val TextSecondary = Color(0xFFA9A6B4)
    val Error = Color(0xFFFF8A9A)
}

enum class HomeTab(val label: String) {
    FOR_YOU("For You"), LIVE("Live"), MOVIES("Movies"), SERIES("Series")
}

@Composable
fun Modifier.tvFocusModifier(
    focused: Boolean,
    cornerRadius: Int = 16,
    focusedScale: Float = 1.055f,
): Modifier {
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "tv-card-scale")
    val border by animateColorAsState(if (focused) TvColors.PurpleBright else Color.Transparent, label = "tv-card-border")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
        .shadow(if (focused) 18.dp else 0.dp, RoundedCornerShape(cornerRadius.dp), clip = false)
        .border(if (focused) 3.dp else 1.dp, border, RoundedCornerShape(cornerRadius.dp))
}

@Composable
fun TvTopNavigationBar(
    selected: HomeTab,
    onSelected: (HomeTab) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE607070D),
                        Color(0x9907070D),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(start = 28.dp, end = 26.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Navigation Tabs (Left-aligned near left safe margin)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HomeTab.entries.forEach { tab ->
                    GoogleTvNavItem(
                        label = tab.label,
                        selected = selected == tab,
                        onClick = { onSelected(tab) },
                    )
                }
            }

            // Push actions to the far right
            Spacer(Modifier.weight(1f))

            // 2. Search and Settings icon actions (Right-aligned, circular dark translucent)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoogleTvIconButton(
                    drawableResId = R.drawable.ic_search,
                    contentDescription = "Search",
                    onClick = onSearch,
                )
                GoogleTvIconButton(
                    drawableResId = R.drawable.ic_settings,
                    contentDescription = "Settings",
                    onClick = onSettings,
                )
            }
        }
    }
}

@Composable
private fun GoogleTvNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "tab-scale")

    val backgroundColor = when {
        selected -> Color(0xFFE8EAED) // Google TV light off-white pill
        focused -> Color(0x33FFFFFF)
        else -> Color.Transparent
    }

    val textColor = when {
        selected -> Color(0xFF1F1F24) // Dark charcoal
        focused -> Color.White
        else -> Color(0xFFC4C7C5) // Google TV inactive gray
    }

    val borderModifier = when {
        focused && selected -> Modifier.border(2.dp, Color.White, CircleShape)
        focused -> Modifier.border(1.5.dp, Color(0x99FFFFFF), CircleShape)
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .height(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(borderModifier)
            .clip(CircleShape)
            .background(backgroundColor)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun GoogleTvIconButton(
    drawableResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "icon-scale")

    val backgroundColor = if (focused) Color(0x66FFFFFF) else Color(0x3D000000)
    val borderModifier = if (focused) {
        Modifier.border(2.dp, Color.White, CircleShape)
    } else {
        Modifier.border(1.dp, Color(0x24FFFFFF), CircleShape)
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(borderModifier)
            .clip(CircleShape)
            .background(backgroundColor)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = drawableResId),
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun FeaturedHero(entry: M3uEntry?, onWatch: (M3uEntry) -> Unit, onDetails: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier.fillMaxWidth().height(300.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF25154A), Color(0xFF12111B), Color(0xFF090910)))),
    ) {
        if (entry?.logoUrl != null) {
            AsyncImage(
                model = entry.logoUrl,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.58f),
                contentScale = ContentScale.Crop,
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF207070D), Color(0x9907070D), Color.Transparent))))
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 44.dp, end = 28.dp).fillMaxWidth(0.56f),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("FEATURED", color = TvColors.PurpleBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(entry?.title ?: "Your entertainment, beautifully organized", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(
                entry?.groupTitle ?: "Add a playlist in Settings to bring your live channels, movies, and series together.",
                color = TvColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeroButton("▶  Watch Now", primary = true, enabled = entry != null) { entry?.let(onWatch) }
                if (entry != null && onDetails != null) HeroButton("ⓘ  Details", primary = false, onClick = onDetails)
            }
        }
    }
}

@Composable
private fun HeroButton(label: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.tvFocusModifier(focused, cornerRadius = 12, focusedScale = 1.05f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) TvColors.Purple else Color(0xCC302D3A))
            .onFocusChanged { focused = it.isFocused }
            .clickable(enabled = enabled, onClick = onClick).focusable(enabled)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(label, color = if (enabled) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ContentRow(title: String, items: List<M3uEntry>, live: Boolean = false, onSelect: (M3uEntry) -> Unit) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = TvColors.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 12.dp),
        ) {
            items(items, key = M3uEntry::id) { entry ->
                if (live) LiveChannelCard(entry, onSelect) else MovieCard(entry, onSelect)
            }
        }
    }
}

@Composable
fun MovieCard(entry: M3uEntry, onSelect: (M3uEntry) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.width(210.dp).tvFocusModifier(focused).clip(RoundedCornerShape(16.dp))
            .background(TvColors.Surface).onFocusChanged { focused = it.isFocused }
            .clickable { onSelect(entry) }.focusable().padding(bottom = 14.dp),
    ) {
        Artwork(entry, Modifier.fillMaxWidth().height(270.dp), ContentScale.Crop)
        Text(entry.title, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text(entry.groupTitle ?: entry.category.displayName(), modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), color = TvColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SeriesCard(entry: M3uEntry, onSelect: (M3uEntry) -> Unit, modifier: Modifier = Modifier) = MovieCard(entry, onSelect, modifier)

@Composable
fun LiveChannelCard(entry: M3uEntry, onSelect: (M3uEntry) -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.width(260.dp).tvFocusModifier(focused).clip(RoundedCornerShape(16.dp))
            .background(TvColors.Surface).onFocusChanged { focused = it.isFocused }
            .clickable { onSelect(entry) }.focusable().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box {
            Artwork(entry, Modifier.fillMaxWidth().height(132.dp), ContentScale.Fit)
            Text("LIVE", modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE11D48)).padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
        Text(entry.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Artwork(entry: M3uEntry, modifier: Modifier, contentScale: ContentScale) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(Color(0xFF2A2140), Color(0xFF171420)))), contentAlignment = Alignment.Center) {
        Text(entry.title.take(1).uppercase(), color = TvColors.PurpleBright, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        if (!entry.logoUrl.isNullOrBlank()) {
            AsyncImage(model = entry.logoUrl, contentDescription = "${entry.title} artwork", modifier = Modifier.fillMaxSize(), contentScale = contentScale)
        }
    }
}

@Composable
fun ContentGrid(
    items: List<M3uEntry>,
    emptyMessage: String,
    onSelect: (M3uEntry) -> Unit,
    modifier: Modifier = Modifier,
    live: Boolean = false,
) {
    if (items.isEmpty()) {
        EmptyContentState("Nothing here yet", emptyMessage, modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(if (live) 270.dp else 210.dp),
            modifier = modifier.padding(horizontal = 4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            items(items, key = M3uEntry::id) { entry ->
                if (live) LiveChannelCard(entry, onSelect, Modifier.fillMaxWidth()) else MovieCard(entry, onSelect, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun EmptyContentState(title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(TvColors.SurfaceRaised), contentAlignment = Alignment.Center) {
                Text("＋", color = TvColors.PurpleBright, style = MaterialTheme.typography.displaySmall)
            }
            Text(title, color = TvColors.TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(message, color = TvColors.TextSecondary, style = MaterialTheme.typography.bodyLarge)
            if (actionLabel != null && onAction != null) HeroButton(actionLabel, primary = true, onClick = onAction)
        }
    }
}

@Composable
fun ContinueWatchingCard(entry: M3uEntry, progress: Float, onResume: (M3uEntry) -> Unit) {
    // Reserved for real persisted progress. The home screen intentionally hides this until that data exists.
    MovieCard(entry, onResume)
}

fun ContentCategory.displayName(): String = when (this) {
    ContentCategory.LIVE_TV -> "Live TV"
    ContentCategory.MOVIE -> "Movie"
    ContentCategory.SERIES -> "Series"
    ContentCategory.VIDEO -> "Video"
    ContentCategory.UNCATEGORIZED -> "Uncategorized"
}
