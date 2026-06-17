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
    fun subscriptionCatalogSilverFeaturesMatchManageSubscriptionCopy() {
        assertEquals(
            listOf(
                "5 EOB Scans",
                "5 Providers Storage",
                "Billing Error Detection",
                "2 Automated Appeal Letters",
                "CPT Tracker"
            ),
            SubscriptionCatalog.features(SubscriptionTier.Silver)
        )
    }

    @Test
    fun subscriptionCatalogGoldFeaturesMatchManageSubscriptionCopy() {
        assertEquals(
            listOf(
                "Unlimited EOB Scans",
                "Unlimited Providers Storage",
                "Unlimited Appeal Letters",
                "Billing Error Detection",
                "Real Time Insurance News",
                "CPT Tracker",
                "Smart Card Summaries",
                "Y-T-D Expense Tracker",
                "Tax Vault Filter (HSA/FSA)"
            ),
            SubscriptionCatalog.features(SubscriptionTier.Gold)
        )
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
