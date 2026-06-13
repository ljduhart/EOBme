package app.eob.me

import app.eob.me.billing.SubscriptionState
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.FirestorePremiumSnapshot
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.mergeSubscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionBillingTest {
    @Test
    fun premiumProductIdMatchesPlayConsoleSku() {
        assertEquals("premium_access_tier", app.eob.me.billing.BillingRepository.PREMIUM_PRODUCT_ID)
    }

    @Test
    fun mergeSubscriptionStatusPrefersPremiumFromEitherSource() {
        assertEquals(
            SubscriptionState.Premium,
            mergeSubscriptionStatus(
                playPremium = true,
                firestoreSnapshot = FirestorePremiumSnapshot.Resolved(isPremium = false)
            )
        )
        assertEquals(
            SubscriptionState.Premium,
            mergeSubscriptionStatus(
                playPremium = false,
                firestoreSnapshot = FirestorePremiumSnapshot.Resolved(isPremium = true)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusReturnsFreeWhenBothSourcesResolvedFree() {
        assertEquals(
            SubscriptionState.Free,
            mergeSubscriptionStatus(
                playPremium = false,
                firestoreSnapshot = FirestorePremiumSnapshot.Resolved(isPremium = false)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusStaysLoadingUntilBothSourcesResolve() {
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playPremium = null,
                firestoreSnapshot = FirestorePremiumSnapshot.Resolved(isPremium = false)
            )
        )
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playPremium = false,
                firestoreSnapshot = FirestorePremiumSnapshot.Loading
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusSurfacesFirestoreErrorWhenPlayIsFree() {
        val state = mergeSubscriptionStatus(
            playPremium = false,
            firestoreSnapshot = FirestorePremiumSnapshot.Error("permission denied")
        )
        assertTrue(state is SubscriptionState.Error)
        assertEquals("permission denied", (state as SubscriptionState.Error).message)
    }

    @Test
    fun eobViewModelApplySubscriptionStateUpdatesHubTier() {
        val viewModel = EobViewModel()
        viewModel.applySubscriptionState(SubscriptionState.Premium)
        assertEquals(SubscriptionTier.Premium, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Free)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Loading)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Error("offline"))
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
    }

    @Test
    fun firestoreRepositoryUsesUsersCollectionAndIsPremiumField() {
        val source = readSource("data/remote/FirestoreSubscriptionRepository.kt")
        assertTrue(source.contains("USERS_COLLECTION = \"users\""))
        assertTrue(source.contains("FIELD_IS_PREMIUM = \"isPremium\""))
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
