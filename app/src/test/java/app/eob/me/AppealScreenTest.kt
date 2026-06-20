package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppealScreenTest {
    @Test
    fun appealScreenContainsPrintReadyDocumentPatterns() {
        val source = readSource("ui/screens/AppealScreen.kt")
        assertTrue(source.contains("fun AppealScreen"))
        assertTrue(source.contains("AnimatedContent"))
        assertTrue(source.contains("slideInHorizontally"))
        assertTrue(source.contains("slideOutHorizontally"))
        assertTrue(source.contains("AnimatedVisibility"))
        assertTrue(source.contains("expandVertically"))
        assertTrue(source.contains("shrinkVertically"))
        assertTrue(source.contains("SingleChoiceSegmentedButtonRow"))
        assertTrue(source.contains("DoctorDisputeStrategySelector"))
        assertTrue(source.contains("FilterChip"))
        assertTrue(source.contains("fun AppealActionBar"))
        assertTrue(source.contains("AppealRegenerateHeroButton"))
        assertTrue(source.contains("Color.White"))
        assertTrue(source.contains("RoundedCornerShape(4.dp)"))
        assertTrue(source.contains("aspectRatio(8.5f / 11f)"))
        assertTrue(source.contains("FontFamily.Serif"))
        assertTrue(source.contains("AppealCanvasBackground"))
        assertTrue(source.contains("CircleShape"))
        assertTrue(source.contains("Brush.linearGradient"))
    }

    @Test
    fun appealRouteDelegatesToViewModelAppealState() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("AppealScreen"))
        assertTrue(navSource.contains("selectedAppealTarget"))
        assertTrue(navSource.contains("selectedDisputeStrategy"))
        assertTrue(navSource.contains("onAppealTargetSwitched"))
        assertTrue(navSource.contains("onDisputeStrategySwitched"))
        assertTrue(navSource.contains("regenerateAppeal"))
        assertTrue(navSource.contains("enableAppealLetterEditing"))
        assertTrue(navSource.contains("saveAppealLetter"))
        assertTrue(navSource.contains("updateAppeal"))
        assertTrue(viewModelSource.contains("fun regenerateAppeal"))
        assertTrue(viewModelSource.contains("fun onAppealTargetSwitched"))
        assertTrue(viewModelSource.contains("fun onDisputeStrategySwitched"))
        assertTrue(viewModelSource.contains("fun updateAppeal"))
    }

    @Test
    fun appealActionBarWiresEditSaveCopySendAndShare() {
        val source = readSource("ui/screens/AppealScreen.kt")
        assertTrue(source.contains("appealEditLetter"))
        assertTrue(source.contains("appealSaveLetter"))
        assertTrue(source.contains("appealCopy"))
        assertTrue(source.contains("appealSend"))
        assertTrue(source.contains("Intent.ACTION_SEND"))
        assertTrue(source.contains("shareAppealLetter"))
        assertTrue(source.contains("copyAppealLetterToClipboard"))
    }

    @Test
    fun appealDocumentAnimationUsesTargetStrategyKeyButRendersAppealLetterState() {
        val source = readSource("ui/screens/AppealScreen.kt")
        assertTrue(source.contains("documentAnimationKey"))
        assertTrue(source.contains("selectedTarget.name"))
        assertTrue(source.contains("selectedDisputeStrategy.name"))
        assertTrue(source.contains("appealLetter = appealLetter"))
        assertFalse(source.contains("displayedLetter"))
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
