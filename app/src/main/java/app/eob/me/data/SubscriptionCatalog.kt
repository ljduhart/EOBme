package app.eob.me.data

/**
 * Play Console SKUs and marketing copy for Free, Silver, and Gold tiers.
 * Annual pricing is the default in [app.eob.me.ui.screens.PaywallDialog].
 */
object SubscriptionCatalog {
    const val SILVER_MONTHLY_PRODUCT_ID = "eobme_silver_monthly"
    const val SILVER_ANNUAL_PRODUCT_ID = "eobme_silver_annual"
    const val GOLD_MONTHLY_PRODUCT_ID = "eobme_gold_monthly"
    const val GOLD_ANNUAL_PRODUCT_ID = "eobme_gold_annual"

    /** Legacy single-SKU entitlement maps to Gold. */
    const val LEGACY_PREMIUM_PRODUCT_ID = "premium_access_tier"

    val ALL_PRODUCT_IDS: Set<String> = setOf(
        SILVER_MONTHLY_PRODUCT_ID,
        SILVER_ANNUAL_PRODUCT_ID,
        GOLD_MONTHLY_PRODUCT_ID,
        GOLD_ANNUAL_PRODUCT_ID,
        LEGACY_PREMIUM_PRODUCT_ID
    )

    fun productId(tier: SubscriptionTier, interval: BillingInterval): String? = when (tier) {
        SubscriptionTier.Free -> null
        SubscriptionTier.Silver -> when (interval) {
            BillingInterval.MONTHLY -> SILVER_MONTHLY_PRODUCT_ID
            BillingInterval.ANNUAL -> SILVER_ANNUAL_PRODUCT_ID
        }
        SubscriptionTier.Gold -> when (interval) {
            BillingInterval.MONTHLY -> GOLD_MONTHLY_PRODUCT_ID
            BillingInterval.ANNUAL -> GOLD_ANNUAL_PRODUCT_ID
        }
    }

    fun tierForProductId(productId: String): SubscriptionTier? = when (productId) {
        SILVER_MONTHLY_PRODUCT_ID, SILVER_ANNUAL_PRODUCT_ID -> SubscriptionTier.Silver
        GOLD_MONTHLY_PRODUCT_ID, GOLD_ANNUAL_PRODUCT_ID, LEGACY_PREMIUM_PRODUCT_ID -> SubscriptionTier.Gold
        else -> null
    }

    fun displayPrice(tier: SubscriptionTier, interval: BillingInterval): String = when (tier) {
        SubscriptionTier.Silver -> when (interval) {
            BillingInterval.MONTHLY -> "$2.99/mo"
            BillingInterval.ANNUAL -> "$29.99/yr"
        }
        SubscriptionTier.Gold -> when (interval) {
            BillingInterval.MONTHLY -> "$4.99/mo"
            BillingInterval.ANNUAL -> "$44.99/yr"
        }
        SubscriptionTier.Free -> "$0.00"
    }

    fun checkoutPrice(tier: SubscriptionTier, interval: BillingInterval): String = when (tier) {
        SubscriptionTier.Silver -> when (interval) {
            BillingInterval.MONTHLY -> "$2.99"
            BillingInterval.ANNUAL -> "$29.99"
        }
        SubscriptionTier.Gold -> when (interval) {
            BillingInterval.MONTHLY -> "$4.99"
            BillingInterval.ANNUAL -> "$44.99"
        }
        SubscriptionTier.Free -> "$0.00"
    }

    fun features(tier: SubscriptionTier): List<String> = when (tier) {
        SubscriptionTier.Free -> listOf(
            "2 EOB Scans",
            "1 Automated Appeal",
            "2 Providers",
            "Basic History"
        )
        SubscriptionTier.Silver -> listOf(
            "6 EOB Scans",
            "2 Automated Appeals",
            "5 Providers",
            "Dispute Tracking"
        )
        SubscriptionTier.Gold -> listOf(
            "Unlimited EOB Scans",
            "Unlimited Appeals",
            "Unlimited Providers",
            "Smart Card Summaries",
            "Real-Time News"
        )
    }

    fun highestTier(first: SubscriptionTier, second: SubscriptionTier): SubscriptionTier {
        return if (first.rank >= second.rank) first else second
    }

    private val SubscriptionTier.rank: Int
        get() = when (this) {
            SubscriptionTier.Free -> 0
            SubscriptionTier.Silver -> 1
            SubscriptionTier.Gold -> 2
        }
}
