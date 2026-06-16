package app.eob.me.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.billing.BillingRepository
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.remote.FirestoreSubscriptionRepository
import app.eob.me.data.repository.FirestorePremiumSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Merges Google Play Billing and Firestore `users/{uid}.isPremium` into [subscriptionState].
 * Hub UI continues to read subscription tier through [EobViewModel.applySubscriptionState].
 *
 * Constructor matches [AppViewModel]: single [Application] parameter so `viewModel()` can
 * instantiate this class through [androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory].
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
                billingRepository.isPlayPremium,
                subscriptionRepository.observeIsPremium(userId)
            ) { playPremium, firestoreSnapshot ->
                mergeSubscriptionState(playPremium, firestoreSnapshot)
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

    fun launchPurchaseFlow(activity: Activity) {
        billingRepository.launchBillingFlow(activity)
    }

    private fun mergeSubscriptionState(
        playPremium: Boolean?,
        firestoreSnapshot: FirestorePremiumSnapshot
    ): SubscriptionState = mergeSubscriptionStatus(playPremium, firestoreSnapshot)
}

internal fun mergeSubscriptionStatus(
    playPremium: Boolean?,
    firestoreSnapshot: FirestorePremiumSnapshot
): SubscriptionState {
    val firestoreResolved = firestoreSnapshot is FirestorePremiumSnapshot.Resolved
    val firestorePremium = (firestoreSnapshot as? FirestorePremiumSnapshot.Resolved)?.isPremium == true

    if (playPremium == true || firestorePremium) {
        return SubscriptionState.Gold
    }

    if (firestoreSnapshot is FirestorePremiumSnapshot.Error && playPremium == false) {
        return SubscriptionState.Error(firestoreSnapshot.message)
    }

    if (playPremium != null && firestoreResolved) {
        return SubscriptionState.Free
    }

    return SubscriptionState.Loading
}
