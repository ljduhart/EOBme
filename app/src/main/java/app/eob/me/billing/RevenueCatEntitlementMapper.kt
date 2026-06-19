package app.eob.me.billing

import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.CustomerInfo

internal object RevenueCatEntitlementMapper {
    fun tierFromCustomerInfo(customerInfo: CustomerInfo): SubscriptionTier {
        val activeEntitlements = customerInfo.entitlements.active
        return when {
            activeEntitlements.containsKey(RevenueCatConfig.ENTITLEMENT_GOLD) -> SubscriptionTier.Gold
            activeEntitlements.containsKey(RevenueCatConfig.ENTITLEMENT_SILVER) -> SubscriptionTier.Silver
            else -> SubscriptionTier.Free
        }
    }

    fun hasActivePaidEntitlement(customerInfo: CustomerInfo): Boolean =
        tierFromCustomerInfo(customerInfo) != SubscriptionTier.Free
}
