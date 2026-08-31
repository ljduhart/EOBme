package app.eob.me.network

import app.eob.me.data.NewsRelease
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object RssNewsMapper {
    const val BECKERS_COMPANY = "Becker's Payer Issues"
    const val HEALTHCARE_DIVE_COMPANY = "Healthcare Dive"

    const val BECKERS_RSS_URL = "https://www.beckerspayer.com/feed/"
    const val HEALTHCARE_DIVE_RSS_URL = "https://www.healthcaredive.com/feeds/news/"

    private val displayDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val sortableDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val pubDateParsers = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US)
    )

    private val liveNewsWindowStart: Calendar = Calendar.getInstance(Locale.US).apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun mapXmlFeed(company: String, xml: String): List<NewsRelease> {
        if (xml.isBlank()) return emptyList()
        return runCatching {
            parseRssItems(xml).mapNotNull { item ->
                mapParsedItem(company, item)
            }
        }.getOrElse { emptyList() }
    }

    fun isWithinLiveNewsWindow(sortableDate: String): Boolean {
        val parsed = runCatching { sortableDateFormat.parse(sortableDate) }.getOrNull() ?: return false
        val articleCal = Calendar.getInstance(Locale.US).apply { time = parsed }
        val endOfToday = Calendar.getInstance(Locale.US).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return !articleCal.before(liveNewsWindowStart) && !articleCal.after(endOfToday)
    }

    fun sortKey(date: String): String {
        return runCatching {
            sortableDateFormat.format(displayDateFormat.parse(date) ?: return date)
        }.getOrNull() ?: date
    }

    private data class ParsedRssItem(
        val title: String?,
        val pubDate: String?,
        val link: String?,
        val description: String?,
        val contentEncoded: String?
    )

    private fun mapParsedItem(company: String, item: ParsedRssItem): NewsRelease? {
        val headline = decodeHtmlEntities(item.title?.trim().orEmpty())
        if (headline.isBlank()) return null
        val parsedDate = parsePubDate(item.pubDate) ?: return null
        if (!isWithinLiveNewsWindow(parsedDate)) return null
        val summary = buildSummaryBody(item)
        val articleUrl = item.link?.trim().orEmpty()
        return NewsRelease(
            company = company,
            headline = headline,
            summary = summary,
            date = formatDisplayDate(parsedDate),
            targetTags = emptyList(),
            articleUrl = articleUrl
        )
    }

    private fun parseRssItems(xml: String): List<ParsedRssItem> {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val items = mutableListOf<ParsedRssItem>()
        var inItem = false
        var title: String? = null
        var link: String? = null
        var pubDate: String? = null
        var description: String? = null
        var contentEncoded: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName(parser)) {
                        "item" -> {
                            inItem = true
                            title = null
                            link = null
                            pubDate = null
                            description = null
                            contentEncoded = null
                        }
                        "title" -> if (inItem) title = readTagText(parser)
                        "link" -> if (inItem) link = readTagText(parser)
                        "pubdate" -> if (inItem) pubDate = readTagText(parser)
                        "description" -> if (inItem) description = readTagText(parser)
                        "content:encoded" -> if (inItem) contentEncoded = readTagText(parser)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (inItem && tagName(parser) == "item") {
                        items.add(
                            ParsedRssItem(
                                title = title,
                                pubDate = pubDate,
                                link = link,
                                description = description,
                                contentEncoded = contentEncoded
                            )
                        )
                        inItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }

    private fun tagName(parser: XmlPullParser): String {
        val prefix = parser.prefix
        val localName = parser.name?.lowercase(Locale.US).orEmpty()
        return if (!prefix.isNullOrBlank()) {
            "$prefix:$localName"
        } else {
            localName
        }
    }

    private fun readTagText(parser: XmlPullParser): String {
        return buildString {
            var event = parser.next()
            while (event != XmlPullParser.END_TAG && event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                        append(parser.text.orEmpty())
                    }
                }
                event = parser.next()
            }
        }.trim()
    }

    private fun parsePubDate(pubDate: String?): String? {
        val raw = pubDate?.trim().orEmpty()
        if (raw.isBlank()) return null
        pubDateParsers.forEach { parser ->
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val parsed = runCatching { parser.parse(raw) }.getOrNull()
            if (parsed != null) {
                return sortableDateFormat.format(parsed)
            }
        }
        return null
    }

    private fun formatDisplayDate(sortableDate: String): String {
        val parsed = sortableDateFormat.parse(sortableDate) ?: return sortableDate
        return displayDateFormat.format(parsed)
    }

    private fun buildSummaryBody(item: ParsedRssItem): String {
        return stripHtml(item.contentEncoded)
            .ifBlank { stripHtml(item.description) }
            .trim()
    }

    private fun stripHtml(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return decodeHtmlEntities(value)
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun decodeHtmlEntities(value: String): String {
        return value
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace(Regex("&#(\\d+);")) { match ->
                match.groupValues[1].toIntOrNull()?.let { codePoint ->
                    codePoint.toChar().toString()
                } ?: match.value
            }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
                match.groupValues[1].toIntOrNull(16)?.let { codePoint ->
                    codePoint.toChar().toString()
                } ?: match.value
            }
    }
}
