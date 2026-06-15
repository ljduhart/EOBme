package app.eob.me.data.remote

import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Listens to `users/{userId}` and maps `subscriptionTier`, with legacy `isPremium` fallback.
 */
class FirestoreSubscriptionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SubscriptionRepository {

    override fun observeSubscriptionTier(userId: String): Flow<FirestoreSubscriptionSnapshot> = callbackFlow {
        if (userId.isBlank()) {
            trySend(FirestoreSubscriptionSnapshot.Resolved(tier = SubscriptionTier.Free))
            close()
            return@callbackFlow
        }

        trySend(FirestoreSubscriptionSnapshot.Loading)
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        trySend(
                            FirestoreSubscriptionSnapshot.Error(
                                error.localizedMessage ?: "Firestore subscription listener failed."
                            )
                        )
                    }

                    snapshot != null && snapshot.exists() -> {
                        trySend(
                            FirestoreSubscriptionSnapshot.Resolved(
                                tier = resolveTier(snapshot.getString(FIELD_SUBSCRIPTION_TIER), snapshot.getBoolean(FIELD_IS_PREMIUM))
                            )
                        )
                    }

                    else -> {
                        trySend(FirestoreSubscriptionSnapshot.Resolved(tier = SubscriptionTier.Free))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    private fun resolveTier(rawTier: String?, legacyPremium: Boolean?): SubscriptionTier {
        val normalized = rawTier?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "gold" -> SubscriptionTier.Gold
            "silver" -> SubscriptionTier.Silver
            "free" -> SubscriptionTier.Free
            else -> when {
                legacyPremium == true -> SubscriptionTier.Gold
                else -> SubscriptionTier.Free
            }
        }
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_SUBSCRIPTION_TIER = "subscriptionTier"
        const val FIELD_IS_PREMIUM = "isPremium"
    }
}
