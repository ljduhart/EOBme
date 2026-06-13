package app.eob.me.data.remote

import app.eob.me.data.repository.FirestorePremiumSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Listens to `users/{userId}` and maps the `isPremium` boolean field.
 */
class FirestoreSubscriptionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : SubscriptionRepository {

    override fun observeIsPremium(userId: String): Flow<FirestorePremiumSnapshot> = callbackFlow {
        if (userId.isBlank()) {
            trySend(FirestorePremiumSnapshot.Resolved(isPremium = false))
            close()
            return@callbackFlow
        }

        trySend(FirestorePremiumSnapshot.Loading)
        val registration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> {
                        trySend(
                            FirestorePremiumSnapshot.Error(
                                error.localizedMessage ?: "Firestore subscription listener failed."
                            )
                        )
                    }

                    snapshot != null && snapshot.exists() -> {
                        trySend(
                            FirestorePremiumSnapshot.Resolved(
                                isPremium = snapshot.getBoolean(FIELD_IS_PREMIUM) == true
                            )
                        )
                    }

                    else -> {
                        trySend(FirestorePremiumSnapshot.Resolved(isPremium = false))
                    }
                }
            }
        awaitClose { registration.remove() }
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val FIELD_IS_PREMIUM = "isPremium"
    }
}
