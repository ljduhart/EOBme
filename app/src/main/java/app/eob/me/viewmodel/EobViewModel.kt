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
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.InsuranceCardDisplay
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.ProviderDirectoryAssurance
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.NewsRelease
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.data.ProviderSummary
import app.eob.me.data.UserProfile
import app.eob.me.data.YearlyHealthCostSummary
import app.eob.me.data.AppLockTimeout
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.HubSettingsState
import app.eob.me.data.HubSettingsStore
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.data.SettingsTab
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.repository.EobRepository
import app.eob.me.util.CacheSizeCalculator
import app.eob.me.util.NetworkUploadGate
import com.google.firebase.crashlytics.FirebaseCrashlytics
import app.eob.me.ui.history.HistoryPagination
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
    val historyPage: Int = 0,
    val calendarExpanded: Boolean = false,
    val selectedInsuranceArticle: InsuranceArticle? = null,
    val ytdBentoViewMode: YtdBentoViewMode = YtdBentoViewMode.CostOverview,
    val selectedCptCategory: CptCategory = CptCategory.OfficeVisit,
    val firebaseSyncStatus: FirebaseSyncStatus = FirebaseSyncStatus(isConfigured = false),
    val newsFeedRevision: Int = 0,
    val appealGeneratorBentoProcessing: Boolean = false,
    val appealLetterEditingEnabled: Boolean = false,
    val hubSettings: HubSettingsState = HubSettingsState()
)

/**
 * Single source of truth for authenticated hub state: EOB records, selection, appeals, news, uploads,
 * and derived hub snapshots (care team, bento, history, providers, yearly costs).
 *
 * UI layers observe [eobRecords], [sortedEobRecords], [insuranceArticles], and [uiState]; all
 * analytics and card state flow through ViewModel methods. Firestore sync goes through [EobRepository].
 */
class EobViewModel : ViewModel() {
    private var repository: EobRepository? = null
    private var settingsStore: HubSettingsStore? = null
    private var appContext: Context? = null
    private var profileListener: ListenerRegistration? = null
    private var newsListener: ListenerRegistration? = null
    private var lastBackgroundAt: Long = System.currentTimeMillis()

    private val _eobRecords = MutableStateFlow<List<EobRecord>>(emptyList())
    val eobRecords: StateFlow<List<EobRecord>> = _eobRecords.asStateFlow()

    val sortedEobRecords: StateFlow<List<EobRecord>> = eobRecords
        .map { records -> records.sortedByDescending { it.serviceDateSortKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private var uploadText: String = ""
    private var firebaseNews: List<NewsRelease> = emptyList()
    private var deletedNewsKeys: Set<String> = emptySet()
    private var syncProfile: UserProfile = UserProfile()
    private var eobListener: ListenerRegistration? = null

    private val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    private val _insuranceArticles = MutableStateFlow(EobInsuranceNews.articlesForYear(currentYear))
    val insuranceArticles: StateFlow<List<InsuranceArticle>> = _insuranceArticles.asStateFlow()

    fun attachRepository(repo: EobRepository, context: Context) {
        repository = repo
        appContext = context.applicationContext
        settingsStore = HubSettingsStore(context)
        loadHubSettings()
        refreshCacheSize()
        refreshFirebaseStatus()
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
        applyCrashlyticsCollection(stored.crashlyticsOptIn)
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
        runCatching {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
        }
    }

    fun setSettingsTab(tab: SettingsTab) {
        _uiState.update { it.copy(hubSettings = it.hubSettings.copy(selectedTab = tab)) }
    }

    fun setBiometricLoginEnabled(enabled: Boolean) {
        updateHubSettings { it.copy(biometricLoginEnabled = enabled) }
        if (!enabled) {
            _uiState.update { state -> state.copy(hubSettings = state.hubSettings.copy(appLocked = false)) }
        }
    }

    fun setAppLockTimeout(timeout: AppLockTimeout) {
        updateHubSettings { it.copy(appLockTimeout = timeout) }
    }

    fun setCrashlyticsOptIn(enabled: Boolean) {
        updateHubSettings { it.copy(crashlyticsOptIn = enabled) }
        applyCrashlyticsCollection(enabled)
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

    fun onAppBackgrounded() {
        lastBackgroundAt = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        val settings = _uiState.value.hubSettings
        if (!settings.biometricLoginEnabled) return
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
    }

    fun updateSyncProfile(profile: UserProfile) {
        syncProfile = profile.sanitizedPlanLimits()
        val selected = _uiState.value.selectedRecord
        if (selected != null) {
            _uiState.update {
                it.copy(appealLetter = AppealLetterGenerator.generate(syncProfile, selected))
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
        newsListener?.remove()
        newsListener = null
        _eobRecords.value = emptyList()
        val preservedSettings = _uiState.value.hubSettings.copy(
            settingsAccountEditing = false,
            settingsNotice = "",
            appLocked = false
        )
        _uiState.value = HubUiState(hubSettings = preservedSettings)
        syncProfile = UserProfile()
        uploadText = ""
        firebaseNews = emptyList()
        deletedNewsKeys = emptySet()
    }

    fun startFirestoreSync(userId: String, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
        val repo = repository ?: return
        updateSyncProfile(profile)
        refreshFirebaseStatus()
        fetchHistoryFromFirestore(repo, userId)
        observeProfile(repo, userId, onProfileChanged)
        observeNews(repo)
    }

    private fun observeProfile(repo: EobRepository, userId: String, onProfileChanged: (UserProfile) -> Unit) {
        profileListener?.remove()
        profileListener = repo.observeProfile(
            userId = userId,
            onProfile = onProfileChanged,
            onError = { message -> updateUploadNotice(message) }
        )
    }

    private fun observeNews(repo: EobRepository) {
        newsListener?.remove()
        newsListener = repo.observeInsuranceNews(
            onNews = { newsItems ->
                firebaseNews = newsItems
                bumpNewsFeedRevision()
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
        val profile = syncProfile
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
        firebaseNews = firebaseNews.filterNot { it.key() == news.key() }
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

    fun historyBentoSnapshot(): HistoryBentoSnapshot {
        return EobAnalyzer.historyBentoSnapshot(_eobRecords.value)
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

    fun historyRecordsForDisplay(
        filter: HistoryBentoFilter,
        searchQuery: String
    ): List<EobRecord> {
        val sorted = _eobRecords.value.sortedByDescending { it.serviceDateSortKey }
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

    fun totalBillingErrors(records: List<EobRecord>): Int {
        return records.sumOf { record ->
            EobAnalyzer.detectBillingIssues(record).count { it.severity != BillingIssueSeverity.Info }
        }
    }

    fun currentNewsReleases(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        return EobKnowledgeBase.currentNewsReleases(visibleNews(fallbackNews))
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

    fun uploadEobBitmap(userId: String, bitmap: Bitmap, sourceName: String, language: AppLanguage) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            setLoadingInvoice(false)
            updateUploadNotice(EobStrings.t(language, "signInBeforeScan"))
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
        eobListener?.remove()
        profileListener?.remove()
        newsListener?.remove()
        super.onCleared()
    }
}

private fun NewsRelease.key(): String = "$company|$headline|$date"
