package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Pr159NavigationPathwayTest {
    @Test
    fun homeHubDoesNotRouteEvidencePreviewFromMiniatureCarousel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val homeRouteStart = navSource.indexOf("composable(EobRoute.Home.route)")
        val nextComposable = navSource.indexOf("composable(", homeRouteStart + 1)
        val homeRouteBlock = navSource.substring(homeRouteStart, nextComposable)

        assertFalse(homeSource.contains("VaultEvidenceCarousel"))
        assertFalse(homeSource.contains("evidenceThumbnails"))
        assertFalse(homeSource.contains("onTaxVaultEvidenceSelected"))
        assertFalse(homeRouteBlock.contains("selectTaxVaultEvidencePreview"))
        assertFalse(homeRouteBlock.contains("taxVaultEvidenceThumbnails"))
    }

    @Test
    fun homeVaultDoorUnlockNavigatesToTaxVaultDashboard() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val homeRouteStart = navSource.indexOf("composable(EobRoute.Home.route)")
        val nextComposable = navSource.indexOf("composable(", homeRouteStart + 1)
        val homeRouteBlock = navSource.substring(homeRouteStart, nextComposable)

        assertTrue(homeRouteBlock.contains("onVaultDoorUnlocked"))
        assertTrue(homeRouteBlock.contains("requestTaxVaultDoorUnlock"))
        assertTrue(homeRouteBlock.contains("navController.navigate(EobRoute.TaxVault.route)"))
    }

    @Test
    fun taxVaultDashboardRoutesEvidencePreviewThroughViewModel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val screenSource = readSource("ui/screens/TaxVaultScreen.kt")
        val filterSource = readSource("ui/components/home/TaxVaultVerticalFilterCard.kt")
        val vaultRouteStart = navSource.indexOf("composable(EobRoute.TaxVault.route)")
        val nextComposable = navSource.indexOf("composable(", vaultRouteStart + 1)
        val vaultRouteBlock = navSource.substring(vaultRouteStart, nextComposable)

        assertTrue(vaultRouteBlock.contains("taxVaultEvidenceThumbnails()"))
        assertTrue(vaultRouteBlock.contains("selectTaxVaultEvidencePreview"))
        assertTrue(vaultRouteBlock.contains("popBackStack(EobRoute.Home.route"))
        assertTrue(screenSource.contains("showMiniatureEvidence = true"))
        assertTrue(screenSource.contains("onEvidenceSelected = onEvidenceSelected"))
        assertTrue(filterSource.contains("onEvidenceSelected = onEvidenceSelected"))
        assertFalse(screenSource.contains("VaultEvidenceCarousel("))
    }

    @Test
    fun bentoAndHubDestinationsRemainWired() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val layoutSource = readSource("ui/components/bento/BentoCellLayout.kt")

        assertTrue(layoutSource.contains("LEGACY_ASPECT_RATIO / 1.5f"))
        assertTrue(homeSource.contains("HubBentoDestination.gridRows"))
        assertTrue(homeSource.contains("onBentoSelected(destination)"))
        assertTrue(homeSource.contains("BentoGridLayout.spacing"))
        assertTrue(navSource.contains("navController.navigate(destination.route)"))
    }

    @Test
    fun insuranceCardNotesSavePathwayRemainsIntact() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")

        assertTrue(homeSource.contains("onInsurancePrescriptionsChange"))
        assertTrue(homeSource.contains("onInsuranceDoctorNotesChange"))
        assertTrue(navSource.contains("updateInsuranceCardPrescriptions"))
        assertTrue(navSource.contains("updateInsuranceCardDoctorNotes"))
        assertTrue(cardSource.contains("onCurrentPrescriptionsChange"))
        assertTrue(cardSource.contains("onDoctorQuickNotesChange"))
        assertFalse(cardSource.contains("placeholder = {"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
