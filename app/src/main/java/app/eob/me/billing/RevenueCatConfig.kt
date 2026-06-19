package app.eob.me.billing

/**
 * RevenueCat project configuration for EOBme Google Play subscriptions.
 * The public API key is safe to embed in the client; entitlement identifiers must
 * match the RevenueCat dashboard (gold / silver).
 */
object RevenueCatConfig {
    const val PUBLIC_API_KEY: String = "goog_rmhYQIPDsEWnEBFWUzMRYYlpYMo"
    const val ENTITLEMENT_GOLD: String = "gold"
    const val ENTITLEMENT_SILVER: String = "silver"
}
