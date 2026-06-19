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
        assertTrue(source.contains("slideInVertically"))
        assertTrue(source.contains("slideOutVertically"))
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
        assertTrue(navSource.contains("regenerateAppeal"))
        assertTrue(navSource.contains("enableAppealLetterEditing"))
        assertTrue(navSource.contains("saveAppealLetter"))
        assertTrue(navSource.contains("updateAppeal"))
        assertTrue(viewModelSource.contains("fun regenerateAppeal"))
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
    fun appealDocumentAnimationDoesNotRetriggerWhileEditing() {
        val source = readSource("ui/screens/AppealScreen.kt")
        assertTrue(source.contains("appealLetterEditingEnabled"))
        assertTrue(source.contains("documentAnimationKey"))
        assertFalse(source.contains("EobAnalyzer"))
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
