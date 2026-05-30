package app.eob.me.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.AppLanguage
import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.CptCategory
import app.eob.me.data.DocumentBounds
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.data.repository.EobRepository
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

data class HubUiState(
    val selectedRecord: EobRecord? = null,
    val uploadNotice: String = "",
    val appealLetter: String = "",
    val appointments: List<DoctorAppointment> = emptyList(),
    val isLoadingInvoice: Boolean = false,
    val historyPage: Int = 0
)

data class CameraScanUiState(
    val documentBounds: DocumentBounds? = null,
    val isDocumentDetected: Boolean = false,
    val scannerHint: String = "Align your insurance document inside the frame"
)

/**
 * Single source of truth for authenticated hub state: EOB records, selection, appeals, news, uploads.
 * UI layers observe [eobRecords] and [uiState] only; Firestore sync goes through [EobRepository].
 */
class EobViewModel : ViewModel() {
    private var repository: EobRepository? = null
    private var profileListener: ListenerRegistration? = null
    private var newsListener: ListenerRegistration? = null

    private val _eobRecords = MutableStateFlow<List<EobRecord>>(emptyList())
    val eobRecords: StateFlow<List<EobRecord>> = _eobRecords.asStateFlow()

    val sortedEobRecords: StateFlow<List<EobRecord>> = eobRecords
        .map { records -> records.sortedByDescending { it.serviceDateSortKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    private val _cameraScanState = MutableStateFlow(CameraScanUiState())
    val cameraScanState: StateFlow<CameraScanUiState> = _cameraScanState.asStateFlow()

    private var smoothedDocumentBounds: DocumentBounds? = null

    var uploadText by mutableStateOf("")
    var selectedCptCategory by mutableStateOf(CptCategory.OfficeVisit)
    var firebaseStatus by mutableStateOf(FirebaseSyncStatus(isConfigured = false))
    var firebaseNews by mutableStateOf<List<NewsRelease>>(emptyList())
    private var deletedNewsKeys by mutableStateOf<Set<String>>(emptySet())
    private var eobListener: ListenerRegistration? = null

    fun attachRepository(repo: EobRepository) {
        repository = repo
        refreshFirebaseStatus()
    }

    fun refreshFirebaseStatus() {
        repository?.let { firebaseStatus = it.status() }
    }

    fun resetHubState() {
        eobListener?.remove()
        eobListener = null
        profileListener?.remove()
        profileListener = null
        newsListener?.remove()
        newsListener = null
        _eobRecords.value = emptyList()
        _uiState.value = HubUiState()
        uploadText = ""
        selectedCptCategory = CptCategory.OfficeVisit
        firebaseNews = emptyList()
        deletedNewsKeys = emptySet()
        clearCameraScanState()
    }

    fun startFirestoreSync(userId: String, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
        val repo = repository ?: return
        refreshFirebaseStatus()
        fetchHistoryFromFirestore(repo, userId, profile)
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
            onNews = { newsItems -> firebaseNews = newsItems },
            onError = { message -> updateUploadNotice(message) }
        )
    }

    fun fetchHistoryFromFirestore(repo: EobRepository, userId: String, profile: UserProfile) {
        if (userId.isBlank()) {
            resetHubState()
            return
        }

        eobListener?.remove()
        eobListener = repo.observeEobs(
            userId = userId,
            onRecords = { records -> applyRemoteRecords(records, profile) },
            onError = { message ->
                _uiState.update { it.copy(uploadNotice = message, isLoadingInvoice = false) }
            }
        )
    }

    private fun applyRemoteRecords(records: List<EobRecord>, profile: UserProfile) {
        viewModelScope.launch(Dispatchers.Default) {
            val compacted = EobAnalyzer.compactDuplicateEobs(records)
                .sortedByDescending { it.serviceDateSortKey }
                .take(HistoryPagination.MAX_EOBS)

            withContext(Dispatchers.Main) {
                _eobRecords.value = compacted
                val currentSelection = _uiState.value.selectedRecord
                val nextSelection = if (currentSelection == null || compacted.none { it.id == currentSelection.id }) {
                    compacted.firstOrNull()
                } else {
                    compacted.firstOrNull { it.id == currentSelection.id }
                }
                _uiState.update {
                    it.copy(
                        selectedRecord = nextSelection,
                        appealLetter = AppealLetterGenerator.generate(profile, nextSelection),
                        isLoadingInvoice = false
                    )
                }
            }
        }
    }

    fun replaceRecords(newRecords: List<EobRecord>, profile: UserProfile) {
        applyRemoteRecords(newRecords, profile)
    }

    fun selectRecord(record: EobRecord, profile: UserProfile) {
        _uiState.update {
            it.copy(
                selectedRecord = record,
                uploadNotice = "",
                appealLetter = AppealLetterGenerator.generate(profile, record)
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
                appealLetter = AppealLetterGenerator.generate(profile, nextSelection)
            )
        }
    }

    fun deleteRecordRemote(
        userId: String,
        record: EobRecord,
        profile: UserProfile,
        onComplete: (String) -> Unit = {}
    ) {
        deleteRecord(record, profile)
        val repo = repository
        if (userId.isNotBlank() && repo != null) {
            repo.deleteEob(userId, record, onComplete)
        } else if (userId.isBlank()) {
            onComplete("Please sign in to delete EOBs from the cloud.")
        }
    }

    fun deleteNews(news: NewsRelease) {
        deletedNewsKeys = deletedNewsKeys + news.key()
        firebaseNews = firebaseNews.filterNot { it.key() == news.key() }
    }

    fun visibleNews(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        return firebaseNews.ifEmpty { fallbackNews }.filterNot { it.key() in deletedNewsKeys }
    }

    fun addAppointment(date: String, provider: String, time: String, notes: String) {
        _uiState.update { state ->
            val nextId = (state.appointments.maxOfOrNull { it.id } ?: 0) + 1
            state.copy(
                appointments = state.appointments + DoctorAppointment(nextId, date, provider, time, notes)
            )
        }
    }

    fun removeAppointment(appointment: DoctorAppointment) {
        _uiState.update { state ->
            state.copy(appointments = state.appointments.filterNot { it.id == appointment.id })
        }
    }

    fun updateAppeal(text: String) {
        _uiState.update { it.copy(appealLetter = text) }
    }

    fun updateUploadNotice(message: String) {
        _uiState.update { it.copy(uploadNotice = message) }
    }

    fun setLoadingInvoice(loading: Boolean) {
        _uiState.update { it.copy(isLoadingInvoice = loading) }
    }

    fun setHistoryPage(page: Int) {
        _uiState.update { it.copy(historyPage = page.coerceIn(0, HistoryPagination.MAX_PAGE_INDEX)) }
    }

    fun regenerateAppeal(profile: UserProfile) {
        val selected = _uiState.value.selectedRecord
        _uiState.update { it.copy(appealLetter = AppealLetterGenerator.generate(profile, selected)) }
    }

    fun uploadEobFile(userId: String, uri: Uri, sourceName: String, language: AppLanguage) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            setLoadingInvoice(false)
            updateUploadNotice("Please sign in before uploading an EOB.")
            return
        }
        setLoadingInvoice(true)
        repo.uploadEobFile(userId, uri, sourceName) { message ->
            val notice = message.ifBlank { EobStrings.t(language, "libraryUploadStarted") }
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
            updateUploadNotice("Please sign in before scanning an EOB.")
            return
        }
        setLoadingInvoice(true)
        repo.uploadEobBitmap(userId, bitmap, sourceName) { message ->
            val notice = message.ifBlank { EobStrings.t(language, "cameraScanStarted") }
            updateUploadNotice(notice)
            if (message.contains("failed", ignoreCase = true)) {
                setLoadingInvoice(false)
            }
        }
    }

    fun savePastedEob(userId: String, sourceName: String, profile: UserProfile) {
        val repo = repository ?: return
        if (uploadText.isBlank()) return
        val currentRecords = _eobRecords.value
        val analyzedRecord = EobAnalyzer.analyze(uploadText, sourceName, (currentRecords.maxOfOrNull { it.id } ?: 0) + 1)
        val duplicateIndex = currentRecords.indexOfFirst { EobAnalyzer.isSameEob(it, analyzedRecord) }
        val notice = if (duplicateIndex >= 0) {
            "Duplicate EOB found. The original copy was replaced with this upload."
        } else {
            "EOB added."
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
            repo.saveEob(userId, record) { updateUploadNotice(it) }
        }
        uploadText = ""
    }

    fun updateDocumentBounds(detected: DocumentBounds?) {
        val blended = blendDocumentBounds(smoothedDocumentBounds, detected)
        smoothedDocumentBounds = blended
        _cameraScanState.update { state ->
            state.copy(
                documentBounds = blended,
                isDocumentDetected = blended?.isDetected == true,
                scannerHint = if (blended?.isDetected == true) {
                    "Document detected — tap capture when ready"
                } else {
                    "Align your insurance document inside the frame"
                }
            )
        }
    }

    fun clearCameraScanState() {
        smoothedDocumentBounds = null
        _cameraScanState.value = CameraScanUiState()
    }

    private fun blendDocumentBounds(
        previous: DocumentBounds?,
        detected: DocumentBounds?
    ): DocumentBounds? {
        if (detected == null) return null
        if (previous == null) return detected
        val alpha = 0.55f
        return DocumentBounds(
            left = previous.left + (detected.left - previous.left) * alpha,
            top = previous.top + (detected.top - previous.top) * alpha,
            right = previous.right + (detected.right - previous.right) * alpha,
            bottom = previous.bottom + (detected.bottom - previous.bottom) * alpha
        )
    }

    fun saveProfileToRemote(userId: String, profile: UserProfile, onComplete: (String) -> Unit) {
        val repo = repository ?: return
        if (userId.isBlank()) {
            onComplete("Please sign in to save your profile.")
            return
        }
        repo.saveProfile(userId, profile, onComplete)
        repo.saveInsuranceCardMetadata(userId, profile) {}
    }

    override fun onCleared() {
        eobListener?.remove()
        profileListener?.remove()
        newsListener?.remove()
        clearCameraScanState()
        super.onCleared()
    }
}

private fun NewsRelease.key(): String = "$company|$headline|$date"
