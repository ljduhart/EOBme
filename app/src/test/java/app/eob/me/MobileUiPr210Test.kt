package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr210Test {
    @Test
    fun hubScanEobAndVaultReceiptUseGmsDocumentScanner() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("fun launchDocumentScanner()"))
        assertTrue(navSource.contains("GmsDocumentScannerLauncher.buildScanRequest"))
        assertTrue(navSource.contains("GmsDocumentScannerLauncher.parseScanResult"))
        assertTrue(navSource.contains("HubBottomTab.ScanEob ->"))
        assertTrue(navSource.contains("launchEobScannerFromHub()"))
        assertTrue(navSource.contains("if (eobViewModel.beginVaultReceiptScan())"))
        assertTrue(navSource.contains("launchDocumentScanner()"))
        assertFalse(navSource.contains("customCameraPermissionLauncher"))
    }

    @Test
    fun gmsScannerResultRoutesThroughEobViewModelPipeline() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("processVaultReceiptScannedDocument("))
        assertTrue(navSource.contains("processScannedDocument("))
        assertTrue(navSource.contains("cameraScanSourceLabel(language)"))
        assertTrue(navSource.contains("vaultReceiptScanPending"))
        assertTrue(navSource.contains("navController.navigate(EobRoute.TaxVault.route)"))
        assertTrue(navSource.contains("navController.navigate(EobRoute.History.route)"))
        assertTrue(navSource.contains("DocumentProcessingOverlay"))
        assertTrue(viewModelSource.contains("fun processScannedDocument"))
        assertTrue(viewModelSource.contains("fun processVaultReceiptScannedDocument"))
    }

    @Test
    fun gmsScannerLauncherRetainsUriPermissions() {
        val launcherSource = readSource("scanner/GmsDocumentScannerLauncher.kt")
        assertTrue(launcherSource.contains("fun parseScanResult(context: Context"))
        assertTrue(launcherSource.contains("retainReadPermission"))
        assertTrue(launcherSource.contains("GmsDocumentScanningResult.fromActivityResultIntent"))
        assertTrue(launcherSource.contains("SCANNER_MODE_FULL"))
        assertTrue(launcherSource.contains("RESULT_FORMAT_JPEG"))
        assertTrue(launcherSource.contains("RESULT_FORMAT_PDF"))
    }

    @Test
    fun documentScanCancelClearsVaultReceiptPending() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val cancelBlock = viewModelSource.substringAfter("fun onDocumentScanCancelled()")
            .substringBefore("fun onDocumentScanLaunchFailed")
        assertTrue(cancelBlock.contains("clearVaultReceiptScanPending()"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr210() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("GmsDocumentScannerLauncher"))
        assertFalse(pipelineSource.contains("launchDocumentScanner"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
