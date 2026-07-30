package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingIssue
import app.eob.me.data.BillingIssueFormatter
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.BillingIssueType
import app.eob.me.data.EobStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingIssueFormatterTest {
    @Test
    fun expenseAnalyticsSummaryUsesLocalizedTitles() {
        val issues = listOf(
            BillingIssue(
                type = BillingIssueType.PossibleUnbundling,
                severity = BillingIssueSeverity.Warning,
                title = "ignored",
                explanation = "",
                recommendedAction = ""
            ),
            BillingIssue(
                type = BillingIssueType.SuspectedUpcoding,
                severity = BillingIssueSeverity.Warning,
                title = "ignored",
                explanation = "",
                recommendedAction = ""
            )
        )
        val summary = BillingIssueFormatter.expenseAnalyticsSummary(AppLanguage.English, issues)
        assertTrue(summary.contains(EobStrings.t(AppLanguage.English, "billingIssueTitlePossibleUnbundling")))
        assertTrue(summary.contains(EobStrings.t(AppLanguage.English, "billingIssueTitleSuspectedUpcoding")))
    }

    @Test
    fun titleMapsIssueTypeToStringKey() {
        val issue = BillingIssue(
            type = BillingIssueType.SuspectedUpcoding,
            severity = BillingIssueSeverity.Warning,
            title = "raw",
            explanation = "",
            recommendedAction = ""
        )
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingIssueTitleSuspectedUpcoding"),
            BillingIssueFormatter.title(AppLanguage.English, issue)
        )
    }
}
