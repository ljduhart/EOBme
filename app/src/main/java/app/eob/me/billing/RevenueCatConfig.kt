package app.eob.me.billing

import app.eob.me.BuildConfig

object RevenueCatConfig {
    const val ENTITLEMENT_GOLD = "gold"
    const val ENTITLEMENT_SILVER = "silver"

    val publicApiKey: String = BuildConfig.REVENUECAT_API_KEY

    val isConfigured: Boolean
        get() = publicApiKey.isNotBlank()
}
