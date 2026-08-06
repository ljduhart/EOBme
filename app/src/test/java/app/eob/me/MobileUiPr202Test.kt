package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr202Test {
    @Test
    fun dxCptAssetUsesNativeOrgJsonOnly() {
        val assetCandidates = listOf(
            File("src/main/assets/dx_cpt_map.json"),
            File("app/src/main/assets/dx_cpt_map.json")
        )
        assertTrue(assetCandidates.any { it.isFile })
        val repoSource = readSource("data/repository/DxCptRepository.kt")
        assertTrue(repoSource.contains("org.json.JSONObject"))
        assertFalse(repoSource.contains("gson", ignoreCase = true))
        assertFalse(repoSource.contains("moshi", ignoreCase = true))
        assertFalse(repoSource.contains("kotlinx.serialization", ignoreCase = true))
        assertTrue(readSource("viewmodel/ReverseDxViewModel.kt").contains("ReverseDxRules"))
        assertTrue(readSource("data/dx/ReverseDxRules.kt").contains("MATCH_THRESHOLD"))
    }

    @Test
    fun reverseDxBottomSheetUsesMaterial3ModalBottomSheet() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val sheetSource = readSource("ui/components/dx/ReverseDxCptBottomSheet.kt")
        assertTrue(cardSource.contains("InsuranceCardDxReverseLookupLauncher"))
        assertTrue(cardSource.contains("onOpenReverseDxLookup"))
        assertTrue(cardSource.contains("InsuranceCardDxReverseLookupIcon"))
        assertTrue(navSource.contains("ReverseDxLookupSessionOverlay"))
        val overlayBlock = navSource.substringAfter("private fun ReverseDxLookupSessionOverlay")
            .substringBefore("@Composable\nprivate fun HistoryRoute")
        assertTrue(overlayBlock.contains("DisposableEffect(Unit)"))
        assertTrue(navSource.contains("reverseDxEngaged"))
        assertTrue(sheetSource.contains("import androidx.compose.material3.ModalBottomSheet"))
        assertTrue(sheetSource.contains("onLaunchScannerClicked"))
    }

    @Test
    fun reverseDxScannerRoutesToCameraCaptureFlow() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("fun launchEobScannerFromHub()"))
        assertTrue(navSource.contains("EobRoute.CameraCapture.route"))
        assertTrue(navSource.contains("BackHandler(enabled = reverseDxLookupVisible)"))
        assertTrue(navSource.contains("launchEobScannerFromHub()"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr202() {
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
