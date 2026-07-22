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

    @Test
    fun providerAvatarPreviewsReturnTwoMostRecentProviders() {
        val older = EobRecord(
            id = 1,
            sourceName = "eob",
            providerName = "Old Clinic",
            insuranceName = "Aetna",
            serviceDate = "01/01/2025",
            serviceDateSortKey = 20250101,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
        val newer = EobRecord(
            id = 2,
            sourceName = "eob",
            providerName = "New Clinic",
            insuranceName = "Aetna",
            serviceDate = "06/01/2026",
            serviceDateSortKey = 20260601,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
        val middle = EobRecord(
            id = 3,
            sourceName = "eob",
            providerName = "Mid Clinic",
            insuranceName = "Aetna",
            serviceDate = "03/01/2026",
            serviceDateSortKey = 20260301,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = ""
        )
        val previews = EobAnalyzer.providerAvatarPreviews(
            listOf(older, newer, middle),
            AppLanguage.English,
            limit = 2
        )
        assertEquals(2, previews.size)
        assertEquals("NC", previews[0].initials)
        assertEquals("MC", previews[1].initials)
    }
}
