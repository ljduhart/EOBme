package app.eob.me.billing

/**
 * Unified subscription status for the hub. [app.eob.me.viewmodel.SubscriptionViewModel]
 * merges Google Play Billing and Firestore `users/{uid}.subscriptionTier` / legacy `isPremium`.
 */
sealed interface SubscriptionState {
    data object Loading : SubscriptionState

    data object Gold : SubscriptionState

    data object Silver : SubscriptionState

    data object Free : SubscriptionState

    data class Error(val message: String) : SubscriptionState
}
