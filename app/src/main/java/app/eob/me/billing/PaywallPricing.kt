package app.eob.me.billing

import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.Offerings

/**
 * Store-localized paywall prices resolved from RevenueCat offerings.
 * UI must never hardcode subscription amounts; fall back to placeholders until offerings load.
 */
data class PaywallPricing(
    private val displayPrices: Map<String, String> = emptyMap(),
    private val checkoutPrices: Map<String, String> = emptyMap()
) {
    fun displayPrice(tier: SubscriptionTier, interval: BillingInterval): String =
        displayPrices[priceKey(tier, interval)] ?: PLACEHOLDER

    fun checkoutPrice(tier: SubscriptionTier, interval: BillingInterval): String =
        checkoutPrices[priceKey(tier, interval)] ?: PLACEHOLDER

    val isLoaded: Boolean
        get() = displayPrices.isNotEmpty()

    companion object {
        private const val PLACEHOLDER = "—"

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
            return PaywallPricing(display, checkout)
        }

        private fun priceKey(tier: SubscriptionTier, interval: BillingInterval): String =
            "${tier.name}_${interval.name}"
    }
}
