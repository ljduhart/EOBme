package app.eob.me.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.AppLanguage
import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.BentoSnapshotExtractor
import app.eob.me.data.CptBentoSnapshot
import app.eob.me.data.CptCodeEntry
import app.eob.me.data.CptCategory
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobInsuranceNews
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.CareTeamCardDisplayState
import app.eob.me.data.CareTeamStateExtractor
import app.eob.me.data.EobHistoryPaymentFilter
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.HistoryTimelineRow
import app.eob.me.data.InsuranceCardDisplay
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.ProviderDirectoryAssurance
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.InsuranceNewsCarrierHubItem
import app.eob.me.data.MajorInsuranceCarrier
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.NewsRelease
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.data.ProviderSummary
import app.eob.me.data.UserProfile
import app.eob.me.data.YtdExpenseData
import app.eob.me.data.YearlyHealthCostSummary
import app.eob.me.data.AppLockTimeout
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.CameraScanDocumentType
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.HubSettingsState
import app.eob.me.data.HubSettingsStore
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.SettingsTab
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.EobRepository
import app.eob.me.network.InsuranceNewsRotation
import app.eob.me.network.RetrofitClient
import app.eob.me.network.RssNewsMapper
import app.eob.me.util.CacheSizeCalculator
import app.eob.me.util.NetworkUploadGate
import app.eob.me.util.HubCrashlyticsGate
import app.eob.me.util.OcrProcessor
import app.eob.me.ui.history.HistoryPagination
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private val FIVE_DIGIT_PIN = Regex("^\\d{5}$")

data class HubUiState(
    val selectedRecord: EobRecord? = null,
    val uploadNotice: String = "",
    val appealLetter: String = "",
    val appointments: List<DoctorAppointment> = emptyList(),
    val preferredDoctors: Map<CareTeamProviderType, PreferredDoctor> = CareTeamProviderType.displayOrder
        .associateWith { PreferredDoctor(type = it) },
    val isLoadingInvoice: Boolean = false,
    val invoiceProcessingPhase: InvoiceProcessingPhase = InvoiceProcessingPhase.Idle,
    val historyBentoFilter: HistoryBentoFilter = HistoryBentoFilter.All,
    val historyPaymentFilter: EobHistoryPaymentFilter = EobHistoryPaymentFilter.All,
    val historyProviderSearch: String = "",
    val historyPage: Int = 0,
    val calendarExpanded: Boolean = false,
    val selectedInsuranceArticle: InsuranceArticle? = null,
    val selectedNewsCarrier: MajorInsuranceCarrier = MajorInsuranceCarrier.UnitedHealthcare,
    val ytdBentoViewMode: YtdBentoViewMode = YtdBentoViewMode.CostOverview,
    val selectedCptCategory: CptCategory = CptCategory.OfficeVisit,
    val firebaseSyncStatus: FirebaseSyncStatus = FirebaseSyncStatus(isConfigured = false),
    val newsFeedRevision: Int = 0,
    val appealGeneratorBentoProcessing: Boolean = false,
    val appealLetterEditingEnabled: Boolean = false,
    val paywallVisible: Boolean = false,
    val paywallMessage: String = "",
    val paywallPurchasePending: Boolean = false,
    val cameraScanDocumentType: CameraScanDocumentType = CameraScanDocumentType.Eob,
    val hubSettings: HubSettingsState = HubSettingsState()
)

/**
 * Single source of truth for authenticated hub state: EOB records, selection, appeals, news, uploads,
 * and derived hub snapshots (care team, bento, history, providers, yearly costs).
 *
 * UI layers observe [eobRecords], [sortedEobRecords], [insuranceBriefings], and [uiState]; all
 * analytics and card state flow through ViewModel methods. Firestore sync goes through [EobRepository].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EobViewModel : ViewModel() {
    private var repository: EobRepository? = null
    private val _repository = MutableStateFlow<EobRepository?>(null)
    private var settingsStore: HubSettingsStore? = null
    private var appContext: Context? = null
    private var profileListener: ListenerRegistration? = null
    private var lastBackgroundAt: Long = System.currentTimeMillis()
    private var hasBeenBackgrounded: Boolean = false

    private val _eobRecords = MutableStateFlow<List<EobRecord>>(emptyList())
    val eobRecords: StateFlow<List<EobRecord>> = _eobRecords.asStateFlow()

    private val _taxVaultFilterState = MutableStateFlow(TaxVaultFilterState.OFF)
    val taxVaultFilterState: StateFlow<TaxVaultFilterState> = _taxVaultFilterState.asStateFlow()

    private val _taxVaultVisibilityMode = MutableStateFlow(TaxVaultVisibilityMode.GATED)
    val taxVaultVisibilityMode: StateFlow<TaxVaultVisibilityMode> = _taxVaultVisibilityMode.asStateFlow()

    val sortedEobRecords: StateFlow<List<EobRecord>> = eobRecords
        .map { records -> records.sortedByDescending { it.serviceDateSortKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private val _documentScanState = MutableStateFlow<DocumentScanPipelineState>(DocumentScanPipelineState.Idle)
    val documentScanState: StateFlow<DocumentScanPipelineState> = _documentScanState.asStateFlow()

    private var uploadText: String = ""
    private var liveBeckersNewsPool: List<NewsRelease> = emptyList()
    private var liveHealthcareDiveNewsPool: List<NewsRelease> = emptyList()
    private var firebaseNews: List<NewsRelease> = emptyList()
    private var deletedNewsKeys: Set<String> = emptySet()
    private var newsRotationJob: Job? = null
    private val _syncProfile = MutableStateFlow(UserProfile())
    private var eobListener: ListenerRegistration? = null

    private val userContextTags: Flow<Set<String>> = combine(
        uiState.map { it.selectedCptCategory }.distinctUntilChanged(),
        _syncProfile
    ) { category, profile ->
        buildSet {
            profile.city.takeIf { it.isNotBlank() }?.let(::add)
            profile.state.takeIf { it.isNotBlank() }?.let(::add)
            add(category.name)
        }
    }

    private val regionalNewsFeed = combine(
        _repository,
        _syncProfile.map { it.state }.distinctUntilChanged()
    ) { repo, userState ->
        repo to userState
    }.flatMapLatest { (repo, userState) ->
        when {
            repo == null || userState.isBlank() -> flowOf(emptyList())
            else -> repo.observeRegionalNews(userState)
        }
    }

    val personalizedNewsFeed: StateFlow<List<NewsRelease>> = combine(
        regionalNewsFeed,
        userContextTags
    ) { releases, contextTags ->
        rankNewsReleases(releases, contextTags)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insuranceBriefings(): List<InsuranceArticle> {
        return EobInsuranceNews.articlesForYear()
    }

    fun insuranceNewsRotationSlot(): Long {
        return InsuranceNewsRotation.rotationSlot()
    }

    fun attachRepository(repo: EobRepository, context: Context) {
        repository = repo
        _repository.value = repo
        appContext = context.applicationContext
        settingsStore = HubSettingsStore(context)
        loadHubSettings()
        refreshCacheSize()
        refreshFirebaseStatus()
        fetchLiveInsuranceNews()
        startInsuranceNewsRotationClock()
    }

    private fun startInsuranceNewsRotationClock() {
        newsRotationJob?.cancel()
        newsRotationJob = viewModelScope.launch {
            while (isActive) {
                val delayMs = InsuranceNewsRotation.millisUntilNextRotation()
                if (delayMs > 0L) {
                    delay(delayMs)
                } else {
                    delay(1_000L)
                }
                bumpNewsFeedRevision()
            }
        }
    }

    fun fetchLiveInsuranceNews() {
        viewModelScope.launch(Dispatchers.IO) {
            val beckersNews = runCatching {
                RssNewsMapper.mapResponse(
                    company = RssNewsMapper.BECKERS_COMPANY,
                    response = RetrofitClient.api.getFeed(RssNewsMapper.BECKERS_RSS_URL)
                )
            }.getOrElse { emptyList() }

            val diveNews = runCatching {
                RssNewsMapper.mapResponse(
                    company = RssNewsMapper.HEALTHCARE_DIVE_COMPANY,
                    response = RetrofitClient.api.getFeed(RssNewsMapper.HEALTHCARE_DIVE_RSS_URL)
                )
            }.getOrElse { emptyList() }

            if (beckersNews.isNotEmpty()) {
                liveBeckersNewsPool = beckersNews
            }
            if (diveNews.isNotEmpty()) {
                liveHealthcareDiveNewsPool = diveNews
            }
            if (!hasLiveInsuranceNewsPools()) return@launch

            withContext(Dispatchers.Main) {
                firebaseNews = rotatedLiveInsuranceIntelligence()
                bumpNewsFeedRevision()
            }
        }
    }

    private fun hasLiveInsuranceNewsPools(): Boolean {
        return liveBeckersNewsPool.isNotEmpty() || liveHealthcareDiveNewsPool.isNotEmpty()
    }

    private fun rotatedLiveInsuranceIntelligence(): List<NewsRelease> {
        return InsuranceNewsRotation.combineRotatedIntelligence(
            beckersPool = liveBeckersNewsPool,
            healthcareDivePool = liveHealthcareDiveNewsPool,
            slot = insuranceNewsRotationSlot()
        )
    }

    private fun loadHubSettings() {
        val stored = settingsStore?.read() ?: return
        _uiState.update { state ->
            state.copy(
                hubSettings = stored.copy(
                    cacheSizeBytes = state.hubSettings.cacheSizeBytes,
                    subscriptionTier = state.hubSettings.subscriptionTier,
                    settingsAccountEditing = false,
                    settingsNotice = "",
                    appLocked = false,
                    selectedTab = state.hubSettings.selectedTab
                )
            )
        }
        if (repository?.status()?.isConfigured == true) {
            applyCrashlyticsCollection(stored.crashlyticsOptIn)
        }
    }

    private fun persistHubSettings(settings: HubSettingsState) {
        settingsStore?.write(settings)
    }

    private fun updateHubSettings(transform: (HubSettingsState) -> HubSettingsState) {
        _uiState.update { state ->
            val updated = transform(state.hubSettings)
            persistHubSettings(updated)
            state.copy(hubSettings = updated)
        }
    }

    private fun applyCrashlyticsCollection(enabled: Boolean) {
        HubCrashlyticsGate.setCollectionEnabled(enabled)
    }

    fun setSettingsTab(tab: SettingsTab) {
        _uiState.update { it.copy(hubSettings = it.hubSettings.copy(selectedTab = tab)) }
    }

    fun setPinLockEnabled(enabled: Boolean) {
        val store = settingsStore
        if (enabled && store != null && !store.hasAppPin()) return
        updateHubSettings { it.copy(pinLockEnabled = enabled) }
        if (!enabled) {
            _uiState.update { state -> state.copy(hubSettings = state.hubSettings.copy(appLocked = false)) }
        }
    }

    fun saveAppPin(pin: String, confirmPin: String, language: AppLanguage): String {
        if (!FIVE_DIGIT_PIN.matches(pin)) {
            return EobStrings.t(language, "settingsPinInvalid")
        }
        if (pin != confirmPin) {
            return EobStrings.t(language, "settingsPinMismatch")
        }
        settingsStore?.saveAppPin(pin)
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(pinConfigured = isAppPinConfigured()))
        }
        return EobStrings.t(language, "settingsPinSaved")
    }

    fun verifyAppPinAndUnlock(pin: String): Boolean {
        if (settingsStore?.verifyAppPin(pin) != true) return false
        unlockApp()
        return true
    }

    fun isAppPinConfigured(): Boolean {
        return settingsStore?.hasAppPin() == true
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        updateHubSettings { it.copy(appLockTimeout = timeout) }
    }

    fun setCrashlyticsOptIn(enabled: Boolean) {
        updateHubSettings { it.copy(crashlyticsOptIn = enabled) }
        if (repository?.status()?.isConfigured == true) {
            applyCrashlyticsCollection(enabled)
        }
    }

    fun setUploadOverWifiOnly(enabled: Boolean) {
        updateHubSettings { it.copy(uploadOverWifiOnly = enabled) }
    }

    fun setImageCompressionLevel(level: ImageCompressionLevel) {
        updateHubSettings { it.copy(imageCompressionLevel = level) }
    }

    fun setAutoCropEnabled(enabled: Boolean) {
        updateHubSettings { it.copy(autoCropEnabled = enabled) }
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        updateHubSettings { it.copy(darkModeEnabled = enabled) }
    }

    fun imageCompressionLevel(): ImageCompressionLevel {
        return _uiState.value.hubSettings.imageCompressionLevel
    }

    fun autoCropEnabled(): Boolean {
        return _uiState.value.hubSettings.autoCropEnabled
    }

    fun canUploadOnCurrentNetwork(context: Context): Boolean {
        return NetworkUploadGate.canUpload(context, _uiState.value.hubSettings.uploadOverWifiOnly)
    }

    fun refreshCacheSize() {
        val cacheDir = appContext?.cacheDir ?: return
        val bytes = CacheSizeCalculator.directorySizeBytes(cacheDir)
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(cacheSizeBytes = bytes))
        }
    }

    fun clearLocalCache(onComplete: (Boolean) -> Unit) {
        val cacheDir = appContext?.cacheDir
        if (cacheDir == null) {
            onComplete(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val cleared = runCatching {
                cacheDir.listFiles()?.forEach { entry ->
                    entry.deleteRecursively()
                }
            }.isSuccess
            refreshCacheSize()
            withContext(Dispatchers.Main) {
                onComplete(cleared)
            }
        }
    }

    fun setSubscriptionTier(tier: SubscriptionTier) {
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(subscriptionTier = tier))
        }
    }

    /** Applies merged subscription status from [SubscriptionViewModel] into hub settings. */
    fun applySubscriptionState(state: SubscriptionState) {
        when (state) {
            SubscriptionState.Gold -> {
                setSubscriptionTier(SubscriptionTier.Gold)
                dismissPaywall()
            }
            SubscriptionState.Silver -> {
                setSubscriptionTier(SubscriptionTier.Silver)
                dismissPaywall()
            }
            SubscriptionState.Free -> setSubscriptionTier(SubscriptionTier.Free)
            SubscriptionState.Loading, is SubscriptionState.Error -> Unit
        }
    }

    fun showPaywall(message: String = "") {
        _uiState.update { it.copy(paywallVisible = true, paywallMessage = message) }
    }

    fun dismissPaywall() {
        _uiState.update {
            it.copy(paywallVisible = false, paywallMessage = "", paywallPurchasePending = false)
        }
    }

    /** Hides paywall so Google Play billing can present; errors re-open paywall via [handleBillingNoticeForPaywall]. */
    fun beginPaywallPurchase() {
        _uiState.update {
            it.copy(paywallPurchasePending = true, paywallVisible = false, paywallMessage = "")
        }
    }

    fun billingNoticeForPaywall(language: AppLanguage): String {
        val notice = _uiState.value.hubSettings.settingsNotice
        return notice.takeIf { it in localizedBillingNotices(language) }.orEmpty()
    }

    private fun localizedBillingNotices(language: AppLanguage): Set<String> = setOf(
        EobStrings.t(language, "billingNotReady"),
        EobStrings.t(language, "billingProductUnavailable"),
        EobStrings.t(language, "billingFlowFailed"),
        EobStrings.t(language, "billingPaymentDeclined"),
        EobStrings.t(language, "billingPaymentPending"),
        EobStrings.t(language, "billingRestoreNone"),
        EobStrings.t(language, "billingRestoreFailed"),
        EobStrings.t(language, "billingRestoreSuccess")
    )

    fun isTaxVaultGoldUnlocked(): Boolean {
        return _uiState.value.hubSettings.subscriptionTier.isGold()
    }

    fun isTaxVaultActive(): Boolean {
        return _taxVaultFilterState.value != TaxVaultFilterState.OFF
    }

    fun isTaxVaultHistoryGated(): Boolean {
        return isTaxVaultActive() && _taxVaultVisibilityMode.value == TaxVaultVisibilityMode.GATED
    }

    fun enableSettingsAccountEditing() {
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(settingsAccountEditing = true, settingsNotice = ""))
        }
    }

    fun disableSettingsAccountEditing() {
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(settingsAccountEditing = false))
        }
    }

    fun updateSettingsNotice(message: String) {
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(settingsNotice = message))
        }
    }

    fun updateBillingNotice(language: AppLanguage, noticeKey: String) {
        updateSettingsNotice(localizedBillingNotice(language, noticeKey))
    }

    fun handleBillingNoticeForPaywall(language: AppLanguage, noticeKey: String) {
        val message = localizedBillingNotice(language, noticeKey)
        _uiState.update { state ->
            when {
                state.paywallPurchasePending -> state.copy(
                    paywallPurchasePending = false,
                    paywallVisible = true,
                    paywallMessage = message,
                    hubSettings = state.hubSettings.copy(settingsNotice = message)
                )

                state.paywallVisible -> state.copy(
                    paywallMessage = message,
                    hubSettings = state.hubSettings.copy(settingsNotice = message)
                )

                else -> state
            }
        }
    }

    private fun localizedBillingNotice(language: AppLanguage, noticeKey: String): String {
        return when (noticeKey) {
            "billing_not_ready" -> EobStrings.t(language, "billingNotReady")
            "billing_product_unavailable" -> EobStrings.t(language, "billingProductUnavailable")
            "billing_payment_declined" -> EobStrings.t(language, "billingPaymentDeclined")
            "billing_payment_pending" -> EobStrings.t(language, "billingPaymentPending")
            "billing_restore_none" -> EobStrings.t(language, "billingRestoreNone")
            "billing_restore_failed" -> EobStrings.t(language, "billingRestoreFailed")
            "billing_restore_success" -> EobStrings.t(language, "billingRestoreSuccess")
            "billing_user_canceled" -> ""
            else -> EobStrings.t(language, "billingFlowFailed")
        }
    }

    fun canUploadOnCurrentNetwork(): Boolean {
        val context = appContext ?: return true
        return canUploadOnCurrentNetwork(context)
    }

    fun onAppBackgrounded() {
        lastBackgroundAt = System.currentTimeMillis()
        hasBeenBackgrounded = true
    }

    fun onAppForegrounded() {
        if (!hasBeenBackgrounded) return
        val settings = _uiState.value.hubSettings
        if (!settings.pinLockEnabled) return
        val elapsed = System.currentTimeMillis() - lastBackgroundAt
        if (elapsed >= settings.appLockTimeout.millis) {
            _uiState.update { state ->
                state.copy(hubSettings = state.hubSettings.copy(appLocked = true))
            }
        }
    }

    fun unlockApp() {
        lastBackgroundAt = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(hubSettings = state.hubSettings.copy(appLocked = false))
        }
    }

    fun deleteAccount(
        userId: String,
        language: AppLanguage,
        onComplete: (String) -> Unit
    ) {
        val repo = repository
        if (userId.isBlank() || repo == null) {
            onComplete(EobStrings.t(language, "settingsDeleteAccountSignIn"))
            return
        }
        repo.deleteAccount(userId) { message ->
            onComplete(EobStrings.localizeRepositoryMessage(language, message))
        }
    }

    fun refreshFirebaseStatus() {
        val status = repository?.status() ?: FirebaseSyncStatus(isConfigured = false)
        _uiState.update { it.copy(firebaseSyncStatus = status) }
        if (status.isConfigured) {
            applyCrashlyticsCollection(_uiState.value.hubSettings.crashlyticsOptIn)
        }
    }

    fun updateSyncProfile(profile: UserProfile) {
        _syncProfile.value = profile.sanitizedPlanLimits()
        val selected = _uiState.value.selectedRecord
        if (selected != null) {
            _uiState.update {
                it.copy(appealLetter = AppealLetterGenerator.generate(_syncProfile.value, selected))
            }
        }
    }

    fun hubTimeKey(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH)
    }

    fun resetHubState() {
        eobListener?.remove()
        eobListener = null
        profileListener?.remove()
        profileListener = null
        _eobRecords.value = emptyList()
        val preservedSettings = _uiState.value.hubSettings.copy(
            settingsAccountEditing = false,
            settingsNotice = "",
            appLocked = false,
            subscriptionTier = SubscriptionTier.Free
        )
        _uiState.value = HubUiState(hubSettings = preservedSettings)
        _syncProfile.value = UserProfile()
        _taxVaultFilterState.value = TaxVaultFilterState.OFF
        _taxVaultVisibilityMode.value = TaxVaultVisibilityMode.GATED
        uploadText = ""
        liveBeckersNewsPool = emptyList()
        liveHealthcareDiveNewsPool = emptyList()
        firebaseNews = emptyList()
        deletedNewsKeys = emptySet()
        newsRotationJob?.cancel()
        newsRotationJob = null
        _documentScanState.value = DocumentScanPipelineState.Idle
    }

    fun startFirestoreSync(userId: String, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
        val repo = repository ?: return
        updateSyncProfile(profile)
        refreshFirebaseStatus()
        fetchHistoryFromFirestore(repo, userId)
        observeProfile(repo, userId, onProfileChanged)
    }

    private fun observeProfile(repo: EobRepository, userId: String, onProfileChanged: (UserProfile) -> Unit) {
        profileListener?.remove()
        profileListener = repo.observeProfile(
            userId = userId,
            onProfile = { remoteProfile ->
                updateSyncProfile(remoteProfile)
                onProfileChanged(remoteProfile)
            },
            onError = { message -> updateUploadNotice(message) }
        )
    }

    fun fetchHistoryFromFirestore(repo: EobRepository, userId: String) {
        if (userId.isBlank()) {
            resetHubState()
            return
        }

        eobListener?.remove()
        eobListener = repo.observeEobs(
            userId = userId,
            onRecords = { records -> applyRemoteRecords(records) },
            onError = { message ->
                _uiState.update { state ->
                    state.copy(
                        uploadNotice = message,
                        isLoadingInvoice = false,
                        invoiceProcessingPhase = when {
                            state.invoiceProcessingPhase == InvoiceProcessingPhase.Processing ->
                                InvoiceProcessingPhase.Idle
                            else -> state.invoiceProcessingPhase
                        }
                    )
                }
            }
        )
    }

    private fun applyRemoteRecords(records: List<EobRecord>) {
        val profile = _syncProfile.value
        viewModelScope.launch(Dispatchers.Default) {
            val compacted = EobAnalyzer.compactDuplicateEobs(records)
                .sortedByDescending { it.serviceDateSortKey }
                .take(HistoryPagination.MAX_EOBS)

            withContext(Dispatchers.Main) {
                _eobRecords.value = compacted
                val currentSelection = _uiState.value.selectedRecord
                val wasProcessing = _uiState.value.isLoadingInvoice
                val nextSelection = if (currentSelection == null || compacted.none { it.id == currentSelection.id }) {
                    compacted.firstOrNull()
                } else {
                    compacted.firstOrNull { it.id == currentSelection.id }
                }
                _uiState.update {
                    it.copy(
                        selectedRecord = nextSelection,
                        appealLetter = AppealLetterGenerator.generate(profile, nextSelection),
                        isLoadingInvoice = false,
                        invoiceProcessingPhase = if (wasProcessing) {
                            InvoiceProcessingPhase.FileDropReveal
                        } else {
                            it.invoiceProcessingPhase
                        }
                    )
                }
            }
        }
    }

    fun replaceRecords(newRecords: List<EobRecord>, profile: UserProfile) {
        updateSyncProfile(profile)
        applyRemoteRecords(newRecords)
    }

    fun selectRecord(record: EobRecord, profile: UserProfile) {
        _uiState.update {
            it.copy(
                selectedRecord = record,
                uploadNotice = "",
                appealLetter = AppealLetterGenerator.generate(profile, record),
                appealLetterEditingEnabled = false
            )
        }
    }

    fun deleteRecord(record: EobRecord, profile: UserProfile) {
        val remaining = _eobRecords.value.filter { it.id != record.id }
        _eobRecords.value = remaining
        val nextSelection = remaining.firstOrNull()
        _uiState.update {
            it.copy(
                selectedRecord = nextSelection,
                appealLetter = AppealLetterGenerator.generate(profile, nextSelection),
                appealLetterEditingEnabled = false
            )
        }
    }

    fun deleteRecordRemote(
        userId: String,
        record: EobRecord,
        profile: UserProfile,
        language: AppLanguage,
        onComplete: (String) -> Unit = {}
    ) {
        deleteRecord(record, profile)
        val repo = repository
        if (userId.isNotBlank() && repo != null) {
            repo.deleteEob(userId, record) { message ->
                onComplete(EobStrings.localizeRepositoryMessage(language, message))
            }
        } else if (userId.isBlank()) {
            onComplete(EobStrings.t(language, "signInToDeleteEob"))
        }
    }

    fun deleteNews(news: NewsRelease) {
        deletedNewsKeys = deletedNewsKeys + news.key()
        liveBeckersNewsPool = liveBeckersNewsPool.filterNot { it.key() == news.key() }
        liveHealthcareDiveNewsPool = liveHealthcareDiveNewsPool.filterNot { it.key() == news.key() }
        firebaseNews = rotatedLiveInsuranceIntelligence()
        bumpNewsFeedRevision()
    }

    private fun bumpNewsFeedRevision() {
        _uiState.update { state -> state.copy(newsFeedRevision = state.newsFeedRevision + 1) }
    }

    fun visibleNews(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        return firebaseNews.ifEmpty { fallbackNews }.filterNot { it.key() in deletedNewsKeys }
    }

    fun updatePreferredDoctor(doctor: PreferredDoctor) {
        _uiState.update { state ->
            state.copy(preferredDoctors = state.preferredDoctors + (doctor.type to doctor))
        }
    }

    fun addAppointment(
        date: String,
        provider: String,
        time: String,
        notes: String,
        providerType: CareTeamProviderType
    ) {
        _uiState.update { state ->
            val nextId = (state.appointments.maxOfOrNull { it.id } ?: 0) + 1
            state.copy(
                appointments = state.appointments + DoctorAppointment(
                    id = nextId,
                    date = date,
                    providerName = provider,
                    time = time,
                    notes = notes,
                    providerType = providerType
                )
            )
        }
    }

    fun removeAppointment(appointment: DoctorAppointment) {
        _uiState.update { state ->
            state.copy(appointments = state.appointments.filterNot { it.id == appointment.id })
        }
    }

    fun updateAppointment(
        appointmentId: Int,
        date: String,
        provider: String,
        time: String,
        notes: String,
        providerType: CareTeamProviderType
    ) {
        _uiState.update { state ->
            state.copy(
                appointments = state.appointments.map { existing ->
                    if (existing.id == appointmentId) {
                        DoctorAppointment(
                            appointmentId,
                            date,
                            provider,
                            time,
                            notes,
                            providerType
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    fun setCalendarExpanded(expanded: Boolean) {
        _uiState.update { it.copy(calendarExpanded = expanded) }
    }

    fun openInsuranceArticle(article: InsuranceArticle) {
        _uiState.update { it.copy(selectedInsuranceArticle = article) }
    }

    fun dismissInsuranceArticle() {
        _uiState.update { it.copy(selectedInsuranceArticle = null) }
    }

    fun setSelectedNewsCarrier(carrier: MajorInsuranceCarrier) {
        _uiState.update { it.copy(selectedNewsCarrier = carrier) }
    }

    fun insuranceCarrierHubItems(): List<InsuranceNewsCarrierHubItem> {
        val articles = insuranceBriefings()
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        return MajorInsuranceCarrier.entries.map { carrier ->
            val carrierArticles = articles.filter { it.carrier == carrier }
            val featured = carrierArticles.firstOrNull { it.monthIndex == currentMonth }
                ?: carrierArticles.minByOrNull { it.monthIndex }
            InsuranceNewsCarrierHubItem(
                carrier = carrier,
                monthlyBriefingCount = carrierArticles.size,
                featuredArticle = featured
            )
        }
    }

    fun filteredNewsReleases(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        val releases = currentNewsReleases(fallbackNews)
        if (hasLiveInsuranceNewsPools()) {
            return releases
        }
        val carrier = _uiState.value.selectedNewsCarrier
        val keywords = carrier.filterKeywords()
        val filtered = releases.filter { release ->
            keywords.any { keyword ->
                release.company.contains(keyword, ignoreCase = true) ||
                    release.headline.contains(keyword, ignoreCase = true) ||
                    release.summary.contains(keyword, ignoreCase = true) ||
                    release.targetTags.any { tag -> tag.contains(keyword, ignoreCase = true) }
            }
        }
        return filtered.ifEmpty { releases }
    }

    fun updateAppeal(text: String) {
        if (!_uiState.value.appealLetterEditingEnabled) return
        _uiState.update { it.copy(appealLetter = text) }
    }

    fun enableAppealLetterEditing() {
        _uiState.update { it.copy(appealLetterEditingEnabled = true) }
    }

    fun saveAppealLetter() {
        _uiState.update { it.copy(appealLetterEditingEnabled = false) }
    }

    fun updateUploadNotice(message: String) {
        _uiState.update { it.copy(uploadNotice = message) }
    }

    fun setLoadingInvoice(loading: Boolean) {
        _uiState.update { state ->
            state.copy(
                isLoadingInvoice = loading,
                invoiceProcessingPhase = when {
                    loading -> InvoiceProcessingPhase.Processing
                    state.invoiceProcessingPhase == InvoiceProcessingPhase.Processing -> InvoiceProcessingPhase.Idle
                    else -> state.invoiceProcessingPhase
                }
            )
        }
    }

    fun acknowledgeInvoiceFileDropAnimation() {
        _uiState.update {
            it.copy(invoiceProcessingPhase = InvoiceProcessingPhase.Idle)
        }
    }

    fun setHistoryBentoFilter(filter: HistoryBentoFilter) {
        _uiState.update { it.copy(historyBentoFilter = filter) }
    }

    fun setHistoryPaymentFilter(filter: EobHistoryPaymentFilter) {
        _uiState.update { it.copy(historyPaymentFilter = filter) }
    }

    fun openProviderRecordHistory(providerName: String) {
        _uiState.update {
            it.copy(
                historyProviderSearch = providerName.trim(),
                historyPage = 0
            )
        }
    }

    fun clearHistoryProviderSearch() {
        _uiState.update { it.copy(historyProviderSearch = "") }
    }

    fun setTaxVaultFilterState(state: TaxVaultFilterState) {
        if (state != TaxVaultFilterState.OFF && !isTaxVaultGoldUnlocked()) return
        _taxVaultFilterState.value = state
    }

    fun setTaxVaultVisibilityMode(mode: TaxVaultVisibilityMode) {
        if (!isTaxVaultGoldUnlocked()) return
        _taxVaultVisibilityMode.value = mode
    }

    fun taxVaultBudgetSummary(profile: UserProfile): TaxVaultBudgetSummary {
        val filter = _taxVaultFilterState.value
        if (filter == TaxVaultFilterState.OFF) {
            return TaxVaultBudgetSummary(eligibleAmount = 0.0, allocationLimit = 0.0, savedAmount = 0.0)
        }
        val safeProfile = profile.sanitizedPlanLimits()
        val eligibleRecords = EobAnalyzer.recordsForTaxVaultFilter(_eobRecords.value, filter)
        val eligibleAmount = eligibleRecords.sumOf { it.totalPatientResponsibility }
        val allocationLimit = when (filter) {
            TaxVaultFilterState.HSA -> safeProfile.hsaAllocation
            TaxVaultFilterState.FSA -> safeProfile.fsaAllocation
            TaxVaultFilterState.OFF -> 0.0
        }
        val savedAmount = (allocationLimit - eligibleAmount).coerceAtLeast(0.0)
        return TaxVaultBudgetSummary(
            eligibleAmount = eligibleAmount,
            allocationLimit = allocationLimit,
            savedAmount = savedAmount
        )
    }

    private fun recordsForHistoryPipeline(): List<EobRecord> {
        val records = _eobRecords.value
        val filter = _taxVaultFilterState.value
        if (filter == TaxVaultFilterState.OFF) return records
        if (_taxVaultVisibilityMode.value != TaxVaultVisibilityMode.GATED) return records
        return EobAnalyzer.recordsForTaxVaultFilter(records, filter)
    }

    fun historyBentoSnapshot(): HistoryBentoSnapshot {
        return EobAnalyzer.historyBentoSnapshot(recordsForHistoryPipeline())
    }

    fun providerAvatarPreviews(language: AppLanguage): List<ProviderAvatarPreview> {
        return EobAnalyzer.providerAvatarPreviews(_eobRecords.value, language)
    }

    fun providerDirectory(): List<ProviderSummary> {
        return EobAnalyzer.providerDirectory(_eobRecords.value)
    }

    fun yearlyHealthCostSummary(preferredYear: Int? = null): YearlyHealthCostSummary {
        return EobAnalyzer.yearlyHealthCostSummary(_eobRecords.value, preferredYear)
    }

    fun ytdExpenseData(profile: UserProfile, preferredYear: Int? = null): YtdExpenseData {
        return EobAnalyzer.ytdExpenseData(_eobRecords.value, profile, preferredYear)
    }

    fun historyRecordsForDisplay(
        filter: HistoryBentoFilter,
        searchQuery: String
    ): List<EobRecord> {
        val sorted = recordsForHistoryPipeline().sortedByDescending { it.serviceDateSortKey }
        val byFilter = when (filter) {
            HistoryBentoFilter.All -> sorted
            HistoryBentoFilter.Flagged -> EobAnalyzer.recordsWithFlaggedBillingErrors(sorted)
        }
        if (searchQuery.isBlank()) return byFilter
        return byFilter.filter { record ->
            record.providerName.contains(searchQuery, ignoreCase = true) ||
                record.insuranceCompany.contains(searchQuery, ignoreCase = true)
        }
    }

    fun historyTimelineSections(
        bentoFilter: HistoryBentoFilter,
        searchQuery: String,
        paymentFilter: EobHistoryPaymentFilter,
        language: AppLanguage
    ): List<Pair<String, List<HistoryTimelineRow>>> {
        val filtered = historyRecordsForDisplay(bentoFilter, searchQuery)
        val paymentFiltered = EobAnalyzer.filterHistoryByPayment(filtered, paymentFilter)
        return EobAnalyzer.groupHistoryByMonth(paymentFiltered, language)
    }

    fun totalBillingErrors(records: List<EobRecord>): Int {
        return records.sumOf { record ->
            EobAnalyzer.detectBillingIssues(record).count { it.severity != BillingIssueSeverity.Info }
        }
    }

    fun currentNewsReleases(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        if (hasLiveInsuranceNewsPools()) {
            val rotatedLiveNews = rotatedLiveInsuranceIntelligence()
                .filterNot { it.key() in deletedNewsKeys }
            if (rotatedLiveNews.isNotEmpty()) {
                return rotatedLiveNews
            }
        }
        val ranked = personalizedNewsFeed.value.filterNot { it.key() in deletedNewsKeys }
        val source = ranked.ifEmpty { visibleNews(fallbackNews) }
        return EobKnowledgeBase.currentNewsReleases(source)
    }

    private fun isInvoicePipelineActive(): Boolean {
        val state = _uiState.value
        return state.isLoadingInvoice ||
            state.invoiceProcessingPhase == InvoiceProcessingPhase.Processing
    }

    fun insuranceCardDisplay(profile: UserProfile, language: AppLanguage): InsuranceCardDisplay {
        val safeProfile = profile.sanitizedPlanLimits()
        return InsuranceCardDisplay(
            insuranceName = safeProfile.insuranceCompany.ifBlank {
                EobStrings.t(language, "cleanInsuranceNameFallback")
            },
            memberId = safeProfile.memberId.ifBlank {
                EobStrings.t(language, "cleanInsuranceMemberIdFallback")
            },
            groupNumber = safeProfile.groupNumber.ifBlank {
                EobStrings.t(language, "cleanInsuranceGroupFallback")
            },
            pcpCopay = safeProfile.pcpCopay.ifBlank {
                EobStrings.t(language, "cleanInsuranceCopayFallback")
            },
            specialistCopay = safeProfile.specialistCopay.ifBlank {
                EobStrings.t(language, "cleanInsuranceCopayFallback")
            },
            footerLocation = safeProfile.locationLine().ifBlank {
                EobStrings.t(language, "valueNotSet")
            },
            verificationCode = safeProfile.verificationFingerprint()
        )
    }

    fun applyInsuranceCardEdits(
        profile: UserProfile,
        insuranceName: String,
        memberId: String,
        groupNumber: String,
        pcpCopay: String,
        specialistCopay: String
    ): UserProfile {
        return profile.copy(
            insuranceName = insuranceName.trim(),
            insuranceId = memberId.trim(),
            groupName = groupNumber.trim(),
            pcpCopay = pcpCopay.trim(),
            specialistCopay = specialistCopay.trim()
        ).sanitizedPlanLimits()
    }

    fun careTeamCardStates(language: AppLanguage): List<CareTeamCardDisplayState> {
        val state = _uiState.value
        return CareTeamStateExtractor.buildCareTeamCards(
            language = language,
            preferredDoctors = state.preferredDoctors,
            appointments = state.appointments,
            records = _eobRecords.value,
            invoiceProcessing = isInvoicePipelineActive()
        )
    }

    fun providerDirectoryAssurance(language: AppLanguage): ProviderDirectoryAssurance {
        return CareTeamStateExtractor.buildProviderDirectoryAssurance(
            language = language,
            preferredDoctors = _uiState.value.preferredDoctors,
            records = _eobRecords.value,
            invoiceProcessing = isInvoicePipelineActive()
        )
    }

    fun cptBentoSnapshot(language: AppLanguage): CptBentoSnapshot {
        return BentoSnapshotExtractor.buildCptBentoSnapshot(
            language = language,
            records = _eobRecords.value,
            selectedCategory = _uiState.value.selectedCptCategory
        )
    }

    fun ytdDeductibleBentoSnapshot(profile: UserProfile): YtdDeductibleBentoSnapshot {
        return BentoSnapshotExtractor.buildYtdDeductibleBentoSnapshot(
            records = _eobRecords.value,
            profile = profile.sanitizedPlanLimits()
        )
    }

    fun insuranceNewsBentoSnapshot(language: AppLanguage): InsuranceNewsBentoSnapshot {
        return BentoSnapshotExtractor.buildInsuranceNewsBentoSnapshot(
            language = language,
            releases = currentNewsReleases(EobKnowledgeBase.newsReleases),
            records = _eobRecords.value
        )
    }

    fun setSelectedCptCategory(category: CptCategory) {
        _uiState.update { it.copy(selectedCptCategory = category) }
    }

    fun setCameraScanDocumentType(type: CameraScanDocumentType) {
        _uiState.update { it.copy(cameraScanDocumentType = type) }
    }

    fun cameraScanSourceLabel(language: AppLanguage): String {
        return when (_uiState.value.cameraScanDocumentType) {
            CameraScanDocumentType.Eob -> EobStrings.t(language, "cameraScanTypeEob")
            CameraScanDocumentType.Receipt -> EobStrings.t(language, "cameraScanTypeReceipt")
        }
    }

    fun cptFlashcardEntries(
        records: List<EobRecord>,
        category: CptCategory,
        language: AppLanguage
    ): List<CptCodeEntry> {
        return BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = language,
            records = records,
            category = category
        )
    }

    fun setYtdBentoViewMode(mode: YtdBentoViewMode) {
        _uiState.update { it.copy(ytdBentoViewMode = mode) }
    }

    fun updateUploadText(text: String) {
        uploadText = text
    }

    fun setHistoryPage(page: Int) {
        _uiState.update { it.copy(historyPage = page.coerceIn(0, HistoryPagination.MAX_PAGE_INDEX)) }
    }

    fun regenerateAppeal(profile: UserProfile) {
        val selected = _uiState.value.selectedRecord
        _uiState.update {
            it.copy(
                appealLetter = AppealLetterGenerator.generate(profile, selected),
                appealLetterEditingEnabled = false
            )
        }
    }

    fun activateAppealGeneratorBento(profile: UserProfile) {
        regenerateAppeal(profile)
        _uiState.update { it.copy(appealGeneratorBentoProcessing = true) }
    }

    fun acknowledgeAppealGeneratorBentoActivation() {
        _uiState.update { it.copy(appealGeneratorBentoProcessing = false) }
    }

    fun uploadEobFile(userId: String, uri: Uri, sourceName: String, language: AppLanguage) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            setLoadingInvoice(false)
            updateUploadNotice(EobStrings.t(language, "signInBeforeUpload"))
            return
        }
        val context = appContext
        if (context != null && !canUploadOnCurrentNetwork(context)) {
            setLoadingInvoice(false)
            updateUploadNotice(EobStrings.t(language, "settingsUploadWifiBlocked"))
            return
        }
        setLoadingInvoice(true)
        repo.uploadEobFile(userId, uri, sourceName) { message ->
            val notice = EobStrings.localizeRepositoryMessage(language, message)
                .ifBlank { EobStrings.t(language, "libraryUploadStarted") }
            updateUploadNotice(notice)
            if (message.contains("failed", ignoreCase = true)) {
                setLoadingInvoice(false)
            }
        }
    }

    fun onDocumentScanStarted() {
        _documentScanState.value = DocumentScanPipelineState.LocalScanning
    }

    fun onDocumentScanCancelled() {
        _documentScanState.value = DocumentScanPipelineState.Idle
    }

    fun onDocumentScanLaunchFailed(language: AppLanguage, message: String) {
        _documentScanState.value = DocumentScanPipelineState.Error(
            message.ifBlank { EobStrings.t(language, "documentScanLaunchFailed") }
        )
    }

    fun dismissDocumentScanState() {
        _documentScanState.value = DocumentScanPipelineState.Idle
    }

    fun processScannedDocument(
        userId: String,
        uri: Uri,
        sourceName: String,
        language: AppLanguage
    ) {
        val repo = repository ?: return
        val context = appContext ?: return
        if (userId.isBlank()) {
            _documentScanState.value = DocumentScanPipelineState.Error(
                EobStrings.t(language, "signInBeforeUpload")
            )
            return
        }
        if (!canUploadOnCurrentNetwork(context)) {
            _documentScanState.value = DocumentScanPipelineState.Error(
                EobStrings.t(language, "settingsUploadWifiBlocked")
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                setLoadingInvoice(true)
                _documentScanState.value = DocumentScanPipelineState.OcrPreCheck
            }

            val preCheck = runCatching {
                repo.runDocumentOcrPreCheck(
                    context = context,
                    uri = uri,
                    scanType = _uiState.value.cameraScanDocumentType
                )
            }.getOrElse { error ->
                withContext(Dispatchers.Main) {
                    setLoadingInvoice(false)
                    _documentScanState.value = DocumentScanPipelineState.Error(
                        EobStrings.t(language, "documentScanOcrFailed")
                    )
                    updateUploadNotice(error.localizedMessage.orEmpty())
                }
                return@launch
            }
            if (!preCheck.passed) {
                withContext(Dispatchers.Main) {
                    setLoadingInvoice(false)
                    _documentScanState.value = DocumentScanPipelineState.Error(
                        EobStrings.t(language, "documentScanOcrPreCheckFailed")
                    )
                    updateUploadNotice(EobStrings.t(language, "documentScanOcrPreCheckFailed"))
                }
                return@launch
            }

            val preparedUri = runCatching {
                OcrProcessor.prepareUriForUpload(context, uri, imageCompressionLevel())
            }.getOrElse { error ->
                withContext(Dispatchers.Main) {
                    setLoadingInvoice(false)
                    _documentScanState.value = DocumentScanPipelineState.Error(
                        EobStrings.t(language, "imagePrepFailed")
                    )
                    updateUploadNotice(error.localizedMessage.orEmpty())
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _documentScanState.value = DocumentScanPipelineState.UploadingAndProcessing
            }

            val extraction = runCatching {
                repo.processHybridScannedDocument(
                    context = context,
                    userId = userId,
                    uri = preparedUri,
                    sourceName = sourceName
                )
            }
            withContext(Dispatchers.Main) {
                extraction.fold(
                    onSuccess = { record ->
                        _documentScanState.value = DocumentScanPipelineState.Success(record)
                        updateUploadNotice(EobStrings.t(language, "documentScanSuccess"))
                        setLoadingInvoice(false)
                    },
                    onFailure = { error ->
                        setLoadingInvoice(false)
                        val message = when (error) {
                            is kotlinx.coroutines.TimeoutCancellationException ->
                                EobStrings.t(language, "documentScanVeryfiTimeout")
                            else -> error.localizedMessage
                                ?.takeIf { it.isNotBlank() }
                                ?: EobStrings.t(language, "documentScanFailed")
                        }
                        _documentScanState.value = DocumentScanPipelineState.Error(message)
                        updateUploadNotice(message)
                    }
                )
            }
        }
    }

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, language: AppLanguage) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            setLoadingInvoice(false)
            updateUploadNotice(EobStrings.t(language, "signInBeforeScan"))
            return
        }
        val context = appContext
        if (context != null && !canUploadOnCurrentNetwork(context)) {
            setLoadingInvoice(false)
            updateUploadNotice(EobStrings.t(language, "settingsUploadWifiBlocked"))
            return
        }
        setLoadingInvoice(true)
        repo.uploadEobBitmap(userId, bitmap, sourceName) { message ->
            val notice = EobStrings.localizeRepositoryMessage(language, message)
                .ifBlank { EobStrings.t(language, "cameraScanStarted") }
            updateUploadNotice(notice)
            if (message.contains("failed", ignoreCase = true)) {
                setLoadingInvoice(false)
            }
        }
    }

    fun savePastedEob(userId: String, sourceName: String, profile: UserProfile, language: AppLanguage) {
        val repo = repository ?: return
        if (uploadText.isBlank()) return
        val currentRecords = _eobRecords.value
        val analyzedRecord = EobAnalyzer.analyze(uploadText, sourceName, (currentRecords.maxOfOrNull { it.id } ?: 0) + 1)
        val duplicateIndex = currentRecords.indexOfFirst { EobAnalyzer.isSameEob(it, analyzedRecord) }
        val notice = if (duplicateIndex >= 0) {
            EobStrings.t(language, "duplicateReplaced")
        } else {
            EobStrings.t(language, "eobAdded")
        }
        val record = if (duplicateIndex >= 0) {
            analyzedRecord.copy(id = currentRecords[duplicateIndex].id)
        } else {
            analyzedRecord
        }
        val updatedRecords = currentRecords.toMutableList().apply {
            if (duplicateIndex >= 0) this[duplicateIndex] = record else add(record)
        }
        _uiState.update { it.copy(uploadNotice = notice) }
        replaceRecords(updatedRecords, profile)
        if (userId.isNotBlank()) {
            repo.saveEob(userId, record) { message ->
                updateUploadNotice(EobStrings.localizeRepositoryMessage(language, message))
            }
        }
        uploadText = ""
    }

    fun saveProfileToRemote(
        userId: String,
        profile: UserProfile,
        language: AppLanguage,
        onComplete: (String) -> Unit
    ) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            onComplete(EobStrings.t(language, "signInToSaveProfile"))
            return
        }
        repo.saveProfile(userId, profile, onComplete)
        repo.saveInsuranceCardMetadata(userId, profile) {}
    }

    override fun onCleared() {
        newsRotationJob?.cancel()
        eobListener?.remove()
        profileListener?.remove()
        super.onCleared()
    }
}

private fun NewsRelease.key(): String = "$company|$headline|$date"

internal fun rankNewsReleases(
    releases: List<NewsRelease>,
    contextTags: Set<String>
): List<NewsRelease> {
    return releases
        .map { article ->
            val intersectionCount = article.targetTags.intersect(contextTags).size
            val dynamicScore = article.baseRelevance + (intersectionCount * 10)
            article to dynamicScore
        }
        .filter { (_, dynamicScore) -> dynamicScore > 0 }
        .sortedByDescending { (_, dynamicScore) -> dynamicScore }
        .map { (article, _) -> article }
}
