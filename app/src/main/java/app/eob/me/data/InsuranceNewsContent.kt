package app.eob.me.data

import java.util.Calendar
import java.util.Locale

data class InsuranceArticle(
    val id: String,
    val carrier: MajorInsuranceCarrier,
    val monthIndex: Int,
    val monthLabel: String,
    val year: Int,
    val headline: String,
    val body: String
)

enum class MajorInsuranceCarrier(val displayName: String, val hubShortName: String) {
    UnitedHealthcare("United Healthcare", "UHC"),
    Medicare("Medicare", "Medicare"),
    Aetna("Aetna", "Aetna"),
    BlueCross("Blue Cross", "Blue Cross"),
    Medicaid("Medicaid", "Medicaid");

    fun filterKeywords(): List<String> = when (this) {
        UnitedHealthcare -> listOf("United", "UHC", "UnitedHealthcare")
        Medicare -> listOf("Medicare", "CMS", "Humana")
        Aetna -> listOf("Aetna", "CVS")
        BlueCross -> listOf("Blue Cross", "BCBS", "Blue Cross Blue Shield")
        Medicaid -> listOf("Medicaid", "CHIP")
    }
}

object EobInsuranceNews {
    fun articlesForYear(
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        throughMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    ): List<InsuranceArticle> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year != currentYear) return emptyList()
        val lastMonth = throughMonth.coerceIn(Calendar.JANUARY, Calendar.DECEMBER)
        return MajorInsuranceCarrier.entries.flatMap { carrier ->
            (Calendar.JANUARY..lastMonth).map { monthIndex ->
                val monthLabel = monthName(monthIndex)
                InsuranceArticle(
                    id = "${carrier.name}_${year}_$monthIndex",
                    carrier = carrier,
                    monthIndex = monthIndex,
                    monthLabel = monthLabel,
                    year = year,
                    headline = headlineFor(carrier, monthLabel, year),
                    body = bodyFor(carrier, monthLabel, year)
                )
            }
        }
    }

    fun articlesForCarrier(
        carrier: MajorInsuranceCarrier,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): List<InsuranceArticle> {
        return articlesForYear(year).filter { it.carrier == carrier }
    }

    private fun monthName(monthIndex: Int): String {
        val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, monthIndex) }
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US) ?: "Month"
    }

    private fun headlineFor(carrier: MajorInsuranceCarrier, month: String, year: Int): String {
        return when (carrier) {
            MajorInsuranceCarrier.UnitedHealthcare ->
                "$month $year: Coverage updates and member cost tools"
            MajorInsuranceCarrier.Medicare ->
                "$month $year: Medicare benefits and enrollment reminders"
            MajorInsuranceCarrier.Aetna ->
                "$month $year: Care navigation and plan transparency"
            MajorInsuranceCarrier.BlueCross ->
                "$month $year: BCBS network and preventive care focus"
            MajorInsuranceCarrier.Medicaid ->
                "$month $year: Medicaid eligibility and care access"
        }
    }

    private fun bodyFor(carrier: MajorInsuranceCarrier, month: String, year: Int): String {
        val intro = when (carrier) {
            MajorInsuranceCarrier.UnitedHealthcare ->
                "United Healthcare published member-facing guidance for $month $year highlighting digital tools, prior authorization transparency, and pharmacy benefit updates."
            MajorInsuranceCarrier.Medicare ->
                "Medicare communications for $month $year emphasize preventive services, Part B cost sharing, and Medicare Advantage plan comparison resources."
            MajorInsuranceCarrier.Aetna ->
                "Aetna's $month $year updates focus on connected care programs, behavioral health access, and clearer EOB explanations for commercial members."
            MajorInsuranceCarrier.BlueCross ->
                "Blue Cross plans shared $month $year updates on network adequacy, value-based care partnerships, and member advocacy for billing accuracy."
            MajorInsuranceCarrier.Medicaid ->
                "Medicaid program notices for $month $year cover renewal windows, care coordination, and community provider network changes."
        }
        return buildString {
            appendLine(intro)
            appendLine()
            appendLine("Key themes this month:")
            appendLine("• Review your Explanation of Benefits for duplicate charges or missing contractual adjustments.")
            appendLine("• Confirm in-network providers before scheduled visits to avoid surprise bills.")
            appendLine("• Use your member portal to track deductible and out-of-pocket progress.")
            appendLine()
            append("This summary is provided for educational purposes inside EOBme and is not an official carrier statement.")
        }
    }
}
