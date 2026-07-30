package app.eob.me.data

object BillingIssueFormatter {
    fun title(language: AppLanguage, issue: BillingIssue): String {
        return EobStrings.t(language, issue.type.titleKey())
    }

    fun expenseAnalyticsSummary(language: AppLanguage, issues: List<BillingIssue>): String {
        return issues
            .filter { issue -> issue.severity != BillingIssueSeverity.Info }
            .distinctBy { issue -> issue.type }
            .joinToString(separator = "; ") { issue -> title(language, issue) }
    }

    fun BillingIssueType.titleKey(): String = when (this) {
        BillingIssueType.DuplicateCharge -> "billingIssueTitleDuplicateCharge"
        BillingIssueType.MathMismatch -> "billingIssueTitleMathMismatch"
        BillingIssueType.MissingInsurancePayment -> "billingIssueTitleMissingInsurancePayment"
        BillingIssueType.HighPatientResponsibility -> "billingIssueTitleHighPatientResponsibility"
        BillingIssueType.MissingProvider -> "billingIssueTitleMissingProvider"
        BillingIssueType.MissingInsurance -> "billingIssueTitleMissingInsurance"
        BillingIssueType.MissingDateOfService -> "billingIssueTitleMissingDateOfService"
        BillingIssueType.MissingCptCode -> "billingIssueTitleMissingCptCode"
        BillingIssueType.PossibleDenial -> "billingIssueTitlePossibleDenial"
        BillingIssueType.VisitDuringGlobalPeriod -> "billingIssueTitleVisitDuringGlobalPeriod"
        BillingIssueType.PossibleUnbundling -> "billingIssueTitlePossibleUnbundling"
        BillingIssueType.SuspectedUpcoding -> "billingIssueTitleSuspectedUpcoding"
    }
}
