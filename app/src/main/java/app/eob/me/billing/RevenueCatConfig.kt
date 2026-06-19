package app.eob.me.billing

import android.content.Context
import android.content.pm.ApplicationInfo
import app.eob.me.data.SubscriptionTier

/**
 * RevenueCat project configuration for EOBme Google Play subscriptions.
 * The public API key is safe to embed in the client; entitlement identifiers must
 * match the RevenueCat dashboard (gold / silver).
 */
object RevenueCatConfig {
    const val PUBLIC_API_KEY: String = "goog_rmhYQIPDsEWnEBFWUzMRYYlpYMo"
    const val ENTITLEMENT_GOLD: String = "gold"
    const val ENTITLEMENT_SILVER: String = "silver"

    const val ATTRIBUTE_APP_BUILD_VARIANT: String = "app_build_variant"
    const val ATTRIBUTE_TAX_VAULT_STATUS: String = "tax_vault_status"

    fun resolveAppBuildVariant(context: Context): String {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return if (isDebuggable) "debug" else "closed_testing"
    }
}
