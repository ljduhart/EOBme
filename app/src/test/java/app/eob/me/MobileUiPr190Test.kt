package app.eob.me

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr190Test {
    @Test
    fun eobHistoryUsesVerticalBreakdownMatchingReferenceLayout() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("HistoryPatientResponsibilityHeader"))
        assertTrue(historySource.contains("HistoryReceiptAmountBreakdown"))
        assertTrue(historySource.contains("private fun ReceiptAmountRow"))
        assertTrue(historySource.contains("Arrangement.SpaceBetween"))
        assertFalse(historySource.contains("HistorySelectedDetailStrip"))
        assertFalse(historySource.contains("HistoryExteriorBilledAmount"))
        assertTrue(
            historySource.indexOf("HistoryReceiptAmountBreakdown") <
                historySource.indexOf("HistoryAppealPillButtons")
        )
    }

    @Test
    fun addReceiptButtonIsFifteenPercentSmallerAndCentered() {
        val uiSource = readSource("ui/components/taxvault/TaxVaultEvidenceUi.kt")
        val buttonBlock = uiSource.substringAfter("fun VaultAddReceiptButton")
            .substringBefore("private fun VaultAddReceiptDocumentCameraIcon")
        assertTrue(uiSource.contains("AddReceiptButtonWidth = 81.6.dp"))
        assertTrue(uiSource.contains("AddReceiptButtonHeight = 91.8.dp"))
        assertTrue(buttonBlock.contains("Arrangement.Center"))
        assertFalse(buttonBlock.contains("Arrangement.SpaceBetween"))
    }

    @Test
    fun providerDirectoryBentoUsesSharedHubAspectRatioAfterPr191() {
        val layoutSource = readSource("ui/components/bento/BentoCellLayout.kt")
        val gridSource = readSource("ui/components/bento/BentoGridCell.kt")
        assertFalse(layoutSource.contains("PROVIDER_DIRECTORY_ASPECT_RATIO"))
        assertTrue(gridSource.contains("cellAspectRatio = cellAspectRatio"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr190() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("HistoryReceiptAmountBreakdown"))
        assertFalse(splashSource.contains("PROVIDER_DIRECTORY_ASPECT_RATIO"))
        assertFalse(introSource.contains("AddReceiptButtonWidth"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
