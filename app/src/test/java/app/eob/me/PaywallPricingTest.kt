package app.eob.me

import app.eob.me.billing.PaywallPricing
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaywallPricingTest {
    @Test
    fun catalogFallbackPricesMatchSubscriptionStrategy() {
        val pricing = PaywallPricing.empty()
        assertFalse(pricing.isStorePricingLoaded)
        assertEquals("$2.99/mo", pricing.displayPrice(SubscriptionTier.Silver, BillingInterval.MONTHLY))
        assertEquals("$29.99/yr", pricing.displayPrice(SubscriptionTier.Silver, BillingInterval.ANNUAL))
        assertEquals("$5.49/mo", pricing.displayPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
        assertEquals("$49.99/yr", pricing.displayPrice(SubscriptionTier.Gold, BillingInterval.ANNUAL))
        assertEquals("$2.99", pricing.checkoutPrice(SubscriptionTier.Silver, BillingInterval.MONTHLY))
        assertEquals("$5.49", pricing.checkoutPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
    }
}
