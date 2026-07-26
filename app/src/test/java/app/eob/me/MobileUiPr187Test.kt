package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr187Test {
    @Test
    fun eobViewModelExposesDuplicateScanWarningStateAndActions() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("val duplicateEobWarningState: StateFlow<DuplicateEobWarningState?>"))
        assertTrue(viewModelSource.contains("fun evaluateNewScan(newEobData: EobRecord"))
        assertTrue(viewModelSource.contains("fun onDiscardDuplicateScan()"))
        assertTrue(viewModelSource.contains("fun onOverwriteDuplicateScan()"))
        assertTrue(viewModelSource.contains("extractHybridScannedDocument"))
        assertTrue(viewModelSource.contains("persistHybridScannedDocument"))
    }

    @Test
    fun duplicateEobDialogIsHostedAtNavLevel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val dialogSource = readSource("ui/components/DuplicateEobWarningDialog.kt")
        assertFalse(homeSource.contains("AlertDialog"))
        assertFalse(homeSource.contains("duplicateEobWarningState"))
        assertTrue(dialogSource.contains("DuplicateEobWarningDialog"))
        assertTrue(dialogSource.contains("duplicateEobDialogTitle"))
        assertTrue(navSource.contains("DuplicateEobWarningDialog("))
        assertTrue(navSource.contains("duplicateEobWarningState.collectAsStateWithLifecycle()"))
    }

    @Test
    fun eobHistoryShowsCopayDeductibleCoinsuranceAtRecordLevelWhenSelected() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(historySource.contains("HistorySelectedDetailStrip"))
        assertTrue(historySource.contains("record.totalCopayAmount.asCurrency()"))
        assertTrue(historySource.contains("record.totalDeductibleAmount.asCurrency()"))
        assertTrue(historySource.contains("record.totalCoinsuranceAmount.asCurrency()"))
        assertFalse(historySource.contains("ReceiptChargeDetailRows"))
        assertFalse(historySource.contains("charge.copayAmount.asCurrency()"))
    }

    @Test
    fun duplicateScanMatchUsesClaimIdOrProviderAndDate() {
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        assertTrue(analyzerSource.contains("fun findDuplicateScanMatch"))
        assertTrue(analyzerSource.contains("fun claimIdForRecord"))
    }

    @Test
    fun protectedOpeningScreensAndPipelineRemainMinimalForPr187() {
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        val ytdSource = readSource("ui/components/bento/YtdExpenseBentoCell.kt")
        assertFalse(splashSource.contains("duplicateEobDialogTitle"))
        assertFalse(introSource.contains("ReceiptChargeDetailRows"))
        assertFalse(ytdSource.contains("DuplicateEobWarningState"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
