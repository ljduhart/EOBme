package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr211Test {
    @Test
    fun gmsScannerRequestsJpegOnlySinglePage() {
        val launcherSource = readSource("scanner/GmsDocumentScannerLauncher.kt")
        assertTrue(launcherSource.contains(".setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)"))
        assertFalse(launcherSource.contains("RESULT_FORMAT_PDF"))
        assertTrue(launcherSource.contains(".setPageLimit(1)"))
        assertFalse(launcherSource.contains(".setPageLimit(5)"))
    }

    @Test
    fun gmsScannerResultUsesFirstPageJpegUriOnly() {
        val launcherSource = readSource("scanner/GmsDocumentScannerLauncher.kt")
        assertTrue(launcherSource.contains("val imageUri = scanResult.pages?.firstOrNull()?.imageUri"))
        assertFalse(launcherSource.contains("scanResult.pdf"))
        assertTrue(launcherSource.contains("return imageUri"))
    }

    @Test
    fun navHostPassesParsedJpegUriIntoEobViewModelPipeline() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val resultBlock = navSource.substringAfter("val documentScannerLauncher = rememberLauncherForActivityResult")
            .substringBefore("fun launchDocumentScanner()")
        assertTrue(resultBlock.contains("GmsDocumentScannerLauncher.parseScanResult"))
        assertTrue(resultBlock.contains("if (scannedUri != null)"))
        assertTrue(resultBlock.contains("eobViewModel.processScannedDocument("))
        assertTrue(resultBlock.contains("uri = scannedUri"))
        assertTrue(resultBlock.contains("eobViewModel.processVaultReceiptScannedDocument("))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr211() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("RESULT_FORMAT_JPEG"))
        assertFalse(pipelineSource.contains("GmsDocumentScannerLauncher"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
