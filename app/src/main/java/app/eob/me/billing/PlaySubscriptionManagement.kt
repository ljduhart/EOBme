package app.eob.me.billing

import android.content.Intent
import android.net.Uri

/**
 * Opens Google Play subscription management for cancel, pause, or plan changes.
 * Play Billing requires subscription changes to be completed in the Play Store UI.
 */
object PlaySubscriptionManagement {
    fun managementUri(applicationId: String, subscriptionProductId: String?): Uri {
        val builder = Uri.parse("https://play.google.com/store/account/subscriptions").buildUpon()
            .appendQueryParameter("package", applicationId)
        if (!subscriptionProductId.isNullOrBlank()) {
            builder.appendQueryParameter("sku", subscriptionProductId)
        }
        return builder.build()
    }

    fun buildManagementIntent(applicationId: String, subscriptionProductId: String?): Intent {
        return Intent(Intent.ACTION_VIEW, managementUri(applicationId, subscriptionProductId))
    }
}
