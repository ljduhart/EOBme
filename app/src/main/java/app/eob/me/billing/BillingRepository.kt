package app.eob.me.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing integration for the `premium_access_tier` subscription SKU.
 * Purchase state is exposed via [isPlayPremium] for [app.eob.me.viewmodel.SubscriptionViewModel].
 */
class BillingRepository(
    private val context: Context
) : PurchasesUpdatedListener {

    private val _isPlayPremium = MutableStateFlow<Boolean?>(null)
    val isPlayPremium: StateFlow<Boolean?> = _isPlayPremium.asStateFlow()

    private val _billingErrorKey = MutableStateFlow<String?>(null)
    val billingErrorKey: StateFlow<String?> = _billingErrorKey.asStateFlow()

    private var premiumProductDetails: ProductDetails? = null
    private var pendingLaunchActivity: Activity? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        _billingErrorKey.value = null
        if (billingClient.isReady) {
            refreshPurchases()
            queryPremiumProductDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshPurchases()
                    queryPremiumProductDetails()
                } else {
                    _isPlayPremium.value = false
                    _billingErrorKey.value = "billing_not_ready"
                }
            }

            override fun onBillingServiceDisconnected() {
                _isPlayPremium.value = false
            }
        })
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        premiumProductDetails = null
        pendingLaunchActivity = null
        _isPlayPremium.value = null
        _billingErrorKey.value = null
    }

    fun launchBillingFlow(activity: Activity) {
        _billingErrorKey.value = null
        if (!billingClient.isReady) {
            _billingErrorKey.value = "billing_not_ready"
            startConnection()
            return
        }
        val product = premiumProductDetails
        if (product == null) {
            pendingLaunchActivity = activity
            queryPremiumProductDetails()
            return
        }
        launchBillingFlowInternal(activity, product)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handlePurchases(purchases.orEmpty())
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> Unit

            else -> {
                _billingErrorKey.value = "billing_flow_failed"
            }
        }
    }

    private fun refreshPurchases() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases)
            } else {
                _isPlayPremium.value = false
                _billingErrorKey.value = "billing_not_ready"
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val activePremium = purchases.any { purchase ->
            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        purchases.filter { purchase ->
            purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                !purchase.isAcknowledged
        }.forEach { purchase ->
            acknowledgePurchase(purchase)
        }
        _isPlayPremium.value = activePremium
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (!billingClient.isReady) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _billingErrorKey.value = "billing_flow_failed"
            }
        }
    }

    private fun queryPremiumProductDetails() {
        if (!billingClient.isReady) return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                premiumProductDetails = details.productDetailsList.firstOrNull()
                val product = premiumProductDetails
                val pendingActivity = pendingLaunchActivity
                pendingLaunchActivity = null
                when {
                    product == null -> {
                        _billingErrorKey.value = "billing_product_unavailable"
                    }

                    pendingActivity != null -> {
                        launchBillingFlowInternal(pendingActivity, product)
                    }
                }
            } else {
                _billingErrorKey.value = "billing_product_unavailable"
            }
        }
    }

    private fun launchBillingFlowInternal(activity: Activity, product: ProductDetails) {
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken.isNullOrBlank()) {
            _billingErrorKey.value = "billing_product_unavailable"
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        val launchResult = billingClient.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingErrorKey.value = "billing_flow_failed"
        }
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_access_tier"
    }
}
