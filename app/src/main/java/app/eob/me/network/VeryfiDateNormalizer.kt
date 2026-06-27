package app.eob.me.network

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Normalizes Veryfi date strings to ISO-8601 calendar dates (`yyyy-MM-dd`).
 */
object VeryfiDateNormalizer {
    private val ISO_OUTPUT = DateTimeFormatter.ISO_LOCAL_DATE
    private val US_SLASH = DateTimeFormatter.ofPattern("M/d/uuuu", Locale.US)
    private val US_SLASH_TWO_YEAR = Regex("""^(\d{1,2})/(\d{1,2})/(\d{2})$""")
    private val ISO_INPUT = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")

    fun toIsoDate(raw: Any?): String {
        val trimmed = raw?.toString()?.trim().orEmpty()
        if (trimmed.isBlank()) return ""

        ISO_INPUT.find(trimmed)?.let { match ->
            return formatIso(
                year = match.groupValues[1].toInt(),
                month = match.groupValues[2].toInt(),
                day = match.groupValues[3].toInt()
            )
        }

        US_SLASH_TWO_YEAR.find(trimmed)?.let { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2].toInt()
            val yearSuffix = match.groupValues[3].toInt()
            val year = if (yearSuffix >= 70) 1900 + yearSuffix else 2000 + yearSuffix
            return formatIso(year = year, month = month, day = day)
        }

        return runCatching {
            val parsed = LocalDate.parse(trimmed, US_SLASH)
            parsed.format(ISO_OUTPUT)
        }.getOrElse {
            trimmed
        }
    }

    fun toDisplayDate(isoDate: String): String {
        if (isoDate.isBlank()) return "Date not recognized"
        return runCatching {
            val parsed = LocalDate.parse(isoDate, ISO_OUTPUT)
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US).format(parsed)
        }.getOrElse { isoDate }
    }

    private fun formatIso(year: Int, month: Int, day: Int): String {
        return runCatching {
            LocalDate.of(year, month, day).format(ISO_OUTPUT)
        }.getOrElse {
            throw DateTimeParseException("Invalid date components", "$year-$month-$day", 0)
        }
    }
}
