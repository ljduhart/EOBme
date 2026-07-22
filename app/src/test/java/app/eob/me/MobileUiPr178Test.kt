package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr178Test {
    @Test
    fun appTargetsAndroid16Api36() {
        val buildGradle = readProjectFile("app/build.gradle.kts")
        assertTrue(buildGradle.contains("compileSdk = 36"))
        assertTrue(buildGradle.contains("targetSdk = 36"))
    }

    @Test
    fun mainActivityUsesEdgeToEdgeSafeDrawingInsets() {
        val mainActivitySource = readSource("MainActivity.kt")
        assertTrue(mainActivitySource.contains("enableEdgeToEdge()"))
        assertTrue(mainActivitySource.contains("contentWindowInsets = WindowInsets.safeDrawing"))
        assertTrue(mainActivitySource.contains("consumeWindowInsets(innerPadding)"))
    }

    @Test
    fun hubScaffoldsDeclareExplicitWindowInsetsForAndroid16() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val vaultSource = readSource("ui/screens/TaxVaultScreen.kt")
        assertTrue(navSource.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(historySource.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
        assertTrue(vaultSource.contains("contentWindowInsets = WindowInsets(0, 0, 0, 0)"))
    }

    @Test
    fun predictiveBackUsesDispatcherNotLegacyApis() {
        val manifest = readProjectFile("app/src/main/AndroidManifest.xml")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(manifest.contains("android:enableOnBackInvokedCallback=\"true\""))
        assertTrue(navSource.contains("BackHandler"))
        assertFalse(projectSourcesContain("onBackPressed()"))
        assertFalse(projectSourcesContain("KEYCODE_BACK"))
        assertFalse(projectSourcesContain("elegantTextHeight"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr178() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertFalse(pipelineSource.contains("contentWindowInsets"))
        assertFalse(veryfiSource.contains("WindowInsets.safeDrawing"))
        assertFalse(splashSource.contains("contentWindowInsets"))
        assertFalse(introSource.contains("contentWindowInsets"))
        assertFalse(viewModelSource.contains("targetSdk = 36"))
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

    private fun projectSourcesContain(token: String): Boolean {
        val roots = listOf(
            File("src/main/java/app/eob/me"),
            File("app/src/main/java/app/eob/me")
        )
        return roots
            .first { it.isDirectory }
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .any { it.readText().contains(token) }
    }
}
