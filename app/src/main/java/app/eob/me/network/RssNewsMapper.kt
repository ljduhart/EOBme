package app.eob.me.network

import app.eob.me.data.NewsRelease
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object RssNewsMapper {
    const val BECKERS_COMPANY = "Becker's Payer Issues"
    const val HEALTHCARE_DIVE_COMPANY = "Healthcare Dive"

    const val BECKERS_RSS_URL = "https://www.beckerspayer.com/?format=feed&type=rss"
    const val HEALTHCARE_DIVE_RSS_URL = "https://www.healthcaredive.com/feeds/news/"

    private val displayDateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    private val sortableDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val pubDateParsers = listOf(
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

    fun mapResponse(company: String, response: RssResponse): List<NewsRelease> {
        if (!response.status.equals("ok", ignoreCase = true)) return emptyList()
        return response.items.orEmpty().mapNotNull { item ->
            mapItem(company, item)
        }
    }

    fun mapItem(company: String, item: RssItem): NewsRelease? {
        val headline = item.title?.trim().orEmpty()
        if (headline.isBlank()) return null
        val parsedDate = parsePubDate(item.pubDate) ?: return null
        if (!isWithinLiveNewsWindow(parsedDate)) return null
        val summary = buildSummary(item)
        return NewsRelease(
            company = company,
            headline = headline,
            summary = summary,
            date = formatDisplayDate(parsedDate),
            targetTags = emptyList()
        )
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

    private fun buildSummary(item: RssItem): String {
        val body = stripHtml(item.content)
            .ifBlank { stripHtml(item.description) }
            .trim()
        val link = item.link?.trim().orEmpty()
        return when {
            body.isNotBlank() && link.isNotBlank() -> "$body\n\n$link"
            body.isNotBlank() -> body
            link.isNotBlank() -> link
            else -> ""
        }
    }

    private fun stripHtml(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return value
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
