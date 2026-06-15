package app.eob.me.billing

import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot

internal fun mergeSubscriptionStatus(
    playTier: SubscriptionTier?,
    firestoreSnapshot: FirestoreSubscriptionSnapshot
): SubscriptionState {
    val firestoreResolved = firestoreSnapshot is FirestoreSubscriptionSnapshot.Resolved
    val firestoreTier = (firestoreSnapshot as? FirestoreSubscriptionSnapshot.Resolved)?.tier
        ?: SubscriptionTier.Free

    if (playTier == SubscriptionTier.Gold || firestoreTier == SubscriptionTier.Gold) {
        return SubscriptionState.Gold
    }

    if (playTier == SubscriptionTier.Silver || firestoreTier == SubscriptionTier.Silver) {
        return SubscriptionState.Silver
    }

    if (firestoreSnapshot is FirestoreSubscriptionSnapshot.Error && playTier == SubscriptionTier.Free) {
        return SubscriptionState.Error(firestoreSnapshot.message)
    }

    if (playTier != null && firestoreResolved) {
        return SubscriptionState.Free
    }

    return SubscriptionState.Loading
}

internal fun subscriptionStateToTier(state: SubscriptionState): SubscriptionTier? = when (state) {
    SubscriptionState.Gold -> SubscriptionTier.Gold
    SubscriptionState.Silver -> SubscriptionTier.Silver
    SubscriptionState.Free -> SubscriptionTier.Free
    SubscriptionState.Loading, is SubscriptionState.Error -> null
}
