package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr169Test {
    @Test
    fun insuranceNewsUsesHighContrastReadableTextInDarkMode() {
        val source = readSource("ui/screens/NewsScreen.kt")
        assertTrue(source.contains("InsuranceNewsDarkModeText"))
        assertTrue(source.contains("EobCyberTextPrimary"))
        assertTrue(source.contains("isHubDarkPresentation()"))
        assertTrue(source.contains("background.luminance()"))
        assertTrue(source.contains("insuranceNewsTitleColor"))
    }

    @Test
    fun swipeDeleteNewsRequiresConfirmation() {
        val source = readSource("ui/screens/NewsScreen.kt")
        val swipeBlock = source.substringAfter("private fun SwipeableNewsBriefingCard")
            .substringBefore("@Composable\nfun NewsBriefingCard")
        assertTrue(swipeBlock.contains("confirmValueChange"))
        assertTrue(swipeBlock.contains("deleteNewsConfirmTitle"))
        assertTrue(swipeBlock.contains("deleteNewsConfirmMessage"))
        assertTrue(swipeBlock.contains("AlertDialog"))
        assertTrue(swipeBlock.contains("remember(newsKey)"))
        assertFalse(swipeBlock.contains("deleteTriggered"))
    }

    @Test
    fun insuranceNewsBentoHidesExternalCarrierLogos() {
        val bentoSource = readSource("ui/components/bento/InsuranceNewsBentoCell.kt")
        assertFalse(bentoSource.contains("InsuranceBriefingLogoStrip"))
        assertFalse(bentoSource.contains("InsuranceBriefingAssets"))
        val newsSource = readSource("ui/screens/NewsScreen.kt")
        assertTrue(newsSource.contains("InsuranceBriefingAssets.logoResId"))
    }

    @Test
    fun taxVaultFilterUsesGoldKeyWithoutCentralRay() {
        val source = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        assertTrue(source.contains("TaxVaultGoldKeyGlyph"))
        assertFalse(source.contains("drawTaxVaultCentralLightRay"))
        val statusBlock = source.substringAfter("VaultKeyCardsAndBinaryGlyph")
            .substringBefore("if (enableShimmerOverlay)")
        assertTrue(statusBlock.contains("taxVaultOffLabel"))
        assertTrue(statusBlock.contains("TaxVaultGoldKeyGlyph"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
