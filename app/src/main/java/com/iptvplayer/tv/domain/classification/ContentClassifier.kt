package com.iptvplayer.tv.domain.classification

import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry
import javax.inject.Inject

fun interface ContentClassifier {
    fun classify(entry: M3uEntry): ContentCategory
}

class DefaultContentClassifier @Inject constructor() : ContentClassifier {
    override fun classify(entry: M3uEntry): ContentCategory {
        val group = entry.groupTitle.orEmpty()
        val explicitType = TYPE_ATTRIBUTE.find(entry.rawExtInf)?.groupValues?.get(1)?.lowercase()

        when {
            explicitType.hasAny(MOVIE_TERMS) -> return ContentCategory.MOVIE
            explicitType.hasAny(SERIES_TERMS) -> return ContentCategory.SERIES
            explicitType.hasAny(LIVE_TERMS) -> return ContentCategory.LIVE_TV
            group.matchesAny(MOVIE_GROUPS) -> return ContentCategory.MOVIE
            group.matchesAny(SERIES_GROUPS) -> return ContentCategory.SERIES
            group.matchesAny(LIVE_GROUPS) -> return ContentCategory.LIVE_TV
            !entry.tvgId.isNullOrBlank() -> return ContentCategory.LIVE_TV
            VIDEO_EXTENSION.containsMatchIn(entry.streamUrl) -> return ContentCategory.VIDEO
            else -> return ContentCategory.UNCATEGORIZED
        }
    }

    private fun String?.hasAny(terms: Set<String>): Boolean =
        this != null && terms.any { term -> containsWord(term) }

    private fun String.matchesAny(patterns: List<Regex>): Boolean =
        patterns.any { it.containsMatchIn(this) }

    private fun String.containsWord(word: String): Boolean =
        Regex("""(?:^|[^a-z0-9])${Regex.escape(word)}(?:$|[^a-z0-9])""", RegexOption.IGNORE_CASE)
            .containsMatchIn(this)

    private companion object {
        val TYPE_ATTRIBUTE = Regex("""(?:^|\s)(?:content-type|content_type|type)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val MOVIE_TERMS = setOf("movie", "movies", "film", "films", "cinema")
        val SERIES_TERMS = setOf("series", "show", "shows", "episode", "episodes", "season", "anime")
        val LIVE_TERMS = setOf("live", "channel", "channels")
        val MOVIE_GROUPS = listOf(
            Regex("""\b(movie|movies|film|films|cinema)\b""", RegexOption.IGNORE_CASE),
            Regex("""\bvideo\s+on\s+demand\b""", RegexOption.IGNORE_CASE),
        )
        val SERIES_GROUPS = listOf(
            Regex("""\b(series|tv\s+shows?|episodes?|seasons?|anime)\b""", RegexOption.IGNORE_CASE),
        )
        val LIVE_GROUPS = listOf(
            Regex("""\b(live(\s+tv)?|tv\s+channels?|channels?)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(news|sports|radio)\b""", RegexOption.IGNORE_CASE),
        )
        val VIDEO_EXTENSION = Regex("""\.(mp4|mkv|avi|mov|webm|mpg|mpeg)(?:$|[?#])""", RegexOption.IGNORE_CASE)
    }
}
