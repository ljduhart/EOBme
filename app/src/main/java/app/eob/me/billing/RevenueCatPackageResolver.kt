package app.eob.me.billing

import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package

internal object RevenueCatPackageResolver {
    fun resolve(
        offerings: Offerings,
        tier: SubscriptionTier,
        interval: BillingInterval
    ): Package? {
        val offerRef = SubscriptionCatalog.offerRef(tier, interval) ?: return null
        val revenueCatIdentifier = offerRef.revenueCatProductIdentifier
        val candidates = buildList {
            offerings.current?.availablePackages.orEmpty().forEach(::add)
            offerings.all.values.forEach { offering ->
                offering.availablePackages.forEach(::add)
            }
        }.distinctBy { it.identifier }

        return candidates.firstOrNull { revenueCatPackage ->
            matchesOffer(revenueCatPackage, offerRef.subscriptionProductId, offerRef.basePlanId, revenueCatIdentifier)
        }
    }

    private fun matchesOffer(
        revenueCatPackage: Package,
        subscriptionProductId: String,
        basePlanId: String,
        revenueCatIdentifier: String
    ): Boolean {
        if (revenueCatPackage.identifier.equals(revenueCatIdentifier, ignoreCase = true)) {
            return true
        }
        val storeProduct = revenueCatPackage.product
        if (storeProduct.id == subscriptionProductId) {
            val subscriptionOptions = storeProduct.subscriptionOptions?.map { option -> option.id }.orEmpty()
            if (subscriptionOptions.any { optionId -> optionId.equals(basePlanId, ignoreCase = true) }) {
                return true
            }
            val defaultOptionId = storeProduct.defaultOption?.id
            if (defaultOptionId.equals(basePlanId, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
