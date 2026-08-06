package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr201Test {
    @Test
    fun dxCptAssetRepositoryAndViewModelPresent() {
        val assetCandidates = listOf(
            File("src/main/assets/dx_cpt_map.json"),
            File("app/src/main/assets/dx_cpt_map.json")
        )
        assertTrue(assetCandidates.any { it.isFile })
        assertTrue(readSource("data/repository/DxCptRepository.kt").contains("dx_cpt_map.json"))
        assertTrue(readSource("viewmodel/ReverseDxViewModel.kt").contains("ReverseDxSearchState"))
        assertTrue(readSource("viewmodel/ReverseDxViewModel.kt").contains("MATCH_THRESHOLD"))
    }

    @Test
    fun reverseDxBottomSheetWiredFromInsuranceCardHub() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val sheetSource = readSource("ui/components/dx/ReverseDxCptBottomSheet.kt")
        assertTrue(cardSource.contains("InsuranceCardDxReverseLookupLauncher"))
        assertTrue(cardSource.contains("onOpenReverseDxLookup"))
        assertTrue(cardSource.contains("InsuranceCardDxReverseLookupIcon"))
        assertTrue(navSource.contains("ReverseDxLookupSessionOverlay"))
        assertTrue(navSource.contains("reverseDxEngaged"))
        assertTrue(sheetSource.contains("ModalBottomSheet"))
        assertTrue(sheetSource.contains("onLaunchScannerClicked"))
    }

    @Test
    fun reverseDxScannerRoutesToCameraCaptureFlow() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("fun launchEobScannerFromHub()"))
        assertTrue(navSource.contains("EobRoute.CameraCapture.route"))
        assertTrue(navSource.contains("reverseDxLookupVisible = false"))
        assertTrue(navSource.contains("launchEobScannerFromHub()"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr201() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("DxCptRepository"))
        assertFalse(pipelineSource.contains("ReverseDxViewModel"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
