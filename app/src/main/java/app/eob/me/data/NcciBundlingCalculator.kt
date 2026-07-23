package app.eob.me.data

import app.eob.me.data.ncci.NcciBundlingMap

data class NcciBundlingAlert(
    val columnOneCode: String,
    val columnTwoCode: String,
    val serviceDate: String,
    val isActive: Boolean = true
)

object NcciBundlingCalculator {
    fun bundlingAlertForChargePair(
        columnOneCharge: EobCharge,
        columnTwoCharge: EobCharge
    ): NcciBundlingAlert? {
        if (!columnOneCharge.serviceDate.equals(columnTwoCharge.serviceDate, ignoreCase = true)) {
            return null
        }
        val columnOne = columnOneCharge.cptCode.trim().uppercase()
        val columnTwo = columnTwoCharge.cptCode.trim().uppercase()
        if (columnOne.isBlank() || columnTwo.isBlank() || columnOne == columnTwo) {
            return null
        }
        val bundledCodes = NcciBundlingMap.bundledCodesFor(columnOne) ?: return null
        if (columnTwo !in bundledCodes) return null
        return NcciBundlingAlert(
            columnOneCode = columnOneCharge.cptCode,
            columnTwoCode = columnTwoCharge.cptCode,
            serviceDate = columnOneCharge.serviceDate,
            isActive = true
        )
    }

    fun bundlingAlertsForRecord(record: EobRecord): List<NcciBundlingAlert> {
        if (record.charges.size < 2) return emptyList()
        val alerts = mutableListOf<NcciBundlingAlert>()
        val chargesByDate = record.charges.groupBy { charge -> charge.serviceDate.trim() }
        chargesByDate.values.forEach { dateCharges ->
            for (columnOneIndex in dateCharges.indices) {
                for (columnTwoIndex in dateCharges.indices) {
                    if (columnOneIndex == columnTwoIndex) continue
                    val alert = bundlingAlertForChargePair(
                        columnOneCharge = dateCharges[columnOneIndex],
                        columnTwoCharge = dateCharges[columnTwoIndex]
                    ) ?: continue
                    alerts += alert
                }
            }
        }
        return alerts.distinctBy { alert ->
            "${alert.serviceDate}|${alert.columnOneCode}|${alert.columnTwoCode}"
        }
    }

    fun billingIssuesFor(record: EobRecord): List<BillingIssue> {
        return bundlingAlertsForRecord(record).map { alert ->
            BillingIssue(
                type = BillingIssueType.PossibleUnbundling,
                severity = BillingIssueSeverity.Warning,
                title = "Possible NCCI unbundling conflict",
                explanation = "CPT ${alert.columnOneCode} and CPT ${alert.columnTwoCode} on ${alert.serviceDate} " +
                    "may violate CMS NCCI procedure-to-procedure edits. The comprehensive code is typically " +
                    "eligible for payment while the bundled code should not be billed separately.",
                recommendedAction = "Confirm whether both services were distinct and medically necessary. " +
                    "If the bundled service was included in the comprehensive code, request a correction."
            )
        }
    }
}
