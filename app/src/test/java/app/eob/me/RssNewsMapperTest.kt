package app.eob.me

import app.eob.me.network.RssNewsMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RssNewsMapperTest {
    @Test
    fun mapXmlFeedBuildsNewsReleaseWithCompanyAndSortedDate() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Older item</title>
                  <link>https://example.com/older</link>
                  <pubDate>Wed, 14 Jan 2026 12:00:00 GMT</pubDate>
                  <description><![CDATA[<p>Older summary</p>]]></description>
                </item>
                <item>
                  <title>Newer item</title>
                  <link>https://example.com/newer</link>
                  <pubDate>Tue, 02 Jun 2026 08:30:00 GMT</pubDate>
                  <description><![CDATA[<p>Newer summary</p>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val mapped = RssNewsMapper.mapXmlFeed(RssNewsMapper.BECKERS_COMPANY, xml)
        assertEquals(2, mapped.size)
        assertEquals(
            setOf("Older item", "Newer item"),
            mapped.map { it.headline }.toSet()
        )
        assertTrue(mapped.all { it.company == RssNewsMapper.BECKERS_COMPANY })
        assertTrue(mapped.any { it.articleUrl == "https://example.com/newer" })
        assertTrue(mapped.all { !it.summary.contains("https://example.com") })
        assertTrue(mapped.all { it.date.contains("2026") })
    }

    @Test
    fun mapXmlFeedRejectsItemsOutsideLiveWindow() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <item>
                  <title>Legacy headline</title>
                  <link>https://example.com/legacy</link>
                  <pubDate>Mon, 15 Dec 2025 09:00:00 GMT</pubDate>
                  <description>Legacy</description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        assertTrue(RssNewsMapper.mapXmlFeed(RssNewsMapper.HEALTHCARE_DIVE_COMPANY, xml).isEmpty())
    }

    @Test
    fun mapXmlFeedReturnsEmptyForBlankXml() {
        assertTrue(RssNewsMapper.mapXmlFeed(RssNewsMapper.BECKERS_COMPANY, "").isEmpty())
    }

    @Test
    fun mapXmlFeedParsesContentEncodedSummary() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/">
              <channel>
                <item>
                  <title>Live payer headline</title>
                  <link>https://example.com/live</link>
                  <pubDate>2026-06-16 15:23:00</pubDate>
                  <content:encoded><![CDATA[<p>Live summary</p>]]></content:encoded>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val mapped = RssNewsMapper.mapXmlFeed(RssNewsMapper.HEALTHCARE_DIVE_COMPANY, xml)
        assertEquals(1, mapped.size)
        assertEquals("Live payer headline", mapped.first().headline)
        assertEquals("06/16/2026", mapped.first().date)
        assertEquals("Live summary", mapped.first().summary)
    }

    @Test
    fun isWithinLiveNewsWindowAcceptsJanuary2026ThroughToday() {
        assertTrue(RssNewsMapper.isWithinLiveNewsWindow("2026-01-01"))
        assertFalse(RssNewsMapper.isWithinLiveNewsWindow("2025-12-31"))
    }
}
