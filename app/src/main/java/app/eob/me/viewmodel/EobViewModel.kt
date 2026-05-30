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
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.ui.history.HistoryPagination
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class EobViewModel : ViewModel() {
    private val _eobRecords = MutableStateFlow<List<EobRecord>>(emptyList())
    val eobRecords: StateFlow<List<EobRecord>> = _eobRecords.asStateFlow()

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    var uploadText by mutableStateOf("")
    var selectedCptCategory by mutableStateOf(CptCategory.OfficeVisit)
    var firebaseStatus by mutableStateOf(FirebaseSyncStatus(isConfigured = false))
    var firebaseNews by mutableStateOf<List<NewsRelease>>(emptyList())
    private var deletedNewsKeys by mutableStateOf<Set<String>>(emptySet())
    private var eobListener: ListenerRegistration? = null

    fun resetHubState() {
        eobListener?.remove()
        eobListener = null
        _eobRecords.value = emptyList()
        _uiState.value = HubUiState()
        uploadText = ""
        selectedCptCategory = CptCategory.OfficeVisit
        firebaseNews = emptyList()
        deletedNewsKeys = emptySet()
    }

    fun fetchHistoryFromFirestore(
        repository: FirebaseEobRepository,
        userId: String,
        profile: UserProfile
    ) {
        if (userId.isBlank()) {
            resetHubState()
            return
        }

        eobListener?.remove()
        eobListener = repository.observeEobs(
            userId = userId,
            onRecords = { records ->
                viewModelScope.launch(Dispatchers.Default) {
                    val compacted = EobAnalyzer.compactDuplicateEobs(records)
                    _eobRecords.value = compacted

                    withContext(Dispatchers.Main) {
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
            },
            onError = { message ->
                _uiState.update { it.copy(uploadNotice = message, isLoadingInvoice = false) }
            }
        )
    }

    fun replaceRecords(newRecords: List<EobRecord>, profile: UserProfile) {
        viewModelScope.launch(Dispatchers.Default) {
                    val compacted = EobAnalyzer.compactDuplicateEobs(newRecords)
                        .sortedByDescending { it.serviceDateSortKey }
                        .take(HistoryPagination.MAX_EOBS)
                    _eobRecords.value = compacted

            withContext(Dispatchers.Main) {
                val currentSelection = _uiState.value.selectedRecord
                val nextSelection = if (currentSelection == null || compacted.none { it.id == currentSelection.id }) {
                    compacted.firstOrNull()
                } else {
                    compacted.firstOrNull { it.id == currentSelection.id }
                }

                _uiState.update {
                    it.copy(
                        selectedRecord = nextSelection,
                        appealLetter = AppealLetterGenerator.generate(profile, nextSelection)
                    )
                }
            }
        }
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

    fun uploadEobFile(
        repository: FirebaseEobRepository,
        userId: String,
        uri: Uri,
        sourceName: String,
        language: AppLanguage
    ) {
        setLoadingInvoice(true)
        repository.uploadEobFile(userId, uri, sourceName) { message ->
            updateUploadNotice(message.ifBlank { EobStrings.t(language, "libraryUploadStarted") })
        }
    }

    fun uploadEobBitmap(
        repository: FirebaseEobRepository,
        userId: String,
        bitmap: Bitmap,
        sourceName: String,
        language: AppLanguage
    ) {
        setLoadingInvoice(true)
        repository.uploadEobBitmap(userId, bitmap, sourceName) { message ->
            updateUploadNotice(message.ifBlank { EobStrings.t(language, "cameraScanStarted") })
        }
    }

    fun savePastedEob(repository: FirebaseEobRepository, userId: String, sourceName: String, profile: UserProfile) {
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
            repository.saveEob(userId, record) { updateUploadNotice(it) }
        }
        uploadText = ""
    }

    override fun onCleared() {
        eobListener?.remove()
        eobListener = null
        super.onCleared()
    }
}

private fun NewsRelease.key(): String = "$company|$headline|$date"
