package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EobAnalyzerBentoTest {
    @Test
    fun historyBentoSnapshotReturnsThreeMonthlyBuckets() {
        val snapshot = EobAnalyzer.historyBentoSnapshot(emptyList())
        assertEquals(3, snapshot.monthlySpend.size)
        assertEquals(4, snapshot.cornerstoneQuadrants.size)
    }

    @Test
    fun flaggedBillingErrorCountDetectsCriticalIssues() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Test Clinic
                99215 billed ${'$'}100.00 insurance paid ${'$'}0.00
                denied not covered
            """.trimIndent(),
            sourceName = "test",
            nextId = 1
        )
        val count = EobAnalyzer.flaggedBillingErrorCount(listOf(record))
        assertTrue(count >= 1)
        val flagged = EobAnalyzer.recordsWithFlaggedBillingErrors(listOf(record))
        assertTrue(flagged.isNotEmpty())
        assertTrue(
            EobAnalyzer.detectBillingIssues(record).any { it.severity != BillingIssueSeverity.Info }
        )
    }

    @Test
    fun providerAvatarPreviewsUseInitials() {
        val record = EobRecord(
            id = 1,
            sourceName = "eob",
            providerName = "Chen Medical",
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
        val previews = EobAnalyzer.providerAvatarPreviews(listOf(record), AppLanguage.English)
        assertEquals(1, previews.size)
        assertEquals("CM", previews.first().initials)
    }
}
