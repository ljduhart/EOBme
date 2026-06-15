package app.eob.me.billing

import android.app.Activity
import android.content.Context
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
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
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing for EOBme Silver and Gold tiers (monthly + annual SKUs).
 * Active tier is exposed via [activePlayTier] for [app.eob.me.viewmodel.SubscriptionViewModel].
 */
class BillingRepository(
    private val context: Context
) : PurchasesUpdatedListener {

    private val _activePlayTier = MutableStateFlow<SubscriptionTier?>(null)
    val activePlayTier: StateFlow<SubscriptionTier?> = _activePlayTier.asStateFlow()

    private val _billingErrorKey = MutableStateFlow<String?>(null)
    val billingErrorKey: StateFlow<String?> = _billingErrorKey.asStateFlow()

    private val productDetailsById = mutableMapOf<String, ProductDetails>()
    private var pendingLaunch: PendingBillingLaunch? = null

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
            querySubscriptionProductDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refreshPurchases()
                    querySubscriptionProductDetails()
                } else {
                    _activePlayTier.value = null
                    _billingErrorKey.value = "billing_not_ready"
                }
            }

            override fun onBillingServiceDisconnected() {
                _activePlayTier.value = null
            }
        })
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        productDetailsById.clear()
        pendingLaunch = null
        _activePlayTier.value = null
        _billingErrorKey.value = null
    }

    fun launchBillingFlow(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ) {
        _billingErrorKey.value = null
        val productId = SubscriptionCatalog.productId(tier, interval)
        if (productId == null) {
            _billingErrorKey.value = "billing_product_unavailable"
            return
        }
        if (!billingClient.isReady) {
            pendingLaunch = PendingBillingLaunch(WeakReference(activity), tier, interval)
            _billingErrorKey.value = "billing_not_ready"
            startConnection()
            return
        }
        val product = productDetailsById[productId]
        if (product == null) {
            pendingLaunch = PendingBillingLaunch(WeakReference(activity), tier, interval)
            querySubscriptionProductDetails()
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
                _activePlayTier.value = null
                _billingErrorKey.value = "billing_not_ready"
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var resolvedTier = SubscriptionTier.Free
        purchases.filter { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }.forEach { purchase ->
            purchase.products.forEach { productId ->
                SubscriptionCatalog.tierForProductId(productId)?.let { tier ->
                    resolvedTier = SubscriptionCatalog.highestTier(resolvedTier, tier)
                }
            }
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
        _activePlayTier.value = resolvedTier
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

    private fun querySubscriptionProductDetails() {
        if (!billingClient.isReady) return
        val productList = SubscriptionCatalog.ALL_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsById.clear()
                details.productDetailsList.forEach { product ->
                    productDetailsById[product.productId] = product
                }
                val pending = pendingLaunch
                pendingLaunch = null
                val pendingActivity = pending?.activityRef?.get()
                val pendingProductId = pending?.let {
                    SubscriptionCatalog.productId(it.tier, it.interval)
                }
                val pendingProduct = pendingProductId?.let(productDetailsById::get)
                when {
                    pending != null &&
                        pendingActivity != null &&
                        !pendingActivity.isFinishing &&
                        !pendingActivity.isDestroyed &&
                        pendingProduct != null -> {
                        launchBillingFlowInternal(pendingActivity, pendingProduct)
                    }

                    pending != null && pendingProduct == null -> {
                        _billingErrorKey.value = "billing_product_unavailable"
                    }
                }
            } else {
                _billingErrorKey.value = "billing_product_unavailable"
            }
        }
    }

    private fun launchBillingFlowInternal(activity: Activity, product: ProductDetails) {
        if (activity.isFinishing || activity.isDestroyed) return
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

    private data class PendingBillingLaunch(
        val activityRef: WeakReference<Activity>,
        val tier: SubscriptionTier,
        val interval: BillingInterval
    )

    companion object {
        /** @deprecated Use [SubscriptionCatalog.LEGACY_PREMIUM_PRODUCT_ID]. */
        const val PREMIUM_PRODUCT_ID: String = SubscriptionCatalog.LEGACY_PREMIUM_PRODUCT_ID
    }
}
