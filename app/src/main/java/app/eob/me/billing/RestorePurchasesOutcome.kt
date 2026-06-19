package app.eob.me.billing

sealed interface RestorePurchasesOutcome {
    data class Success(val hasActiveSubscription: Boolean) : RestorePurchasesOutcome
    data class Failure(val message: String) : RestorePurchasesOutcome
}
