package app.eob.me.data

sealed class FeatureAccess {
    data class Limited(val limit: Int) : FeatureAccess()
    data object Unlimited : FeatureAccess()
    data object Denied : FeatureAccess()
}

object EobmeFeatureGate {
    fun getEobScanLimit(tier: SubscriptionTier): FeatureAccess = when (tier) {
        SubscriptionTier.Free -> FeatureAccess.Limited(1)
        SubscriptionTier.Silver -> FeatureAccess.Limited(4)
        SubscriptionTier.Gold -> FeatureAccess.Unlimited
    }

    fun getProviderStorageLimit(tier: SubscriptionTier): FeatureAccess = when (tier) {
        SubscriptionTier.Free -> FeatureAccess.Limited(2)
        SubscriptionTier.Silver -> FeatureAccess.Limited(5)
        SubscriptionTier.Gold -> FeatureAccess.Unlimited
    }

    fun getAppealLetterLimit(tier: SubscriptionTier): FeatureAccess = when (tier) {
        SubscriptionTier.Free -> FeatureAccess.Denied
        SubscriptionTier.Silver -> FeatureAccess.Limited(2)
        SubscriptionTier.Gold -> FeatureAccess.Unlimited
    }

    fun hasBillingErrorDetection(tier: SubscriptionTier): Boolean = tier != SubscriptionTier.Free
    fun hasRealTimeNews(tier: SubscriptionTier): Boolean = tier != SubscriptionTier.Free
    fun hasSmartCardSummaries(tier: SubscriptionTier): Boolean = tier == SubscriptionTier.Gold
    fun hasYtdExpenseTracker(tier: SubscriptionTier): Boolean = tier == SubscriptionTier.Gold
    fun hasTaxVaultFilter(tier: SubscriptionTier): Boolean = tier == SubscriptionTier.Gold
    fun hasTaxVaultClaimPackager(tier: SubscriptionTier): Boolean = tier == SubscriptionTier.Gold

    // Universal Features
    fun hasCptTracker(): Boolean = true
    fun hasAppointmentCalendar(): Boolean = true
    fun hasCareTeamSmartCards(): Boolean = true
}
