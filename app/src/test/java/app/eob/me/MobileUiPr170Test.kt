package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr170Test {
    @Test
    fun insuranceNewsUsesWhiteReadableTextInDarkMode() {
        val source = readSource("ui/screens/NewsScreen.kt")
        assertTrue(source.contains("InsuranceNewsDarkModeText"))
        assertTrue(source.contains("EobCyberTextPrimary"))
        assertTrue(source.contains("isHubDarkPresentation()"))
        assertFalse(source.contains("InsuranceNewsDarkModeText = Color.Black"))
    }

    @Test
    fun eobHistoryUsesSingleLazyColumnScroll() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(historySource.contains("item(key = \"history_search\")"))
        assertTrue(historySource.contains("OutlinedTextField"))
        assertTrue(historySource.contains("historyTimelineItems"))
        assertTrue(historySource.contains("searchQuery"))
        assertFalse(historySource.contains(".weight(0.16f)"))
        assertFalse(historySource.contains(".weight(0.84f)"))
        assertFalse(navSource.contains("fillMaxHeight(0.10f)"))
        assertTrue(navSource.contains("onSearchQueryChange"))
        assertTrue(navSource.contains("totalBillingErrors = totalBillingErrors"))
    }

    @Test
    fun appealGeneratorBentoShowsTitleAboveIcon() {
        val source = readSource("ui/components/bento/AppealGeneratorBentoCell.kt")
        val columnBlock = source.substringAfter("verticalArrangement = Arrangement.Center")
            .substringBefore("@Composable\nprivate fun AppealProcessingRing")
        val titleIndex = columnBlock.indexOf("BentoCellTitle")
        val iconIndex = columnBlock.indexOf("HubBentoIcon")
        assertTrue(titleIndex >= 0)
        assertTrue(iconIndex >= 0)
        assertTrue(titleIndex < iconIndex)
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
