package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr208Test {
    @Test
    fun providerExpandUsesSingleSynchronizedAnimationPath() {
        val source = readSource("ui/screens/AnimatedProviderDirectory.kt")
        val cardBlock = source.substringAfter("fun ExpandableProviderCard")
            .substringBefore("fun FinancialStatBlock")
        assertFalse(cardBlock.contains("animateContentSize"))
        assertTrue(cardBlock.contains("ProviderExpandDurationMillis"))
        assertTrue(cardBlock.contains("expandFrom = Alignment.Top"))
        assertTrue(cardBlock.contains("shrinkTowards = Alignment.Top"))
        assertTrue(cardBlock.contains("animateDpAsState"))
    }

    @Test
    fun providerDirectoryScreenAndNavHostRemainUnchangedForPr208() {
        val screenSource = readSource("ui/screens/ProviderDirectoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(screenSource.contains("AnimatedProviderDirectoryScreen"))
        assertTrue(screenSource.contains("toPremiumProviderSummary"))
        assertTrue(navSource.contains("onViewProviderRecords"))
        assertTrue(navSource.contains("openProviderRecordHistory"))
    }

    @Test
    fun protectedPipelineUntouchedForPr208() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("ProviderExpandDurationMillis"))
        assertFalse(pipelineSource.contains("ExpandableProviderCard"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
