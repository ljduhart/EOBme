package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr177Test {
    @Test
    fun cptGlobalPeriodMapAndCalculatorExistWithLocalLookup() {
        val mapSource = readSource("data/CptGlobalPeriodMap.kt")
        val calculatorSource = readSource("data/CptGlobalPeriodCalculator.kt")
        assertTrue(mapSource.contains("CptGlobalPeriodEntry(\"27447\", 90"))
        assertTrue(mapSource.contains("CptGlobalPeriodEntry(\"12001\", 10"))
        assertTrue(calculatorSource.contains("CptGlobalPeriodMap.globalDaysFor"))
        assertTrue(calculatorSource.contains("plusDays"))
        assertTrue(calculatorSource.contains("VisitDuringGlobalPeriod"))
    }

    @Test
    fun knowledgeBaseSyncsGlobalPeriodCodesToHospitalCategory() {
        val knowledgeSource = readSource("data/EobKnowledgeBase.kt")
        assertTrue(knowledgeSource.contains("CptGlobalPeriodMap.entryFor"))
        assertTrue(knowledgeSource.contains("CptCategory.Hospital"))
        assertTrue(knowledgeSource.contains("99214"))
        assertTrue(knowledgeSource.contains("CptCategory.OfficeVisit"))
    }

    @Test
    fun eobViewModelRemainsSourceOfTruthForGlobalPeriodProcessing() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("fun globalPeriodAlertForCharge"))
        assertTrue(viewModelSource.contains("fun globalPeriodAlertsForRecord"))
        assertTrue(viewModelSource.contains("fun detectBillingIssuesForRecord"))
        assertTrue(viewModelSource.contains("CptGlobalPeriodCalculator"))
    }

    @Test
    fun historyScreenShowsAnimatedGlobalPeriodThoughtBubble() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(historySource.contains("GlobalPeriodThoughtBubble"))
        assertTrue(historySource.contains("AnimatedVisibility"))
        assertTrue(historySource.contains("Icons.Rounded.Lightbulb"))
        assertTrue(historySource.contains("surfaceVariant"))
        assertTrue(historySource.contains("historyGlobalPeriodAlert"))
        assertTrue(navSource.contains("globalPeriodAlertForCharge = eobViewModel::globalPeriodAlertForCharge"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr177() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("CptGlobalPeriodMap"))
        assertFalse(veryfiSource.contains("GlobalPeriodThoughtBubble"))
        assertFalse(splashSource.contains("CptGlobalPeriodCalculator"))
        assertFalse(introSource.contains("historyGlobalPeriodAlert"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
