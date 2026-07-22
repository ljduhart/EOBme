package app.eob.me

import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr181Test {
    @Test
    fun ncciBundlingMapsAreSplitBySpecialtyAndAggregated() {
        val masterSource = readSource("data/ncci/NcciBundlingMap.kt")
        val emSource = readSource("data/ncci/NcciEmBundlingMap.kt")
        val labSource = readSource("data/ncci/NcciLabBundlingMap.kt")
        val surgerySource = readSource("data/ncci/NcciSurgeryBundlingMap.kt")
        val radiologySource = readSource("data/ncci/NcciRadiologyBundlingMap.kt")
        val specialtySource = readSource("data/ncci/NcciSpecialtyBundlingMap.kt")

        assertTrue(emSource.contains("object NcciEmBundlingMap"))
        assertTrue(labSource.contains("object NcciLabBundlingMap"))
        assertTrue(surgerySource.contains("object NcciSurgeryBundlingMap"))
        assertTrue(radiologySource.contains("object NcciRadiologyBundlingMap"))
        assertTrue(specialtySource.contains("object NcciSpecialtyBundlingMap"))
        assertTrue(masterSource.contains("val ncciBundlingRules"))
        assertTrue(masterSource.contains("NcciEmBundlingMap.rules"))
        assertTrue(masterSource.contains("NcciLabBundlingMap.rules"))
        assertTrue(masterSource.contains("NcciSurgeryBundlingMap.rules"))
        assertTrue(masterSource.contains("NcciRadiologyBundlingMap.rules"))
        assertTrue(masterSource.contains("NcciSpecialtyBundlingMap.rules"))
        assertTrue(masterSource.contains("fun bundledCodesFor"))
    }

    @Test
    fun eobViewModelRemainsSourceOfTruthForNcciBundling() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val calculatorSource = readSource("data/NcciBundlingCalculator.kt")
        assertTrue(viewModelSource.contains("fun bundlingAlertForCharges"))
        assertTrue(viewModelSource.contains("fun bundlingAlertsForRecord"))
        assertTrue(viewModelSource.contains("NcciBundlingCalculator.billingIssuesFor(record)"))
        assertTrue(calculatorSource.contains("object NcciBundlingCalculator"))
        assertTrue(calculatorSource.contains("BillingIssueType.PossibleUnbundling"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr181() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("NcciBundlingMap"))
        assertFalse(veryfiSource.contains("ncciBundlingRules"))
        assertFalse(splashSource.contains("PossibleUnbundling"))
        assertFalse(introSource.contains("NcciBundlingCalculator"))
    }

    @Test
    fun viewModelFacadeDetectsUnbundlingThroughBillingIssues() {
        val viewModel = EobViewModel()
        val record = app.eob.me.data.EobAnalyzer.analyze(
            rawText = """
                Provider: Clinic
                Aetna
                01/15/2026
                99213 billed $120.00
                36415 billed $15.00
            """.trimIndent(),
            sourceName = "test",
            nextId = 3
        )
        val issues = viewModel.detectBillingIssuesForRecord(record)
        assertTrue(issues.any { it.type == app.eob.me.data.BillingIssueType.PossibleUnbundling })
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
