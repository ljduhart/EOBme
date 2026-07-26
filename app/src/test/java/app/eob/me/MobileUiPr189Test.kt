package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr189Test {
    @Test
    fun historyGroupsByProviderAndBilledAmount() {
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(analyzerSource.contains("fun groupHistoryByProvider"))
        assertTrue(analyzerSource.contains("compareByDescending<EobRecord> { it.totalBilledAmount }"))
        assertTrue(viewModelSource.contains("groupHistoryByProvider"))
    }

    @Test
    fun providerGroupingSortsHighestBilledFirstWithinProvider() {
        val lowBilled = sampleRecord(id = 1, provider = "Alpha Clinic", billed = 120.0, sortKey = 20260101)
        val highBilled = sampleRecord(id = 2, provider = "Alpha Clinic", billed = 480.0, sortKey = 20260201)
        val otherProvider = sampleRecord(id = 3, provider = "Beta Clinic", billed = 900.0, sortKey = 20260301)

        val sections = EobAnalyzer.groupHistoryByProvider(
            listOf(lowBilled, highBilled, otherProvider),
            app.eob.me.data.AppLanguage.English
        )

        assertEquals(2, sections.size)
        assertEquals("Alpha Clinic", sections[0].header)
        assertEquals(480.0, sections[0].rows[0].record.totalBilledAmount, 0.01)
        assertEquals(120.0, sections[0].rows[1].record.totalBilledAmount, 0.01)
        assertEquals("Beta Clinic", sections[1].header)
    }

    @Test
    fun eobHistoryShowsBilledOutsideAndHorizontalSelectedDetails() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("HistoryExteriorBilledAmount"))
        assertTrue(historySource.contains("HistorySelectedDetailStrip"))
        assertTrue(historySource.contains("LazyRow"))
        assertTrue(historySource.contains("HistoryDetailAmountChip"))
        assertFalse(historySource.contains("HistoryReceiptAmountBreakdown"))
        assertFalse(historySource.contains("private fun ReceiptAmountRow"))
    }

    @Test
    fun selectedDetailsHideZeroBalances() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("if (record.totalDeductibleAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalContractualAdjustmentAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalCopayAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalCoinsuranceAmount > 0.0)"))
        assertTrue(historySource.contains("if (record.totalPatientResponsibility > 0.0)"))
    }

    @Test
    fun mainActivityIsResizeableForTabletsWithoutOrientationLock() {
        val manifest = readProjectFile("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android:resizeableActivity=\"true\""))
        assertFalse(manifest.contains("screenOrientation"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr189() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("groupHistoryByProvider"))
        assertFalse(splashSource.contains("HistorySelectedDetailStrip"))
        assertFalse(introSource.contains("resizeableActivity"))
    }

    private fun sampleRecord(
        id: Int,
        provider: String,
        billed: Double,
        sortKey: Int
    ): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "test",
            providerName = provider,
            insuranceName = "Insurer",
            serviceDate = "01/01/2026",
            serviceDateSortKey = sortKey,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = "billed",
            totalBilledAmount = billed
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(File(relativePath), File("../$relativePath"))
        return candidates.first { it.isFile }.readText()
    }
}
