package app.eob.me.billing

import android.app.Activity
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encapsulates RevenueCat Purchases SDK calls for offerings, purchases, and [CustomerInfo] updates.
 */
class RevenueCatManager {

    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()

    private val _offerings = MutableStateFlow<Offerings?>(null)
    val offerings: StateFlow<Offerings?> = _offerings.asStateFlow()

    private val _billingErrorKey = MutableStateFlow<String?>(null)
    val billingErrorKey: StateFlow<String?> = _billingErrorKey.asStateFlow()

    private var isListening = false

    private val updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
        _customerInfo.value = customerInfo
    }

    fun attachToPurchases() {
        if (!RevenueCatConfig.isConfigured || isListening) return
        Purchases.sharedInstance.updatedCustomerInfoListener = updatedCustomerInfoListener
        isListening = true
        refreshCustomerInfo()
        fetchOfferings()
    }

    fun start() {
        if (!RevenueCatConfig.isConfigured) return
        attachToPurchases()
    }

    fun stop() {
        if (!isListening) return
        Purchases.sharedInstance.removeUpdatedCustomerInfoListener()
        isListening = false
    }

    fun fetchOfferings(
        onSuccess: (Offerings) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!RevenueCatConfig.isConfigured) {
            onError("billing_not_ready")
            return
        }
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _billingErrorKey.value = mapPurchasesError(error)
                onError(error.message)
            },
            onSuccess = { offerings ->
                _billingErrorKey.value = null
                _offerings.value = offerings
                onSuccess(offerings)
            }
        )
    }

    fun purchasePackage(
        activity: Activity,
        rcPackage: Package,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!RevenueCatConfig.isConfigured) {
            _billingErrorKey.value = "billing_not_ready"
            onError("billing_not_ready")
            return
        }
        _billingErrorKey.value = null
        val purchaseParams = PurchaseParams.Builder(activity, rcPackage).build()
        Purchases.sharedInstance.purchase(
            purchaseParams,
            object : PurchaseCallback {
                override fun onCompleted(
                    storeTransaction: StoreTransaction,
                    customerInfo: CustomerInfo
                ) {
                    _customerInfo.value = customerInfo
                    _billingErrorKey.value = null
                    onSuccess()
                }

                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                    if (userCancelled) return
                    _billingErrorKey.value = mapPurchasesError(error)
                    onError(error.message)
                }
            }
        )
    }

    fun purchaseTier(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cachedOfferings = _offerings.value
        if (cachedOfferings != null) {
            purchaseTierFromOfferings(activity, tier, interval, cachedOfferings, onSuccess, onError)
            return
        }
        fetchOfferings(
            onSuccess = { offerings ->
                purchaseTierFromOfferings(activity, tier, interval, offerings, onSuccess, onError)
            },
            onError = onError
        )
    }

    fun packageForTier(
        tier: SubscriptionTier,
        interval: BillingInterval,
        offerings: Offerings?
    ): Package? {
        if (offerings == null) return null
        val productId = SubscriptionCatalog.productId(tier, interval) ?: return null
        val offering = offerings.current ?: return null
        return offering.availablePackages.firstOrNull { storePackage ->
            storePackage.product.id == productId
        }
    }

    private fun purchaseTierFromOfferings(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval,
        offerings: Offerings,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val rcPackage = packageForTier(tier, interval, offerings)
        if (rcPackage == null) {
            _billingErrorKey.value = "billing_product_unavailable"
            onError("billing_product_unavailable")
            return
        }
        purchasePackage(activity, rcPackage, onSuccess, onError)
    }

    private fun refreshCustomerInfo() {
        if (!RevenueCatConfig.isConfigured) return
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { },
            onSuccess = { customerInfo ->
                _customerInfo.value = customerInfo
            }
        )
    }

    private fun mapPurchasesError(error: PurchasesError): String {
        return when (error.code) {
            PurchasesErrorCode.PurchaseNotAllowedError,
            PurchasesErrorCode.PurchaseInvalidError,
            PurchasesErrorCode.ProductNotAvailableForPurchaseError -> "billing_product_unavailable"

            PurchasesErrorCode.NetworkError,
            PurchasesErrorCode.StoreProblemError -> "billing_not_ready"

            else -> "billing_flow_failed"
        }
    }
}
