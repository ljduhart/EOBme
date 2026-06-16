package app.eob.me

import app.eob.me.network.RssItem
import app.eob.me.network.RssNewsMapper
import app.eob.me.network.RssResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RssNewsMapperTest {
    @Test
    fun mapResponseBuildsNewsReleaseWithCompanyAndSortedDate() {
        val response = RssResponse(
            status = "ok",
            feed = null,
            items = listOf(
                RssItem(
                    title = "Older item",
                    pubDate = "Wed, 14 Jan 2026 12:00:00 GMT",
                    link = "https://example.com/older",
                    content = "<p>Older summary</p>",
                    description = null
                ),
                RssItem(
                    title = "Newer item",
                    pubDate = "Tue, 02 Jun 2026 08:30:00 GMT",
                    link = "https://example.com/newer",
                    content = "<p>Newer summary</p>",
                    description = null
                )
            )
        )

        val mapped = RssNewsMapper.mapResponse(RssNewsMapper.BECKERS_COMPANY, response)
        assertEquals(2, mapped.size)
        assertEquals(
            setOf("Older item", "Newer item"),
            mapped.map { it.headline }.toSet()
        )
        assertTrue(mapped.all { it.company == RssNewsMapper.BECKERS_COMPANY })
        assertTrue(mapped.any { it.summary.contains("https://example.com/newer") })
        assertTrue(mapped.all { it.date.contains("2026") })
    }

    @Test
    fun mapResponseRejectsItemsOutsideLiveWindow() {
        val response = RssResponse(
            status = "ok",
            feed = null,
            items = listOf(
                RssItem(
                    title = "Legacy headline",
                    pubDate = "Mon, 15 Dec 2025 09:00:00 GMT",
                    link = "https://example.com/legacy",
                    content = "Legacy",
                    description = null
                )
            )
        )

        assertTrue(RssNewsMapper.mapResponse(RssNewsMapper.HEALTHCARE_DIVE_COMPANY, response).isEmpty())
    }

    @Test
    fun mapResponseReturnsEmptyWhenStatusIsNotOk() {
        val response = RssResponse(status = "error", feed = null, items = emptyList())
        assertTrue(RssNewsMapper.mapResponse(RssNewsMapper.BECKERS_COMPANY, response).isEmpty())
    }

    @Test
    fun mapResponseParsesRss2JsonPubDateFormat() {
        val response = RssResponse(
            status = "ok",
            feed = null,
            items = listOf(
                RssItem(
                    title = "Live payer headline",
                    pubDate = "2026-06-16 15:23:00",
                    link = "https://example.com/live",
                    content = "<p>Live summary</p>",
                    description = null
                )
            )
        )

        val mapped = RssNewsMapper.mapResponse(RssNewsMapper.HEALTHCARE_DIVE_COMPANY, response)
        assertEquals(1, mapped.size)
        assertEquals("Live payer headline", mapped.first().headline)
        assertEquals("06/16/2026", mapped.first().date)
    }

    @Test
    fun isWithinLiveNewsWindowAcceptsJanuary2026ThroughToday() {
        assertTrue(RssNewsMapper.isWithinLiveNewsWindow("2026-01-01"))
        assertFalse(RssNewsMapper.isWithinLiveNewsWindow("2025-12-31"))
    }
}
