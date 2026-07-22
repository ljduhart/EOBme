package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobCharge
import app.eob.me.data.UpcodingVerificationCalculator
import app.eob.me.data.UpcodingVerificationMap
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr180Test {
    @Test
    fun upcodingVerificationMapContainsHighLevelEmCodes() {
        val mapSource = readSource("data/UpcodingVerificationMap.kt")
        assertTrue(mapSource.contains("\"99204\" to \"45-59 minutes\""))
        assertTrue(mapSource.contains("\"99205\" to \"60-74 minutes\""))
        assertTrue(mapSource.contains("\"99214\" to \"30-39 minutes\""))
        assertTrue(mapSource.contains("\"99215\" to \"40-54 minutes\""))
        assertEquals("40-54 minutes", UpcodingVerificationMap.requiredTimeFor("99215"))
    }

    @Test
    fun calculatorReturnsRequiredTimeForMatchingCharge() {
        val charge = EobCharge(
            cptCode = "99214",
            cptDescription = "Office visit",
            category = CptCategory.OfficeVisit,
            billedAmount = 180.0,
            insurancePaidAmount = 0.0,
            contractualAdjustmentAmount = 0.0,
            copayAmount = 0.0,
            deductibleAmount = 0.0,
            coinsuranceAmount = 0.0,
            serviceDate = "01/15/2026"
        )
        val alert = UpcodingVerificationCalculator.upcodingVerificationForCharge(charge)
        assertNotNull(alert)
        assertEquals("30-39 minutes", alert?.requiredTimeRange)
        assertTrue(alert?.isActive == true)
    }

    @Test
    fun eobViewModelRemainsSourceOfTruthForUpcodingVerification() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("fun upcodingVerificationForCharge"))
        assertTrue(viewModelSource.contains("fun upcodingVerificationsForRecord"))
        assertTrue(viewModelSource.contains("UpcodingVerificationCalculator"))
    }

    @Test
    fun historyScreenShowsUpcodingVerificationBubbleWithWarningStyling() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(historySource.contains("UpcodingVerificationBubble"))
        assertTrue(historySource.contains("AnimatedVisibility"))
        assertTrue(historySource.contains("Icons.Rounded.Warning"))
        assertTrue(historySource.contains("errorContainer"))
        assertTrue(historySource.contains("onErrorContainer"))
        assertTrue(historySource.contains("historyUpcodingVerification"))
        assertTrue(historySource.contains("historyUpcodingYes"))
        assertTrue(historySource.contains("historyUpcodingNo"))
        assertTrue(historySource.contains("OutlinedButton"))
        assertTrue(navSource.contains("upcodingVerificationForCharge = eobViewModel::upcodingVerificationForCharge"))
    }

    @Test
    fun cptTrackerExcludesZeroBilledCharges() {
        val extractorSource = readSource("data/BentoSnapshotExtractor.kt")
        assertTrue(extractorSource.contains("filter { it.billedAmount > 0.0 }"))

        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Clinic
                Aetna
                01/15/2026
                99213 billed ${'$'}0.00
                99214 billed ${'$'}150.00
            """.trimIndent(),
            sourceName = "test",
            nextId = 1
        )
        val entries = app.eob.me.data.BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.OfficeVisit
        )
        assertEquals(1, entries.size)
        assertEquals("99214", entries.first().code)
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr180() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("UpcodingVerificationBubble"))
        assertFalse(veryfiSource.contains("upcodingVerificationRules"))
        assertFalse(splashSource.contains("historyUpcodingVerification"))
        assertFalse(introSource.contains("UpcodingVerificationCalculator"))
    }

    @Test
    fun viewModelFacadeReturnsNullForNonUpcodingCodes() {
        val viewModel = EobViewModel()
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\n99213 billed ${'$'}120.00",
            sourceName = "test",
            nextId = 2
        )
        val charge = record.charges.first()
        assertNull(viewModel.upcodingVerificationForCharge(record, charge))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
