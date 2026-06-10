package app.eob.me.billing

import android.app.Activity
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
import app.eob.me.data.SubscriptionTier

/**
 * Google Play Billing wrapper for subscription management from Settings.
 */
class PlayBillingManager(
    private val activity: Activity,
    private val onTierChanged: (SubscriptionTier) -> Unit,
    private val onBillingMessage: (String) -> Unit
) : PurchasesUpdatedListener {
    private var premiumProduct: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun start() {
        if (billingClient.isReady) {
            refreshSubscriptionTier()
            queryPremiumProduct()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshSubscriptionTier()
                    queryPremiumProduct()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun launchManageSubscription() {
        if (!billingClient.isReady) {
            onBillingMessage("billing_not_ready")
            start()
            return
        }
        val product = premiumProduct
        if (product == null) {
            queryPremiumProduct(andLaunch = true)
            return
        }
        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken.isNullOrBlank()) {
            onBillingMessage("billing_product_unavailable")
            return
        }
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            refreshSubscriptionTier()
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            onBillingMessage("billing_flow_failed")
        }
    }

    fun refreshSubscriptionTier() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val isPremium = purchases.any { purchase ->
                    purchase.products.contains(PREMIUM_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                onTierChanged(if (isPremium) SubscriptionTier.Premium else SubscriptionTier.Free)
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun queryPremiumProduct(andLaunch: Boolean = false) {
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
                premiumProduct = details.productDetailsList.firstOrNull()
                if (andLaunch) launchManageSubscription()
            }
        }
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "eobme_premium"
    }
}
