package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobCharge
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr188Test {
    @Test
    fun chargesWithBillableAmountsOmitsZeroBilledLines() {
        val charges = listOf(
            sampleCharge("99213", 150.0),
            sampleCharge("99214", 0.0),
            sampleCharge("36415", 12.5)
        )
        val filtered = EobAnalyzer.chargesWithBillableAmounts(charges)
        assertEquals(2, filtered.size)
        assertEquals(listOf("99213", "36415"), filtered.map { it.cptCode })
    }

    @Test
    fun compactDuplicateEobsSanitizesZeroBilledCharges() {
        val record = EobRecord(
            id = 1,
            sourceName = "test",
            providerName = "Provider",
            insuranceName = "Insurer",
            serviceDate = "01/01/2026",
            serviceDateSortKey = 20260101,
            charges = listOf(
                sampleCharge("99213", 100.0),
                sampleCharge("99214", 0.0)
            ),
            duplicateChargeWarnings = emptyList(),
            rawText = "billed"
        )
        val compacted = EobAnalyzer.compactDuplicateEobs(listOf(record))
        assertEquals(1, compacted.single().charges.size)
        assertEquals("99213", compacted.single().charges.single().cptCode)
    }

    @Test
    fun eobHistoryShowsVerticalSelectedDetailsWithPatientResponsibilityHeader() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("HistoryPatientResponsibilityHeader"))
        assertTrue(historySource.contains("HistoryReceiptAmountBreakdown"))
        assertTrue(historySource.contains("ReceiptAmountRow"))
        assertTrue(historySource.contains("if (record.totalCopayAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalDeductibleAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalCoinsuranceAmount > 0.0)"))
        assertTrue(historySource.contains("EobAnalyzer.chargesWithBillableAmounts(record.charges)"))
        assertTrue(historySource.contains("if (lineCount > 0)"))
        assertFalse(historySource.contains("ReceiptChargeDetailRows"))
        assertFalse(historySource.contains("charge.billedAmount.asCurrency()"))
        assertFalse(historySource.contains("HistorySelectedDetailStrip"))
        assertFalse(historySource.contains("HistoryExteriorBilledAmount"))
    }

    @Test
    fun insuranceBridgeFiltersZeroBilledServiceLines() {
        val bridgeSource = readSource("data/InsuranceEobRecordBridge.kt")
        assertTrue(bridgeSource.contains("EobAnalyzer.chargesWithBillableAmounts"))
    }

    @Test
    fun smartCardsRetainDoubleTapDialWhenCompleteWithPhone() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertTrue(careTeamSource.contains("onDoubleTap = {"))
        assertTrue(careTeamSource.contains("cardState.isCompleteWithPhone"))
        assertTrue(careTeamSource.contains("DeviceCallingUtils.safelyDialNumber"))
        assertTrue(careTeamSource.contains("Intent.ACTION_DIAL").not())
        assertTrue(careTeamSource.contains("safelyDialNumber"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr188() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("chargesWithBillableAmounts"))
        assertFalse(veryfiSource.contains("HistoryReceiptAmountBreakdown"))
        assertFalse(splashSource.contains("chargesWithBillableAmounts"))
        assertFalse(introSource.contains("HistoryReceiptAmountBreakdown"))
    }

    private fun sampleCharge(code: String, billedAmount: Double): EobCharge {
        return EobCharge(
            cptCode = code,
            cptDescription = "Procedure",
            category = CptCategory.OfficeVisit,
            billedAmount = billedAmount,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = "01/01/2026"
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
