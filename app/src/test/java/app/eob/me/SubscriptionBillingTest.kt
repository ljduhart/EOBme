package app.eob.me

import android.app.Application
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingInterval
import app.eob.me.data.EobStrings
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.viewmodel.AppViewModel
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.SubscriptionViewModel
import app.eob.me.viewmodel.mergeSubscriptionStatus
import app.eob.me.viewmodel.subscriptionStateToTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionBillingTest {
    @Test
    fun subscriptionViewModelConstructorMatchesAndroidViewModelFactory() {
        val constructor = SubscriptionViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun appViewModelConstructorMatchesAndroidViewModelFactory() {
        val constructor = AppViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun subscriptionCatalogDefinesAllTierSkus() {
        assertEquals("eobme_silver_monthly", SubscriptionCatalog.SILVER_MONTHLY_PRODUCT_ID)
        assertEquals("eobme_silver_annual", SubscriptionCatalog.SILVER_ANNUAL_PRODUCT_ID)
        assertEquals("eobme_gold_monthly", SubscriptionCatalog.GOLD_MONTHLY_PRODUCT_ID)
        assertEquals("eobme_gold_annual", SubscriptionCatalog.GOLD_ANNUAL_PRODUCT_ID)
        assertEquals("premium_access_tier", SubscriptionCatalog.LEGACY_PREMIUM_PRODUCT_ID)
    }

    @Test
    fun subscriptionCatalogMarketingPricesMatchStrategy() {
        assertEquals("$2.99/mo", SubscriptionCatalog.displayPrice(SubscriptionTier.Silver, BillingInterval.MONTHLY))
        assertEquals("$29.99/yr", SubscriptionCatalog.displayPrice(SubscriptionTier.Silver, BillingInterval.ANNUAL))
        assertEquals("$4.99/mo", SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
        assertEquals("$44.99/yr", SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, BillingInterval.ANNUAL))
    }

    @Test
    fun mergeSubscriptionStatusPrefersGoldFromRevenueCat() {
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free),
                revenueCatTier = SubscriptionTier.Gold
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusPrefersGoldFromEitherSource() {
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Gold,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Gold)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusResolvesSilverTier() {
        assertEquals(
            SubscriptionState.Silver,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Silver,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusReturnsFreeWhenBothSourcesResolvedFree() {
        assertEquals(
            SubscriptionState.Free,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusStaysLoadingUntilBothSourcesResolve() {
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playTier = null,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Loading
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusSurfacesFirestoreErrorWhenPlayIsFree() {
        val state = mergeSubscriptionStatus(
            playTier = SubscriptionTier.Free,
            firestoreSnapshot = FirestoreSubscriptionSnapshot.Error("permission denied")
        )
        assertTrue(state is SubscriptionState.Error)
        assertEquals("permission denied", (state as SubscriptionState.Error).message)
    }

    @Test
    fun eobViewModelApplySubscriptionStateUpdatesHubTier() {
        val viewModel = EobViewModel()
        viewModel.applySubscriptionState(SubscriptionState.Gold)
        assertEquals(SubscriptionTier.Gold, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Silver)
        assertEquals(SubscriptionTier.Silver, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Free)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Loading)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Error("offline"))
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
    }

    @Test
    fun eobViewModelPaywallStateIsHubSourceOfTruth() {
        val viewModel = EobViewModel()
        viewModel.showPaywall("Upgrade required")
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals("Upgrade required", viewModel.uiState.value.paywallMessage)
        viewModel.dismissPaywall()
        assertFalse(viewModel.uiState.value.paywallVisible)
    }

    @Test
    fun eobViewModelReopensPaywallWhenPurchaseBillingFails() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertTrue(viewModel.uiState.value.paywallPurchasePending)

        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_flow_failed")

        assertFalse(viewModel.uiState.value.paywallPurchasePending)
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingFlowFailed"),
            viewModel.uiState.value.paywallMessage
        )
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingFlowFailed"),
            viewModel.uiState.value.hubSettings.settingsNotice
        )
    }

    @Test
    fun handleBillingNoticeForPaywallIgnoresBackgroundBillingErrors() {
        val viewModel = EobViewModel()
        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_not_ready")
        assertEquals("", viewModel.uiState.value.hubSettings.settingsNotice)
        assertFalse(viewModel.uiState.value.paywallVisible)
    }

    @Test
    fun eobViewModelApplySubscriptionStateDismissesPaywall() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        viewModel.applySubscriptionState(SubscriptionState.Gold)
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertFalse(viewModel.uiState.value.paywallPurchasePending)
    }

    @Test
    fun subscriptionStateToTierMapsActiveStates() {
        assertEquals(SubscriptionTier.Gold, subscriptionStateToTier(SubscriptionState.Gold))
        assertEquals(SubscriptionTier.Silver, subscriptionStateToTier(SubscriptionState.Silver))
        assertEquals(SubscriptionTier.Free, subscriptionStateToTier(SubscriptionState.Free))
    }

    @Test
    fun firestoreRepositoryUsesSubscriptionTierFieldWithLegacyFallback() {
        val source = readSource("data/remote/FirestoreSubscriptionRepository.kt")
        assertTrue(source.contains("FIELD_SUBSCRIPTION_TIER = \"subscriptionTier\""))
        assertTrue(source.contains("FIELD_IS_PREMIUM = \"isPremium\""))
    }

    @Test
    fun subscriptionViewModelBindUserBlankResolvesFreeWithoutMergingPlayTier() {
        val source = readSource("viewmodel/SubscriptionViewModel.kt")
        assertTrue(source.contains("if (userId.isBlank())"))
        assertTrue(source.contains("_subscriptionState.value = SubscriptionState.Free"))
    }

    @Test
    fun paywallDialogSourcesTierFeaturesFromSubscriptionCatalog() {
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        assertTrue(paywallSource.contains("SubscriptionCatalog.features(SubscriptionTier.Silver)"))
        assertTrue(paywallSource.contains("SubscriptionCatalog.features(SubscriptionTier.Gold)"))
        assertTrue(paywallSource.contains("Column(verticalArrangement = Arrangement.spacedBy"))
    }

    @Test
    fun subscriptionViewModelRefreshesRevenueCatWhenPlayTierChanges() {
        val source = readSource("viewmodel/SubscriptionViewModel.kt")
        assertTrue(source.contains("billingRepository.activePlayTier.collect"))
        assertTrue(source.contains("refreshSubscriptionState()"))
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
