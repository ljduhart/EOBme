package app.eob.me.data.repository

import kotlinx.coroutines.flow.Flow

sealed interface FirestorePremiumSnapshot {
    data object Loading : FirestorePremiumSnapshot

    data class Resolved(val isPremium: Boolean) : FirestorePremiumSnapshot

    data class Error(val message: String) : FirestorePremiumSnapshot
}

/**
 * Remote subscription entitlement from Firestore `users/{userId}.isPremium`.
 */
interface SubscriptionRepository {
    fun observeIsPremium(userId: String): Flow<FirestorePremiumSnapshot>
}
