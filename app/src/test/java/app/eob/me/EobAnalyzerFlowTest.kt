package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EobAnalyzerFlowTest {
    @Test
    fun normalizeScanCollapsesWhitespace() {
        val normalized = EobAnalyzer.normalizeScan("Line1\t\tLine2\n\n\nLine3")
        assertFalse(normalized.contains('\t'))
        assertFalse(normalized.contains("\n\n\n"))
    }

    @Test
    fun compactDuplicateEobsKeepsSingleCopy() {
        val record = sampleRecord(id = 1)
        val duplicate = record.copy(id = 2, sourceName = "dup")
        val compacted = EobAnalyzer.compactDuplicateEobs(listOf(record, duplicate))
        assertEquals(1, compacted.size)
        assertEquals(1, compacted.first().id)
    }

    @Test
    fun isSameEobMatchesProviderInsuranceDateAndCpt() {
        val first = sampleRecord(id = 1)
        val second = first.copy(id = 2, sourceName = "other")
        assertTrue(EobAnalyzer.isSameEob(first, second))
        assertFalse(EobAnalyzer.isSameEob(first, second.copy(providerName = "Other Provider")))
    }

    @Test
    fun providerDirectoryAggregatesByProvider() {
        val a = sampleRecord(id = 1, provider = "Alpha Medical")
        val b = sampleRecord(id = 2, provider = "Alpha Medical")
        val directory = EobAnalyzer.providerDirectory(listOf(a, b))
        assertEquals(1, directory.size)
        assertEquals(2, directory.first().eobCount)
    }

    @Test
    fun yearlyHealthCostSummaryTotalsYearRecords() {
        val record = sampleRecord(id = 1, serviceDate = "01/15/2026")
        val summary = EobAnalyzer.yearlyHealthCostSummary(listOf(record), preferredYear = 2026)
        assertEquals(2026, summary.year)
        assertEquals(1, summary.eobCount)
        assertTrue(summary.totalBilled > 0.0)
    }

    @Test
    fun cptUsageGroupsByCodeForYear() {
        val record = sampleRecord(id = 1).copy(
            charges = listOf(
                charge("99213"),
                charge("99213")
            )
        )
        val usage = EobAnalyzer.cptUsage(listOf(record), 2026)
        assertTrue(usage.any { it.info.code == "99213" && it.count >= 2 })
    }

    @Test
    fun accuracyReviewFlagsShortOcrText() {
        val record = sampleRecord(id = 1).copy(rawText = "short")
        val review = EobAnalyzer.accuracyReview(record)
        assertTrue(review.warnings.any { it.contains("OCR", ignoreCase = true) })
    }

    @Test
    fun validCptCodesExtractsFromText() {
        val codes = EobAnalyzer.validCptCodes("Office visit 99213 and lab 80053")
        assertTrue(codes.contains("99213"))
        assertTrue(codes.contains("80053"))
    }

    @Test
    fun recordSignalsOutOfNetworkFromVeryfiBalanceField() {
        val record = sampleRecord(id = 1).copy(
            rawText = """{"provider_name":"North Clinic","out_of_network_out_of_pocket_balance":2100}"""
        )
        assertTrue(EobAnalyzer.recordSignalsOutOfNetwork(record))
    }

    @Test
    fun providerDirectoryTrimsProviderNamesForGrouping() {
        val a = sampleRecord(id = 1, provider = "Alpha Medical ").copy(firestoreId = "a")
        val b = sampleRecord(id = 2, provider = "Alpha Medical").copy(firestoreId = "b")
        val directory = EobAnalyzer.providerDirectory(listOf(a, b))
        assertEquals(1, directory.size)
        assertEquals(2, directory.first().eobCount)
    }

    private fun sampleRecord(
        id: Int,
        provider: String = "Flow Clinic",
        serviceDate: String = "01/15/2026"
    ) = EobAnalyzer.analyze(
        rawText = """
            Provider: $provider
            Aetna
            $serviceDate
            99213 billed ${'$'}150.00 insurance paid ${'$'}90.00 contractual adjustment ${'$'}20.00
        """.trimIndent(),
        sourceName = "flow-$id",
        nextId = id
    )

    private fun charge(code: String) = EobCharge(
        cptCode = code,
        cptDescription = "Test",
        category = CptCategory.OfficeVisit,
        billedAmount = 100.0,
        insurancePaidAmount = 50.0,
        contractualAdjustmentAmount = 10.0,
        copayAmount = 5.0,
        deductibleAmount = 0.0,
        coinsuranceAmount = 0.0,
        serviceDate = "01/15/2026"
    )
}
