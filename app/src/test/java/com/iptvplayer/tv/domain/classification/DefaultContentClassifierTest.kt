package com.iptvplayer.tv.domain.classification

import com.iptvplayer.tv.domain.model.ContentCategory
import com.iptvplayer.tv.domain.model.M3uEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultContentClassifierTest {
    private val classifier = DefaultContentClassifier()

    @Test
    fun `recognizes movie group conventions`() {
        assertEquals(ContentCategory.MOVIE, classifier.classify(entry(group = "VOD | Movies")))
    }

    @Test
    fun `recognizes series groups`() {
        assertEquals(ContentCategory.SERIES, classifier.classify(entry(group = "TV Series")))
    }

    @Test
    fun `recognizes live channel groups and EPG ids`() {
        assertEquals(ContentCategory.LIVE_TV, classifier.classify(entry(group = "Live TV")))
        assertEquals(ContentCategory.LIVE_TV, classifier.classify(entry(group = "News", tvgId = "news.1")))
    }

    @Test
    fun `recognizes progressive video without assuming it is a movie`() {
        assertEquals(ContentCategory.VIDEO, classifier.classify(entry(url = "https://example.com/title.mp4")))
    }

    @Test
    fun `does not classify HLS by extension alone`() {
        assertEquals(
            ContentCategory.UNCATEGORIZED,
            classifier.classify(entry(url = "https://example.com/title.m3u8")),
        )
    }

    @Test
    fun `uses explicit movie group instead of HLS extension`() {
        assertEquals(
            ContentCategory.MOVIE,
            classifier.classify(entry(group = "Movies", url = "https://example.com/title.m3u8")),
        )
    }

    @Test
    fun `keeps ambiguous content uncategorized`() {
        assertEquals(ContentCategory.UNCATEGORIZED, classifier.classify(entry(group = "Documentaries")))
    }

    private fun entry(
        group: String? = null,
        tvgId: String? = null,
        url: String = "https://example.com/stream",
    ) = M3uEntry(
        id = "test",
        title = "Test",
        streamUrl = url,
        tvgId = tvgId,
        groupTitle = group,
        rawExtInf = "#EXTINF:-1,Test",
    )
}
