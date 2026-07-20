package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr176Test {
    @Test
    fun settingsHelpfulInsightsIncludeHintsNineAndTen() {
        val stringsSource = readSource("data/EobStrings.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        assertTrue(stringsSource.contains("settingsHelpfulHint9"))
        assertTrue(stringsSource.contains("settingsHelpfulHint10"))
        assertTrue(
            stringsSource.contains(
                "you can manually adjust the edges to encompass the entire document"
            )
        )
        assertTrue(
            stringsSource.contains(
                "toggle the top right corner of the Insurance card to reveal shortcuts"
            )
        )
        assertFalse(stringsSource.contains("(number 9"))
        assertFalse(stringsSource.contains("(number 10"))
        assertTrue(settingsSource.contains("\"settingsHelpfulHint9\""))
        assertTrue(settingsSource.contains("\"settingsHelpfulHint10\""))
    }

    @Test
    fun ytdGaugeCardsUseEqualSizedLayout() {
        val ytdSource = readSource("ui/screens/YtdExpenseScreen.kt")
        assertTrue(ytdSource.contains("height(IntrinsicSize.Max)"))
        assertTrue(ytdSource.contains("fillMaxHeight()"))
        assertTrue(ytdSource.contains("ytdDeductibleMet"))
        assertTrue(ytdSource.contains("ytdOopMaxProgress"))
        assertTrue(ytdSource.contains(".height(44.dp)"))
        assertTrue(ytdSource.contains(".height(36.dp)"))
    }

    @Test
    fun documentProcessingScannerUsesCyberBlueLaser() {
        val overlaySource = readSource("ui/components/DocumentProcessingOverlay.kt")
        assertTrue(overlaySource.contains("EobCyberAccent"))
        assertTrue(overlaySource.contains("EobCyberGlow"))
        assertFalse(overlaySource.contains("LaserRed"))
        assertFalse(overlaySource.contains("0xFFFF2D2D"))
    }

    @Test
    fun protectedPipelineAndViewModelRemainUntouchedForPr176() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("settingsHelpfulHint9"))
        assertFalse(veryfiSource.contains("EobCyberAccent"))
        assertFalse(viewModelSource.contains("settingsHelpfulHint10"))
        assertFalse(splashSource.contains("settingsHelpfulHint9"))
        assertFalse(introSource.contains("settingsHelpfulHint10"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
