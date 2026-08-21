package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr207Test {
    @Test
    fun coilBitmapLoaderUsesCoilExecuteWithBoundedSize() {
        val loaderSource = readSource("util/CoilBitmapLoader.kt")
        assertTrue(loaderSource.contains("object CoilBitmapLoader"))
        assertTrue(loaderSource.contains("context.imageLoader.execute(request)"))
        assertTrue(loaderSource.contains(".size(maxDimension)"))
        assertTrue(loaderSource.contains("allowHardware(false)"))
        assertFalse(loaderSource.contains("import android.graphics.BitmapFactory"))
        assertFalse(loaderSource.contains("BitmapFactory.decode"))
    }

    @Test
    fun taxVaultClaimPackagerUsesCoilForRemoteEvidence() {
        val packagerSource = readSource("data/TaxVaultClaimPackager.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(packagerSource.contains("suspend fun buildClaimPackage"))
        assertTrue(packagerSource.contains("CoilBitmapLoader.loadBitmapFromUrl"))
        assertFalse(packagerSource.contains("BitmapFactory"))
        assertFalse(packagerSource.contains("HttpURLConnection"))
        assertTrue(viewModelSource.contains("private suspend fun buildTaxVaultClaimPackage"))
    }

    @Test
    fun cameraCaptureViewModelUsesCoilForCapturedFiles() {
        val viewModelSource = readSource("viewmodel/CameraCaptureViewModel.kt")
        assertTrue(viewModelSource.contains("CoilBitmapLoader.loadBitmapFromFile"))
        assertTrue(viewModelSource.contains("maxDimension = compression.maxDimension"))
        assertFalse(viewModelSource.contains("BitmapFactory"))
    }

    @Test
    fun coilDependencyDeclaredInGradle() {
        val gradleSource = readProjectFile("app/build.gradle.kts")
        val versionsSource = readProjectFile("gradle/libs.versions.toml")
        assertTrue(gradleSource.contains("libs.coil.compose"))
        assertTrue(gradleSource.contains("libs.coil"))
        assertTrue(versionsSource.contains("coil = \"2.7.0\""))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr207() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("CoilBitmapLoader"))
        assertFalse(pipelineSource.contains("TaxVaultClaimPackager"))
    }

    @Test
    fun navigationPathsRemainSyncedForCameraAndTaxVaultExport() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val cameraSource = readSource("ui/screens/CameraCaptureScreen.kt")
        assertTrue(navSource.contains("exportTaxVaultClaimPackage(context)"))
        assertTrue(navSource.contains("onExportClaimPackage"))
        assertTrue(navSource.contains("eobViewModel.clearVaultReceiptScanPending()"))
        assertTrue(navSource.contains("navController.popBackStack()"))
        assertTrue(navSource.contains("popUpTo(EobRoute.CameraCapture.route) { inclusive = true }"))
        assertTrue(navSource.contains("EobRoute.TaxVault.route"))
        assertTrue(navSource.contains("EobRoute.History.route"))
        assertTrue(cameraSource.contains("viewModel.handleCapturedFile"))
    }

    @Test
    fun coilLoaderDisablesCacheSoRecycledBitmapsStaySafe() {
        val loaderSource = readSource("util/CoilBitmapLoader.kt")
        assertTrue(loaderSource.contains("memoryCachePolicy(CachePolicy.DISABLED)"))
        assertTrue(loaderSource.contains("diskCachePolicy(CachePolicy.DISABLED)"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readProjectFile(relativePath: String): String {
        val candidates = listOf(
            File(relativePath),
            File("../$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
