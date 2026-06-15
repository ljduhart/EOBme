package app.eob.me.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.billing.BillingRepository
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.remote.FirestoreSubscriptionRepository
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Merges Google Play Billing and Firestore `users/{uid}.subscriptionTier` into [subscriptionState].
 * Hub UI continues to read subscription tier through [EobViewModel.applySubscriptionState].
 */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val billingRepository: BillingRepository = BillingRepository(application.applicationContext)
    private val subscriptionRepository: SubscriptionRepository = FirestoreSubscriptionRepository()

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    val billingNoticeKey: StateFlow<String?> = billingRepository.billingErrorKey

    private var observeJob: Job? = null
    private var boundUserId: String? = null

    fun bindUser(userId: String) {
        if (boundUserId == userId && observeJob?.isActive == true) return
        boundUserId = userId
        observeJob?.cancel()
        _subscriptionState.value = SubscriptionState.Loading
        observeJob = viewModelScope.launch {
            combine(
                billingRepository.activePlayTier,
                subscriptionRepository.observeSubscriptionTier(userId)
            ) { playTier, firestoreSnapshot ->
                mergeSubscriptionState(playTier, firestoreSnapshot)
            }.collect { merged ->
                _subscriptionState.value = merged
            }
        }
    }

    fun startBilling() {
        billingRepository.startConnection()
    }

    fun stopBilling() {
        billingRepository.endConnection()
        observeJob?.cancel()
        observeJob = null
        boundUserId = null
        _subscriptionState.value = SubscriptionState.Loading
    }

    fun launchPurchaseFlow(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ) {
        billingRepository.launchBillingFlow(activity, tier, interval)
    }

    private fun mergeSubscriptionState(
        playTier: SubscriptionTier?,
        firestoreSnapshot: FirestoreSubscriptionSnapshot
    ): SubscriptionState = mergeSubscriptionStatus(playTier, firestoreSnapshot)
}

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
