package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr185Test {
    @Test
    fun paywallAndManageSubscriptionShareScrollableTierComparisonPanel() {
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        val manageSource = readSource("ui/screens/ManageSubscriptionScreen.kt")
        val panelSource = readSource("ui/components/SubscriptionTierComparisonPanel.kt")
        assertTrue(paywallSource.contains("SubscriptionTierComparisonPanel"))
        assertTrue(manageSource.contains("SubscriptionTierComparisonPanel"))
        assertTrue(panelSource.contains("SubscriptionGoldTierCard"))
        assertTrue(panelSource.contains("verticalScroll"))
        assertTrue(panelSource.contains("SubscriptionCatalog.goldStandardFeatures"))
        assertTrue(panelSource.contains("SubscriptionCatalog.goldHighlightFeatures"))
    }

    @Test
    fun premiumBentoAndRouteGuardsUseContextualPaywallMessages() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("paywallMessageForBentoDestination"))
        assertTrue(navSource.contains("paywallMessageForBillingErrorGate"))
        assertTrue(navSource.contains("paywallMessageForInsuranceCardFeature"))
        assertTrue(navSource.contains("HubBentoDestination.YtdExpense"))
        assertTrue(navSource.contains("HubBentoDestination.InsuranceNews"))
        assertTrue(navSource.contains("HubBentoDestination.AppealGenerator"))
        assertTrue(viewModelSource.contains("fun paywallMessageForBentoDestination"))
        assertTrue(viewModelSource.contains("fun paywallMessageForBillingErrorGate"))
        assertTrue(viewModelSource.contains("fun paywallMessageForInsuranceCardFeature"))
    }

    @Test
    fun paywallUnlockMessagesResolveForEveryLanguage() {
        val keys = listOf(
            "paywallUnlockYtdExpense",
            "paywallUnlockInsuranceNews",
            "paywallUnlockAppealGenerator",
            "paywallUnlockBillingErrors"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                val value = EobStrings.t(language, key)
                assertNotEquals(key, value)
                assertTrue(value.contains("Free") || value.contains("Gratis") ||
                    value.contains("Gratuit") || value.contains("免费"))
            }
        }
    }

    @Test
    fun eobViewModelReturnsFeatureSpecificPaywallMessagesForBentoDestinations() {
        val viewModel = EobViewModel()
        val language = AppLanguage.English
        assertTrue(
            viewModel.paywallMessageForBentoDestination(language, HubBentoDestination.YtdExpense)
                .contains("Y-T-D")
        )
        assertTrue(
            viewModel.paywallMessageForBentoDestination(language, HubBentoDestination.InsuranceNews)
                .contains("Insurance News")
        )
        assertTrue(
            viewModel.paywallMessageForBentoDestination(language, HubBentoDestination.AppealGenerator)
                .contains("Appeal Generator")
        )
        assertTrue(
            viewModel.paywallMessageForBillingErrorGate(language)
                .contains("Billing Error Detection")
        )
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr185() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("SubscriptionTierComparisonPanel"))
        assertFalse(veryfiSource.contains("paywallMessageForBentoDestination"))
        assertFalse(splashSource.contains("paywallUnlockYtdExpense"))
        assertFalse(introSource.contains("SubscriptionTierComparisonPanel"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
