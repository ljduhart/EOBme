package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr179Test {
    @Test
    fun smartCardsUseSingleTapFlipAndDoubleTapDialWhenComplete() {
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertTrue(careTeamSource.contains("onTap = { isFlipped = !isFlipped }"))
        assertTrue(careTeamSource.contains("onDoubleTap = {"))
        assertTrue(careTeamSource.contains("cardState.isCompleteWithPhone"))
        assertTrue(careTeamSource.contains("dialCareTeamPhone(context, language, doctor.phone)"))
        assertFalse(careTeamSource.contains("onCall = cardState.phoneDialUri"))
    }

    @Test
    fun shimmerStopsForAllCardsWhenAnyCardIsCompleteWithPhone() {
        val modelSource = readSource("data/EobModels.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val careTeamSource = readSource("ui/components/home/HomeCareTeamCards.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(modelSource.contains("isCompleteWithPhone"))
        assertTrue(viewModelSource.contains("fun careTeamShimmerSuppressed"))
        assertTrue(careTeamSource.contains("shimmerSuppressed"))
        assertTrue(careTeamSource.contains("!cardState.isAssigned && !shimmerSuppressed"))
        assertTrue(navSource.contains("careTeamShimmerSuppressed"))
    }

    @Test
    fun homeBentoShowsTwoMostRecentProvidersRegardlessOfTier() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val analyzerSource = readSource("data/EobAnalyzer.kt")
        val bentoSource = readSource("ui/components/bento/ProviderDirectoryBentoCell.kt")
        assertTrue(viewModelSource.contains("HOME_BENTO_PROVIDER_PREVIEW_LIMIT = 2"))
        assertTrue(viewModelSource.contains("providerDirectoryByRecency"))
        assertFalse(viewModelSource.contains("getProviderStorageLimit(tier)"))
        assertTrue(analyzerSource.contains("fun providerDirectoryByRecency"))
        assertTrue(bentoSource.contains("avatars.take(2)"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr179() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("isCompleteWithPhone"))
        assertFalse(veryfiSource.contains("providerDirectoryByRecency"))
        assertFalse(splashSource.contains("onDoubleTap"))
        assertFalse(introSource.contains("HOME_BENTO_PROVIDER_PREVIEW_LIMIT"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
