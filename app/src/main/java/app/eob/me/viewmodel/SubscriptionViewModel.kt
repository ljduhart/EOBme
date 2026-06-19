package app.eob.me.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.billing.BillingRepository
import app.eob.me.billing.PaywallPricing
import app.eob.me.billing.RestorePurchasesOutcome
import app.eob.me.billing.RevenueCatBillingRepository
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.BillingInterval
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.remote.FirestoreSubscriptionRepository
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.data.repository.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Merges Google Play Billing, Firestore `users/{uid}.subscriptionTier`, and RevenueCat
 * entitlements into [subscriptionState]. Hub UI reads tier through [EobViewModel.applySubscriptionState].
 *
 * RevenueCat SDK access is isolated in [RevenueCatBillingRepository]; this ViewModel only
 * orchestrates repository streams and purchase delegation.
 */
class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val billingRepository: BillingRepository = BillingRepository(application.applicationContext)
    private val revenueCatBillingRepository: RevenueCatBillingRepository =
        RevenueCatBillingRepository(application.applicationContext)
    private val subscriptionRepository: SubscriptionRepository = FirestoreSubscriptionRepository()

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    val paywallPricing: StateFlow<PaywallPricing> = revenueCatBillingRepository.paywallPricing

    val billingNoticeKey: StateFlow<String?> = combine(
        billingRepository.billingErrorKey,
        revenueCatBillingRepository.billingNoticeKey
    ) { playNotice, revenueCatNotice ->
        playNotice ?: revenueCatNotice
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private var observeJob: Job? = null
    private var playTierObserveJob: Job? = null
    private var boundUserId: String? = null

    fun bindUser(userId: String, email: String = "", displayName: String = "") {
        if (email.isNotBlank() || displayName.isNotBlank()) {
            revenueCatBillingRepository.attachUserMetadata(email = email, displayName = displayName)
        }
        if (boundUserId == userId && observeJob?.isActive == true) return
        boundUserId = userId
        observeJob?.cancel()
        if (userId.isBlank()) {
            viewModelScope.launch {
                revenueCatBillingRepository.logOut()
                _subscriptionState.value = SubscriptionState.Free
            }
            return
        }
        _subscriptionState.value = SubscriptionState.Loading
        observeJob = viewModelScope.launch {
            revenueCatBillingRepository.logIn(userId)
            revenueCatBillingRepository.refreshCustomerInfo()
            revenueCatBillingRepository.refreshOfferings()
            combine(
                billingRepository.activePlayTier,
                subscriptionRepository.observeSubscriptionTier(userId),
                revenueCatBillingRepository.activeTier
            ) { playTier, firestoreSnapshot, revenueCatTier ->
                mergeSubscriptionState(playTier, firestoreSnapshot, revenueCatTier)
            }.collect { merged ->
                _subscriptionState.value = merged
            }
        }
    }

    fun startBilling() {
        revenueCatBillingRepository.startListening()
        billingRepository.startConnection()
        viewModelScope.launch {
            revenueCatBillingRepository.refreshCustomerInfo()
            revenueCatBillingRepository.refreshOfferings()
        }
        playTierObserveJob?.cancel()
        playTierObserveJob = viewModelScope.launch {
            var previousPlayTier: SubscriptionTier? = null
            billingRepository.activePlayTier.collect { playTier ->
                val resolved = playTier ?: SubscriptionTier.Free
                if (resolved != previousPlayTier) {
                    previousPlayTier = resolved
                    revenueCatBillingRepository.refreshCustomerInfo()
                }
            }
        }
    }

    fun stopBilling() {
        billingRepository.endConnection()
        revenueCatBillingRepository.stopListening()
        observeJob?.cancel()
        observeJob = null
        playTierObserveJob?.cancel()
        playTierObserveJob = null
        boundUserId = null
        viewModelScope.launch {
            revenueCatBillingRepository.logOut()
            _subscriptionState.value = SubscriptionState.Loading
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        tier: SubscriptionTier,
        interval: BillingInterval
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            billingRepository.emitBillingNotice("billing_flow_failed")
            return
        }
        clearBillingNotice()
        viewModelScope.launch {
            val handledByRevenueCat = revenueCatBillingRepository.purchase(activity, tier, interval)
            if (!handledByRevenueCat) {
                billingRepository.launchBillingFlow(activity, tier, interval)
                return@launch
            }
            billingRepository.startConnection()
            revenueCatBillingRepository.refreshCustomerInfo()
        }
    }

    fun clearBillingNotice() {
        billingRepository.clearBillingNotice()
        revenueCatBillingRepository.clearBillingNotice()
    }

    fun restoreUserPurchases(onSuccess: (Boolean) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            when (val outcome = revenueCatBillingRepository.restoreUserPurchases()) {
                is RestorePurchasesOutcome.Success -> {
                    billingRepository.startConnection()
                    revenueCatBillingRepository.refreshCustomerInfo()
                    onSuccess(outcome.hasActiveSubscription)
                }

                is RestorePurchasesOutcome.Failure -> {
                    onFailure(outcome.message)
                }
            }
        }
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
