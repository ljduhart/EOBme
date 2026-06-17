package app.eob.me

import app.eob.me.data.EobmeFeatureGate
import app.eob.me.data.FeatureAccess
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {
    @Test
    fun subscriptionCatalogFeaturesMatchTierStrategy() {
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Free).contains("CPT Tracker"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Silver).contains("Billing Error Detection"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Gold).contains("Tax Vault Filter (HSA/FSA)"))
    }

    @Test
    fun eobScanLimitsFollowTier() {
        assertEquals(FeatureAccess.Limited(2), EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Free))
        assertEquals(FeatureAccess.Limited(5), EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Silver))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Gold))
    }

    @Test
    fun appealLettersDeniedForFreeTier() {
        assertEquals(FeatureAccess.Denied, EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Free))
        assertEquals(FeatureAccess.Limited(2), EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Silver))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Gold))
    }

    @Test
    fun goldOnlyFeaturesAreGated() {
        assertFalse(EobmeFeatureGate.hasRealTimeNews(SubscriptionTier.Free))
        assertFalse(EobmeFeatureGate.hasYtdExpenseTracker(SubscriptionTier.Silver))
        assertTrue(EobmeFeatureGate.hasTaxVaultFilter(SubscriptionTier.Gold))
    }

    @Test
    fun universalFeaturesAlwaysEnabled() {
        assertTrue(EobmeFeatureGate.hasCptTracker())
        assertTrue(EobmeFeatureGate.hasAppointmentCalendar())
    }
}
