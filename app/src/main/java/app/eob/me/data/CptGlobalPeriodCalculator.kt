package app.eob.me.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class CptGlobalPeriodAlert(
    val cptCode: String,
    val globalDays: Int,
    val serviceDate: String,
    val expirationDate: String,
    val isActive: Boolean
)

data class CptGlobalPeriodWindow(
    val triggeringCode: String,
    val triggeringDescription: String,
    val serviceDate: String,
    val globalDays: Int,
    val startDate: LocalDate,
    val endDate: LocalDate
)

object CptGlobalPeriodCalculator {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US)

    fun globalPeriodAlertForCharge(
        charge: EobCharge,
        today: LocalDate = LocalDate.now()
    ): CptGlobalPeriodAlert? {
        val globalDays = CptGlobalPeriodMap.globalDaysFor(charge.cptCode) ?: return null
        val startDate = parseDisplayDate(charge.serviceDate) ?: return null
        val expirationDate = startDate.plusDays(globalDays.toLong())
        return CptGlobalPeriodAlert(
            cptCode = charge.cptCode,
            globalDays = globalDays,
            serviceDate = formatDisplayDate(startDate),
            expirationDate = formatDisplayDate(expirationDate),
            isActive = isDateWithinGlobalPeriod(startDate, expirationDate, today)
        )
    }

    fun globalPeriodAlertsForRecord(
        record: EobRecord,
        today: LocalDate = LocalDate.now()
    ): List<CptGlobalPeriodAlert> {
        return record.charges.mapNotNull { charge ->
            globalPeriodAlertForCharge(charge, today)?.takeIf { alert -> alert.isActive }
        }
    }

    fun buildGlobalPeriodWindows(records: List<EobRecord>): List<CptGlobalPeriodWindow> {
        return records
            .flatMap { record -> record.charges }
            .mapNotNull { charge -> windowForCharge(charge) }
    }

    fun billingIssuesFor(
        record: EobRecord,
        allRecords: List<EobRecord>
    ): List<BillingIssue> {
        val windows = buildGlobalPeriodWindows(allRecords)
        if (windows.isEmpty()) return emptyList()

        val issues = mutableListOf<BillingIssue>()
        record.charges.forEach { charge ->
            if (charge.category != CptCategory.OfficeVisit) return@forEach
            val visitDate = parseDisplayDate(charge.serviceDate) ?: return@forEach
            windows.forEach { window ->
                if (window.triggeringCode.equals(charge.cptCode, ignoreCase = true)) return@forEach
                if (!isDateWithinGlobalPeriod(window.startDate, window.endDate, visitDate)) return@forEach
                issues += BillingIssue(
                    type = BillingIssueType.VisitDuringGlobalPeriod,
                    severity = BillingIssueSeverity.Warning,
                    title = "Office visit during global surgical period",
                    explanation = "CPT ${charge.cptCode} on ${charge.serviceDate} falls within the " +
                        "${window.globalDays}-day global period for ${window.triggeringCode} " +
                        "(${window.triggeringDescription}) that started ${window.serviceDate}.",
                    recommendedAction = "Confirm whether this visit should be bundled into the surgical global " +
                        "payment instead of billed separately."
                )
            }
        }
        return issues.distinctBy { issue -> issue.explanation }
    }

    private fun windowForCharge(charge: EobCharge): CptGlobalPeriodWindow? {
        val entry = CptGlobalPeriodMap.entryFor(charge.cptCode) ?: return null
        val startDate = parseDisplayDate(charge.serviceDate) ?: return null
        val endDate = startDate.plusDays(entry.globalDays.toLong())
        return CptGlobalPeriodWindow(
            triggeringCode = entry.code,
            triggeringDescription = entry.procedureDescription,
            serviceDate = formatDisplayDate(startDate),
            globalDays = entry.globalDays,
            startDate = startDate,
            endDate = endDate
        )
    }

    fun parseDisplayDate(date: String): LocalDate? {
        val trimmed = date.trim()
        if (trimmed.isBlank() || trimmed == "Date not recognized") return null
        return try {
            LocalDate.parse(trimmed, displayDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatDisplayDate(date: LocalDate): String {
        return date.format(displayDateFormatter)
    }

    private fun isDateWithinGlobalPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
        targetDate: LocalDate
    ): Boolean {
        return !targetDate.isBefore(startDate) && !targetDate.isAfter(endDate)
    }
}
