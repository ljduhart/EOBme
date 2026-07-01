package app.eob.me.billing

import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.Offerings

/**
 * Store-localized paywall prices resolved from RevenueCat offerings.
 * Falls back to [SubscriptionCatalog] marketing prices until Play offerings sync.
 */
data class PaywallPricing(
    private val displayPrices: Map<String, String> = emptyMap(),
    private val checkoutPrices: Map<String, String> = emptyMap(),
    private val storePricesLoaded: Boolean = false
) {
    fun displayPrice(tier: SubscriptionTier, interval: BillingInterval): String {
        if (tier == SubscriptionTier.Free) {
            return SubscriptionCatalog.displayPrice(tier, interval)
        }
        return displayPrices[priceKey(tier, interval)]
            ?: SubscriptionCatalog.displayPrice(tier, interval)
    }

    fun checkoutPrice(tier: SubscriptionTier, interval: BillingInterval): String {
        if (tier == SubscriptionTier.Free) {
            return SubscriptionCatalog.checkoutPrice(tier, interval)
        }
        return checkoutPrices[priceKey(tier, interval)]
            ?: SubscriptionCatalog.checkoutPrice(tier, interval)
    }

    /** True when RevenueCat returned localized Play Store prices for at least one package. */
    val isStorePricingLoaded: Boolean
        get() = storePricesLoaded

    companion object {
        fun empty(): PaywallPricing = PaywallPricing()

        fun fromOfferings(offerings: Offerings): PaywallPricing {
            val display = mutableMapOf<String, String>()
            val checkout = mutableMapOf<String, String>()
            SubscriptionTier.entries.filter { it != SubscriptionTier.Free }.forEach { tier ->
                BillingInterval.entries.forEach { interval ->
                    val revenueCatPackage = RevenueCatPackageResolver.resolve(offerings, tier, interval)
                        ?: return@forEach
                    val formatted = revenueCatPackage.product.price.formatted
                    if (formatted.isBlank()) return@forEach
                    val key = priceKey(tier, interval)
                    val suffix = when (interval) {
                        BillingInterval.MONTHLY -> "/mo"
                        BillingInterval.ANNUAL -> "/yr"
                    }
                    display[key] = "$formatted$suffix"
                    checkout[key] = formatted
                }
            }
            return PaywallPricing(
                displayPrices = display,
                checkoutPrices = checkout,
                storePricesLoaded = display.isNotEmpty()
            )
        }

        private fun priceKey(tier: SubscriptionTier, interval: BillingInterval): String =
            "${tier.name}_${interval.name}"
    }
}
