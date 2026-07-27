package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr191Test {
    @Test
    fun providerDirectoryHubBentoMatchesOtherBentoCellAspectRatio() {
        val layoutSource = readSource("ui/components/bento/BentoCellLayout.kt")
        val gridSource = readSource("ui/components/bento/BentoGridCell.kt")
        assertFalse(layoutSource.contains("PROVIDER_DIRECTORY_ASPECT_RATIO"))
        assertTrue(gridSource.contains("HubBentoDestination.ProviderDirectory ->"))
        val providerBlock = gridSource.substringAfter("HubBentoDestination.ProviderDirectory ->")
            .substringBefore("HubBentoDestination.CptTracker ->")
        assertTrue(providerBlock.contains("cellAspectRatio = cellAspectRatio"))
        assertFalse(providerBlock.contains("PROVIDER_DIRECTORY_ASPECT_RATIO"))
    }

    @Test
    fun providerDirectoryListCardsAreFifteenPercentShorterVertically() {
        val source = readSource("ui/screens/AnimatedProviderDirectory.kt")
        assertTrue(source.contains("ProviderDirectoryVerticalScale = 0.85f"))
        assertTrue(source.contains("ProviderCardPadding"))
        assertTrue(source.contains("ProviderAvatarSize"))
        assertTrue(source.contains("ProviderListItemSpacing"))
    }

    @Test
    fun fsaSelectedShowsMiniatureEvidenceAboveTaxVaultFilter() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        assertTrue(screenSource.contains("filterState == TaxVaultFilterState.FSA && evidenceThumbnails.isNotEmpty()"))
        assertTrue(screenSource.contains("VaultEvidenceCarousel("))
        assertTrue(screenSource.contains("showMiniatureEvidence = filterState != TaxVaultFilterState.FSA"))
        val fsaEvidenceBlock = screenSource.substringAfter("filterState == TaxVaultFilterState.FSA")
            .substringBefore("TaxVaultVerticalFilterCard(")
        assertTrue(fsaEvidenceBlock.contains("VaultEvidenceCarousel"))
        val filterBlock = screenSource.substringAfter("TaxVaultVerticalFilterCard(")
            .substringBefore("VaultExportSection(")
        assertTrue(filterBlock.contains("showMiniatureEvidence = filterState != TaxVaultFilterState.FSA"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr191() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("ProviderDirectoryVerticalScale"))
        assertFalse(splashSource.contains("VaultEvidenceCarousel"))
        assertFalse(introSource.contains("showMiniatureEvidence = filterState"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
