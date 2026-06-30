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
 * Google Play Billing for EOBme Silver and Gold subscriptions (parent SKU + base plan).
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

    fun clearBillingNotice() {
        _billingErrorKey.value = null
    }

    fun emitBillingNotice(noticeKey: String) {
        _billingErrorKey.value = noticeKey
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
        val offerRef = SubscriptionCatalog.offerRef(tier, interval)
        if (offerRef == null) {
            _billingErrorKey.value = "billing_product_unavailable"
            return
        }
        if (!billingClient.isReady) {
            pendingLaunch = PendingBillingLaunch(WeakReference(activity), tier, interval)
            _billingErrorKey.value = "billing_not_ready"
            startConnection()
            return
        }
        val product = productDetailsById[offerRef.subscriptionProductId]
        if (product == null) {
            pendingLaunch = PendingBillingLaunch(WeakReference(activity), tier, interval)
            querySubscriptionProductDetails()
            return
        }
        launchBillingFlowInternal(activity, product, offerRef.basePlanId)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                handlePurchases(purchases.orEmpty())
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingErrorKey.value = "billing_user_canceled"
            }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _billingErrorKey.value = "billing_already_subscribed"
                refreshPurchases()
            }

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
        val productList = SubscriptionCatalog.ALL_SUBSCRIPTION_PRODUCT_IDS.map { productId ->
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
                if (pending == null) return@queryProductDetailsAsync
                val pendingActivity = pending.activityRef.get()
                val pendingOfferRef = SubscriptionCatalog.offerRef(pending.tier, pending.interval)
                val pendingProduct = pendingOfferRef?.subscriptionProductId?.let(productDetailsById::get)
                when {
                    pendingActivity == null ||
                        pendingActivity.isFinishing ||
                        pendingActivity.isDestroyed ||
                        pendingOfferRef == null ||
                        pendingProduct == null -> {
                        _billingErrorKey.value = "billing_product_unavailable"
                    }

                    else -> {
                        launchBillingFlowInternal(
                            activity = pendingActivity,
                            product = pendingProduct,
                            basePlanId = pendingOfferRef.basePlanId
                        )
                    }
                }
            } else {
                _billingErrorKey.value = "billing_product_unavailable"
            }
        }
    }

    private fun launchBillingFlowInternal(
        activity: Activity,
        product: ProductDetails,
        basePlanId: String
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            _billingErrorKey.value = "billing_flow_failed"
            return
        }
        val offerToken = resolveOfferToken(product, basePlanId)
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
        when (launchResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refreshPurchases()
            else -> _billingErrorKey.value = "billing_flow_failed"
        }
    }

    companion object {
        const val PREMIUM_PRODUCT_ID: String = SubscriptionCatalog.LEGACY_PREMIUM_PRODUCT_ID

        internal fun resolveOfferToken(product: ProductDetails, basePlanId: String): String? {
            return product.subscriptionOfferDetails
                ?.firstOrNull { offer -> offer.basePlanId == basePlanId }
                ?.offerToken
        }
    }

    private data class PendingBillingLaunch(
        val activityRef: WeakReference<Activity>,
        val tier: SubscriptionTier,
        val interval: BillingInterval
    )
}
