package app.eob.me.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.eob.me.EobApplication
import app.eob.me.billing.RevenueCatManager
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import kotlinx.coroutines.flow.StateFlow

/**
 * Launches RevenueCat purchases for hub paywall selections.
 * Subscription tier state is applied in [EobViewModel.bindRevenueCat].
 */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val revenueCatManager: RevenueCatManager =
        (application as? EobApplication)?.revenueCatManager ?: RevenueCatManager()

    val billingNoticeKey: StateFlow<String?> = revenueCatManager.billingErrorKey

    fun launchPurchaseFlow(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ) {
        revenueCatManager.purchaseTier(
            activity = activity,
            tier = tier,
            interval = interval,
            onSuccess = { },
            onError = { }
        )
    }
}
