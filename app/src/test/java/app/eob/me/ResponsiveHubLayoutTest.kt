package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveHubLayoutTest {
    @Test
    fun homeTaxVaultUsesFluidLayoutWithoutMiniatureEvidence() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val filterSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val navSource = readSource("navigation/EobNavHost.kt")

        assertFalse(homeSource.contains("VaultEvidenceCarousel"))
        assertFalse(homeSource.contains("evidenceThumbnails"))
        assertTrue(homeSource.contains("wrapContentHeight()"))
        assertTrue(homeSource.contains("fillMaxWidth()"))
        assertFalse(homeSource.contains(".height(vaultHeight)"))
        assertFalse(homeSource.contains(".width(vaultWidth)"))
        assertFalse(filterSource.contains("Spacer(modifier = Modifier.weight(1f))"))
        assertTrue(filterSource.contains("showMiniatureEvidence"))
        assertTrue(filterSource.contains("VaultEvidenceCarousel"))
        assertFalse(navSource.contains("onTaxVaultEvidenceSelected"))
    }

    @Test
    fun taxVaultDashboardEmbedsMiniatureEvidenceInsideFilterCard() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val filterSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        assertTrue(screenSource.contains("showMiniatureEvidence = true"))
        assertTrue(screenSource.contains("evidenceThumbnails = evidenceThumbnails"))
        assertFalse(screenSource.contains("VaultEvidenceCarousel("))
        assertTrue(filterSource.contains("showMiniatureEvidence && evidenceThumbnails.isNotEmpty()"))
    }

    @Test
    fun taxVaultScreenFilterCardUsesFluidHeight() {
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        assertTrue(screenSource.contains("wrapContentHeight()"))
        assertFalse(screenSource.contains(".height(280.dp)"))
    }

    @Test
    fun cptTrackerUsesAdaptiveGridAndAspectRatioFlashcards() {
        val source = readSource("ui/screens/CptTrackerScreen.kt")
        assertTrue(source.contains("GridCells.Adaptive(minSize = 150.dp)"))
        assertTrue(source.contains("aspectRatio(5f / 6f)"))
        assertFalse(source.contains("GridCells.Fixed(2)"))
        assertFalse(source.contains(".height(188.dp)"))
    }

    @Test
    fun bentoCellsAreFiftyPercentTallerOnMobile() {
        val layoutSource = readSource("ui/components/bento/BentoCellLayout.kt")
        assertTrue(layoutSource.contains("LEGACY_ASPECT_RATIO = 1.35f"))
        assertTrue(layoutSource.contains("LEGACY_ASPECT_RATIO / 1.5f"))
        assertTrue(readSource("ui/screens/HomeScreen.kt").contains("bentoSpacing"))
    }

    @Test
    fun miniatureEvidenceCarouselLivesInSharedUiModule() {
        val uiSource = readSource("ui/components/taxvault/TaxVaultEvidenceUi.kt")
        assertTrue(uiSource.contains("fun VaultEvidenceCarousel"))
        assertTrue(uiSource.contains("MiniatureCardAspectRatio"))
        assertTrue(uiSource.contains("overflow = TextOverflow.Ellipsis"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
