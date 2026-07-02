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
                "Appointment Calendar",
                "4 Smart Cards (CareTeam)",
                "Real Time Insurance News",
                "Y-T-D Expense Tracker"
            ),
            SubscriptionCatalog.features(SubscriptionTier.Silver)
        )
        assertEquals(9, SubscriptionCatalog.features(SubscriptionTier.Silver).size)
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
                "Appointment Calendar",
                "4 Smart Cards (CareTeam)",
                "Smart Card Summaries",
                "Y-T-D Expense Tracker",
                "Tax Vault Filter",
                "Tax Vault Claim Packager"
            ),
            SubscriptionCatalog.features(SubscriptionTier.Gold)
        )
        assertEquals(12, SubscriptionCatalog.features(SubscriptionTier.Gold).size)
        assertEquals(3, SubscriptionCatalog.goldHighlightFeatures().size)
        assertEquals(9, SubscriptionCatalog.goldStandardFeatures().size)
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
    fun silverTierCodedFeaturesMatchManageSubscriptionList() {
        val tier = SubscriptionTier.Silver
        assertEquals(9, SubscriptionCatalog.features(tier).size)
        assertEquals(FeatureAccess.Limited(4), EobmeFeatureGate.getEobScanLimit(tier))
        assertEquals(FeatureAccess.Limited(5), EobmeFeatureGate.getProviderStorageLimit(tier))
        assertEquals(FeatureAccess.Limited(2), EobmeFeatureGate.getAppealLetterLimit(tier))
        assertTrue(EobmeFeatureGate.hasBillingErrorDetection(tier))
        assertTrue(EobmeFeatureGate.hasRealTimeNews(tier))
        assertTrue(EobmeFeatureGate.hasYtdExpenseTracker(tier))
        assertTrue(EobmeFeatureGate.hasCptTracker())
        assertTrue(EobmeFeatureGate.hasCareTeamSmartCards())
        assertFalse(EobmeFeatureGate.hasSmartCardSummaries(tier))
        assertFalse(EobmeFeatureGate.hasTaxVaultFilter(tier))
        assertFalse(EobmeFeatureGate.hasTaxVaultClaimPackager(tier))
    }

    @Test
    fun goldTierCodedFeaturesMatchManageSubscriptionList() {
        val tier = SubscriptionTier.Gold
        assertEquals(12, SubscriptionCatalog.features(tier).size)
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getEobScanLimit(tier))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getProviderStorageLimit(tier))
        assertEquals(FeatureAccess.Unlimited, EobmeFeatureGate.getAppealLetterLimit(tier))
        assertTrue(EobmeFeatureGate.hasBillingErrorDetection(tier))
        assertTrue(EobmeFeatureGate.hasRealTimeNews(tier))
        assertTrue(EobmeFeatureGate.hasYtdExpenseTracker(tier))
        assertTrue(EobmeFeatureGate.hasSmartCardSummaries(tier))
        assertTrue(EobmeFeatureGate.hasTaxVaultFilter(tier))
        assertTrue(EobmeFeatureGate.hasTaxVaultClaimPackager(tier))
        assertTrue(EobmeFeatureGate.hasCptTracker())
    }

    @Test
    fun goldOnlyFeaturesAreGated() {
        assertFalse(EobmeFeatureGate.hasTaxVaultFilter(SubscriptionTier.Silver))
        assertFalse(EobmeFeatureGate.hasTaxVaultClaimPackager(SubscriptionTier.Silver))
        assertFalse(EobmeFeatureGate.hasSmartCardSummaries(SubscriptionTier.Silver))
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
