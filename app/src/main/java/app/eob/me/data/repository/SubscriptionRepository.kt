package app.eob.me.data.repository

import app.eob.me.data.SubscriptionTier
import kotlinx.coroutines.flow.Flow

sealed interface FirestoreSubscriptionSnapshot {
    data object Loading : FirestoreSubscriptionSnapshot

    data class Resolved(val tier: SubscriptionTier) : FirestoreSubscriptionSnapshot

    data class Error(val message: String) : FirestoreSubscriptionSnapshot
}

/**
 * Remote subscription entitlement from Firestore `users/{userId}.subscriptionTier`.
 */
interface SubscriptionRepository {
    fun observeSubscriptionTier(userId: String): Flow<FirestoreSubscriptionSnapshot>
}
