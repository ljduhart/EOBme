package app.eob.me

import app.eob.me.data.CameraScanDocumentType
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCaptureScreenTest {
    @Test
    fun cameraCaptureScreenUsesFullScreenImmersiveDock() {
        val source = readSource("ui/screens/CameraCaptureScreen.kt")
        assertTrue(source.contains("fun CameraCaptureScreen"))
        assertTrue(source.contains("CameraScanDock"))
        assertTrue(source.contains("CameraScanTypeSelector"))
        assertTrue(source.contains("LastCaptureThumbnail"))
        assertTrue(source.contains("CameraTopUtilityBar"))
        assertTrue(source.contains("cameraScanTypeEob"))
        assertTrue(source.contains("cameraScanTypeReceipt"))
        assertTrue(source.contains("cameraBordersNotFound"))
        assertFalse(source.contains("ScannerControlBar"))
        assertFalse(source.contains("QR CODE"))
        assertFalse(source.contains("ID CARD"))
    }

    @Test
    fun cameraRouteDelegatesScanTypeToEobViewModel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("selectedScanType = uiState.cameraScanDocumentType"))
        assertTrue(navSource.contains("setCameraScanDocumentType"))
        assertTrue(navSource.contains("cameraScanSourceLabel"))
        assertTrue(viewModelSource.contains("fun setCameraScanDocumentType"))
        assertTrue(viewModelSource.contains("fun cameraScanSourceLabel"))
    }

    @Test
    fun eobViewModelCameraScanSourceLabelReflectsSelectedType() {
        val viewModel = EobViewModel()
        viewModel.setCameraScanDocumentType(CameraScanDocumentType.Eob)
        assertEquals("EOB", viewModel.cameraScanSourceLabel(app.eob.me.data.AppLanguage.English))
        viewModel.setCameraScanDocumentType(CameraScanDocumentType.Receipt)
        assertEquals("Receipt", viewModel.cameraScanSourceLabel(app.eob.me.data.AppLanguage.English))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
