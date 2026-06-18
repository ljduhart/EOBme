package app.eob.me.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.billing.BillingRepository
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.remote.FirestoreSubscriptionRepository
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Merges Google Play Billing, Firestore `users/{uid}.subscriptionTier`, and RevenueCat
 * entitlements into [subscriptionState]. Hub UI reads tier through [EobViewModel.applySubscriptionState].
 */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val billingRepository: BillingRepository = BillingRepository(application.applicationContext)
    private val subscriptionRepository: SubscriptionRepository = FirestoreSubscriptionRepository()

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _currentTier = MutableStateFlow(SubscriptionTier.Free)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    val billingNoticeKey: StateFlow<String?> = billingRepository.billingErrorKey

    private var observeJob: Job? = null
    private var playTierObserveJob: Job? = null
    private var boundUserId: String? = null

    fun bindUser(userId: String) {
        if (boundUserId == userId && observeJob?.isActive == true) return
        boundUserId = userId
        observeJob?.cancel()
        refreshSubscriptionState()
        if (userId.isBlank()) {
            _subscriptionState.value = SubscriptionState.Free
            return
        }
        _subscriptionState.value = SubscriptionState.Loading
        observeJob = viewModelScope.launch {
            combine(
                billingRepository.activePlayTier,
                subscriptionRepository.observeSubscriptionTier(userId),
                currentTier
            ) { playTier, firestoreSnapshot, revenueCatTier ->
                mergeSubscriptionState(playTier, firestoreSnapshot, revenueCatTier)
            }.collect { merged ->
                _subscriptionState.value = merged
            }
        }
    }

    fun refreshSubscriptionState() {
        viewModelScope.launch {
            try {
                val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
                val activeEntitlements = customerInfo.entitlements.active.keys
                _currentTier.value = when {
                    activeEntitlements.contains("gold") -> SubscriptionTier.Gold
                    activeEntitlements.contains("silver") -> SubscriptionTier.Silver
                    else -> SubscriptionTier.Free
                }
            } catch (_: Exception) {
                _currentTier.value = SubscriptionTier.Free
            }
        }
    }

    fun startBilling() {
        billingRepository.startConnection()
        refreshSubscriptionState()
        playTierObserveJob?.cancel()
        playTierObserveJob = viewModelScope.launch {
            var previousPlayTier: SubscriptionTier? = null
            billingRepository.activePlayTier.collect { playTier ->
                val resolved = playTier ?: SubscriptionTier.Free
                if (resolved != previousPlayTier) {
                    previousPlayTier = resolved
                    refreshSubscriptionState()
                }
            }
        }
    }

    fun stopBilling() {
        billingRepository.endConnection()
        observeJob?.cancel()
        observeJob = null
        playTierObserveJob?.cancel()
        playTierObserveJob = null
        boundUserId = null
        _currentTier.value = SubscriptionTier.Free
        _subscriptionState.value = SubscriptionState.Loading
    }

    fun launchPurchaseFlow(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ) {
        billingRepository.launchBillingFlow(activity, tier, interval)
    }

    fun clearBillingNotice() {
        billingRepository.clearBillingNotice()
    }

    private fun mergeSubscriptionState(
        playTier: SubscriptionTier?,
        firestoreSnapshot: FirestoreSubscriptionSnapshot,
        revenueCatTier: SubscriptionTier
    ): SubscriptionState = mergeSubscriptionStatus(playTier, firestoreSnapshot, revenueCatTier)
}

internal fun mergeSubscriptionStatus(
    playTier: SubscriptionTier?,
    firestoreSnapshot: FirestoreSubscriptionSnapshot,
    revenueCatTier: SubscriptionTier = SubscriptionTier.Free
): SubscriptionState {
    val firestoreResolved = firestoreSnapshot is FirestoreSubscriptionSnapshot.Resolved
    val firestoreTier = (firestoreSnapshot as? FirestoreSubscriptionSnapshot.Resolved)?.tier
        ?: SubscriptionTier.Free

    if (playTier == SubscriptionTier.Gold || firestoreTier == SubscriptionTier.Gold || revenueCatTier == SubscriptionTier.Gold) {
        return SubscriptionState.Gold
    }

    if (playTier == SubscriptionTier.Silver || firestoreTier == SubscriptionTier.Silver || revenueCatTier == SubscriptionTier.Silver) {
        return SubscriptionState.Silver
    }

    if (firestoreSnapshot is FirestoreSubscriptionSnapshot.Error && playTier == SubscriptionTier.Free) {
        return SubscriptionState.Error(firestoreSnapshot.message)
    }

    if (playTier != null && firestoreResolved) {
        return SubscriptionState.Free
    }

    if (revenueCatTier != SubscriptionTier.Free) {
        return when (revenueCatTier) {
            SubscriptionTier.Gold -> SubscriptionState.Gold
            SubscriptionTier.Silver -> SubscriptionState.Silver
            SubscriptionTier.Free -> SubscriptionState.Free
        }
    }

    return SubscriptionState.Loading
}

internal fun subscriptionStateToTier(state: SubscriptionState): SubscriptionTier? = when (state) {
    SubscriptionState.Gold -> SubscriptionTier.Gold
    SubscriptionState.Silver -> SubscriptionTier.Silver
    SubscriptionState.Free -> SubscriptionTier.Free
    SubscriptionState.Loading, is SubscriptionState.Error -> null
}
