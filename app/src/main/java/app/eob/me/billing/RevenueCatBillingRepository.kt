package app.eob.me.billing

import android.app.Activity
import android.content.Context
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitLogOut
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sole owner of [Purchases.sharedInstance]. Streams entitlement tier updates from RevenueCat's
 * customer-info listener so renewals and background Play pings reach the UI immediately.
 */
class RevenueCatBillingRepository(
    private val appContext: Context
) {

    private val _activeTier = MutableStateFlow(SubscriptionTier.Free)
    val activeTier: StateFlow<SubscriptionTier> = _activeTier.asStateFlow()

    private val _billingNoticeKey = MutableStateFlow<String?>(null)
    val billingNoticeKey: StateFlow<String?> = _billingNoticeKey.asStateFlow()

    private val _paywallPricing = MutableStateFlow(PaywallPricing.empty())
    val paywallPricing: StateFlow<PaywallPricing> = _paywallPricing.asStateFlow()

    private var listenerAttached = false

    private val customerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
        _activeTier.value = RevenueCatEntitlementMapper.tierFromCustomerInfo(customerInfo)
    }

    fun startListening() {
        if (!listenerAttached) {
            Purchases.sharedInstance.updatedCustomerInfoListener = customerInfoListener
            listenerAttached = true
        }
    }

    fun stopListening() {
        if (listenerAttached) {
            Purchases.sharedInstance.removeUpdatedCustomerInfoListener()
            listenerAttached = false
        }
        _activeTier.value = SubscriptionTier.Free
        _billingNoticeKey.value = null
        _paywallPricing.value = PaywallPricing.empty()
    }

    fun clearBillingNotice() {
        _billingNoticeKey.value = null
    }

    suspend fun logIn(userId: String) {
        if (userId.isBlank()) return
        runCatching { Purchases.sharedInstance.awaitLogIn(userId) }
            .onSuccess { loginResult ->
                _activeTier.value = RevenueCatEntitlementMapper.tierFromCustomerInfo(loginResult.customerInfo)
            }
    }

    suspend fun logOut() {
        runCatching { Purchases.sharedInstance.awaitLogOut() }
        _activeTier.value = SubscriptionTier.Free
    }

    suspend fun refreshCustomerInfo() {
        try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            _activeTier.value = RevenueCatEntitlementMapper.tierFromCustomerInfo(customerInfo)
        } catch (_: PurchasesException) {
            _activeTier.value = SubscriptionTier.Free
        }
    }

    suspend fun refreshOfferings() {
        try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val pricing = PaywallPricing.fromOfferings(offerings)
            if (pricing.isStorePricingLoaded) {
                _paywallPricing.value = pricing
            }
        } catch (_: PurchasesException) {
            // Keep last known or placeholder pricing until offerings sync.
        }
    }

    /**
     * @return true when RevenueCat handled the purchase; false when offerings are unavailable
     * and the caller should fall back to Google Play Billing.
     */
    suspend fun purchase(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ): Boolean {
        _billingNoticeKey.value = null
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val revenueCatPackage = RevenueCatPackageResolver.resolve(offerings, tier, interval)
                ?: return false
            val purchaseParams = PurchaseParams.Builder(activity, revenueCatPackage).build()
            val purchaseResult = Purchases.sharedInstance.awaitPurchase(purchaseParams)
            _activeTier.value = RevenueCatEntitlementMapper.tierFromCustomerInfo(purchaseResult.customerInfo)
            val pricing = PaywallPricing.fromOfferings(offerings)
            if (pricing.isStorePricingLoaded) {
                _paywallPricing.value = pricing
            }
            true
        } catch (transactionError: PurchasesTransactionException) {
            _billingNoticeKey.value = RevenueCatPurchaseErrorMapper.noticeKeyFor(transactionError)
            true
        } catch (_: PurchasesException) {
            false
        }
    }

    suspend fun restoreUserPurchases(): RestorePurchasesOutcome {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            _activeTier.value = RevenueCatEntitlementMapper.tierFromCustomerInfo(customerInfo)
            RestorePurchasesOutcome.Success(
                hasActiveSubscription = RevenueCatEntitlementMapper.hasActivePaidEntitlement(customerInfo)
            )
        } catch (error: PurchasesException) {
            RestorePurchasesOutcome.Failure(
                message = error.message ?: "Failed to reach Google Play servers."
            )
        }
    }

    fun attachUserMetadata(
        email: String,
        displayName: String,
        taxVaultStatus: String = "initialized"
    ) {
        if (email.isNotBlank()) {
            Purchases.sharedInstance.setEmail(email)
        }
        if (displayName.isNotBlank()) {
            Purchases.sharedInstance.setDisplayName(displayName)
        }
        Purchases.sharedInstance.setAttributes(
            mapOf(
                RevenueCatConfig.ATTRIBUTE_APP_BUILD_VARIANT to RevenueCatConfig.resolveAppBuildVariant(appContext),
                RevenueCatConfig.ATTRIBUTE_TAX_VAULT_STATUS to taxVaultStatus
            )
        )
    }
}
