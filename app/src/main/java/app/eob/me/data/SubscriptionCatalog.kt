package app.eob.me.data

/**
 * Play Console subscription parents with base plans and RevenueCat product identifiers.
 * Google Play subscription IDs: [SILVER_SUBSCRIPTION_ID], [GOLD_SUBSCRIPTION_ID].
 * Base plan IDs: [BASE_PLAN_MONTHLY], [BASE_PLAN_ANNUAL] (e.g. eobme_gold + monthly).
 * RevenueCat identifiers follow parent:basePlan (e.g. eobme_gold:monthly).
 */
object SubscriptionCatalog {
    const val SILVER_SUBSCRIPTION_ID = "eobme_silver"
    const val GOLD_SUBSCRIPTION_ID = "eobme_gold"
    const val BASE_PLAN_MONTHLY = "monthly"
    const val BASE_PLAN_ANNUAL = "annual"

    /** Legacy single-SKU entitlement maps to Gold. */
    const val LEGACY_PREMIUM_PRODUCT_ID = "premium_access_tier"

    data class SubscriptionOfferRef(
        val subscriptionProductId: String,
        val basePlanId: String
    ) {
        val revenueCatProductIdentifier: String
            get() = "$subscriptionProductId:$basePlanId"
    }

    val ALL_SUBSCRIPTION_PRODUCT_IDS: Set<String> = setOf(
        SILVER_SUBSCRIPTION_ID,
        GOLD_SUBSCRIPTION_ID,
        LEGACY_PREMIUM_PRODUCT_ID
    )

    fun subscriptionProductId(tier: SubscriptionTier): String? = when (tier) {
        SubscriptionTier.Free -> null
        SubscriptionTier.Silver -> SILVER_SUBSCRIPTION_ID
        SubscriptionTier.Gold -> GOLD_SUBSCRIPTION_ID
    }

    fun basePlanId(interval: BillingInterval): String = when (interval) {
        BillingInterval.MONTHLY -> BASE_PLAN_MONTHLY
        BillingInterval.ANNUAL -> BASE_PLAN_ANNUAL
    }

    fun offerRef(tier: SubscriptionTier, interval: BillingInterval): SubscriptionOfferRef? {
        val subscriptionProductId = subscriptionProductId(tier) ?: return null
        return SubscriptionOfferRef(
            subscriptionProductId = subscriptionProductId,
            basePlanId = basePlanId(interval)
        )
    }

    fun revenueCatProductIdentifier(tier: SubscriptionTier, interval: BillingInterval): String? =
        offerRef(tier, interval)?.revenueCatProductIdentifier

    fun tierForProductId(productId: String): SubscriptionTier? = when (productId) {
        SILVER_SUBSCRIPTION_ID -> SubscriptionTier.Silver
        GOLD_SUBSCRIPTION_ID, LEGACY_PREMIUM_PRODUCT_ID -> SubscriptionTier.Gold
        else -> null
    }

    fun displayPrice(tier: SubscriptionTier, interval: BillingInterval): String = when (tier) {
        SubscriptionTier.Silver -> when (interval) {
            BillingInterval.MONTHLY -> "$2.99/mo"
            BillingInterval.ANNUAL -> "$29.99/yr"
        }
        SubscriptionTier.Gold -> when (interval) {
            BillingInterval.MONTHLY -> "$5.49/mo"
            BillingInterval.ANNUAL -> "$49.99/yr"
        }
        SubscriptionTier.Free -> "$0.00"
    }

    fun checkoutPrice(tier: SubscriptionTier, interval: BillingInterval): String = when (tier) {
        SubscriptionTier.Silver -> when (interval) {
            BillingInterval.MONTHLY -> "$2.99"
            BillingInterval.ANNUAL -> "$29.99"
        }
        SubscriptionTier.Gold -> when (interval) {
            BillingInterval.MONTHLY -> "$5.49"
            BillingInterval.ANNUAL -> "$49.99"
        }
        SubscriptionTier.Free -> "$0.00"
    }

    fun features(tier: SubscriptionTier): List<String> = when (tier) {
        SubscriptionTier.Free -> listOf(
            "1 EOB Scan per month",
            "2 Providers Directory Storage",
            "CPT Tracker",
            "Appointment Calendar",
            "4 Smart Cards (CareTeam)"
        )
        SubscriptionTier.Silver -> listOf(
            "4 EOB Scans per month",
            "5 Providers Storage",
            "Billing Error Detection",
            "2 Automated Appeal Letters per month",
            "CPT Tracker",
            "Appointment Calendar",
            "4 Smart Cards (CareTeam)",
            "Real Time Insurance News",
            "Y-T-D Expense Tracker"
        )
        SubscriptionTier.Gold -> listOf(
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
        )
    }

    private val goldHighlightFeatureNames = setOf(
        "Tax Vault Filter",
        "Tax Vault Claim Packager",
        "Smart Card Summaries"
    )

    fun goldHighlightFeatures(): List<String> = features(SubscriptionTier.Gold).filter { it in goldHighlightFeatureNames }

    fun goldStandardFeatures(): List<String> = features(SubscriptionTier.Gold).filter { it !in goldHighlightFeatureNames }

    fun highestTier(first: SubscriptionTier, second: SubscriptionTier): SubscriptionTier {
        return if (first.rank() >= second.rank()) first else second
    }
}
