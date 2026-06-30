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
                "4 EOB Scans per month",
                "5 Providers Storage",
                "Billing Error Detection",
                "2 Automated Appeal Letters per month",
                "CPT Tracker",
                "4 Smart Cards (CareTeam)",
                "Real Time Insurance News"
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
                "Tax Vault Filter (HSA/FSA)",
                "Tax Vault Claim Packager"
            ),
            SubscriptionCatalog.features(SubscriptionTier.Gold)
        )
    }

    @Test
    fun eobScanLimitsFollowTier() {
        assertEquals(FeatureAccess.Limited(1), EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Free))
        assertEquals(FeatureAccess.Limited(4), EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Silver))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getEobScanLimit(SubscriptionTier.Gold))
    }

    @Test
    fun appealLettersDeniedForFreeTier() {
        assertEquals(FeatureAccess.Denied, EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Free))
        assertEquals(FeatureAccess.Limited(2), EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Silver))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getAppealLetterLimit(SubscriptionTier.Gold))
    }

    @Test
    fun silverUnlocksRealTimeNews() {
        assertFalse(EobmeFeatureGate.hasRealTimeNews(SubscriptionTier.Free))
        assertTrue(EobmeFeatureGate.hasRealTimeNews(SubscriptionTier.Silver))
        assertTrue(EobmeFeatureGate.hasRealTimeNews(SubscriptionTier.Gold))
    }

    @Test
    fun goldOnlyFeaturesAreGated() {
        assertFalse(EobmeFeatureGate.hasYtdExpenseTracker(SubscriptionTier.Silver))
        assertTrue(EobmeFeatureGate.hasTaxVaultFilter(SubscriptionTier.Gold))
        assertTrue(EobmeFeatureGate.hasTaxVaultClaimPackager(SubscriptionTier.Gold))
        assertTrue(EobmeFeatureGate.hasSmartCardSummaries(SubscriptionTier.Gold))
    }

    @Test
    fun universalFeaturesAlwaysEnabled() {
        assertTrue(EobmeFeatureGate.hasCptTracker())
        assertTrue(EobmeFeatureGate.hasAppointmentCalendar())
        assertTrue(EobmeFeatureGate.hasCareTeamSmartCards())
    }
}
