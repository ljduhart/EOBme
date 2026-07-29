package app.eob.me

import app.eob.me.data.EobStrings
import app.eob.me.data.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr195Test {
    @Test
    fun receiptPreviewShowsOnlyFourTaxVaultFields() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val receiptPreviewBlock = screenSource
            .substringAfter("private fun VaultEvidenceReceiptPreviewContent")
            .substringBefore("@Composable\nprivate fun VaultEvidenceDetailLine")
        assertTrue(receiptPreviewBlock.contains("taxVaultReceiptStoreName"))
        assertTrue(receiptPreviewBlock.contains("taxVaultReceiptRxNumber"))
        assertTrue(receiptPreviewBlock.contains("taxVaultReceiptDate"))
        assertTrue(receiptPreviewBlock.contains("taxVaultReceiptTotalSale"))
        assertFalse(receiptPreviewBlock.contains("patientResponsibility"))
        assertFalse(receiptPreviewBlock.contains("appointmentDate"))
    }

    @Test
    fun addReceiptButtonRequiresActiveTaxVaultFilter() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(screenSource.contains("if (filterState != TaxVaultFilterState.OFF)"))
        assertTrue(viewModelSource.contains("if (!isTaxVaultActive() || !isTaxVaultGoldUnlocked()) return"))
        assertTrue(viewModelSource.contains("rxNumber = parsed.rxNumber"))
    }

    @Test
    fun helpfulInsightSevenAndElevenUseRequestedVerbiage() {
        val stringsSource = readSource("data/EobStrings.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        assertTrue(
            stringsSource.contains(
                "For Gold tier users, the Tax Vault Filter can be toggle off by pressing the lit green highlighted card to the left of the Tax Vault Filter title, in the Tax Vault Dashboard."
            )
        )
        assertTrue(
            stringsSource.contains(
                "EOBme is design to detect and alert for possible Billing Errors, Upcoding, Unbundling, and Global Grace Period."
            )
        )
        assertTrue(settingsSource.contains("\"settingsHelpfulHint11\""))
        assertEquals(
            "EOBme is design to detect and alert for possible Billing Errors, Upcoding, Unbundling, and Global Grace Period.",
            EobStrings.t(AppLanguage.English, "settingsHelpfulHint11")
        )
    }

    @Test
    fun miniatureReceiptPolaroidShowsRxInsteadOfPlaceholderItems() {
        val uiSource = readSource("ui/components/taxvault/TaxVaultEvidenceUi.kt")
        assertTrue(uiSource.contains("RX ${'$'}{thumbnail.rxNumber}"))
        assertFalse(uiSource.contains("ITEM .............."))
        assertTrue(uiSource.contains("TOTAL SALE"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr195() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("taxVaultReceiptRxNumber"))
        assertFalse(splashSource.contains("settingsHelpfulHint11"))
        assertFalse(introSource.contains("VaultReceiptMapper"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
