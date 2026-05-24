package app.eob.me.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EobViewModel : ViewModel() {
    private val _eobRecords = MutableStateFlow(
        listOf(EobAnalyzer.analyze(EobStrings.sampleEobText, "Sample camera scan", 1))
    )
    val eobRecords: StateFlow<List<EobRecord>> = _eobRecords.asStateFlow()

    val appointments = mutableStateListOf<DoctorAppointment>()
    var selectedRecord by mutableStateOf<EobRecord?>(_eobRecords.value.firstOrNull())
        private set
    var uploadText by mutableStateOf("")
    var uploadNotice by mutableStateOf("")
        private set
    var selectedCptCategory by mutableStateOf(CptCategory.OfficeVisit)
    var appealLetter by mutableStateOf(AppealLetterGenerator.generate(UserProfile(), selectedRecord))
        private set
    var firebaseStatus by mutableStateOf(FirebaseSyncStatus(isConfigured = false))
    var firebaseNews by mutableStateOf<List<NewsRelease>>(emptyList())
    private var deletedNewsKeys by mutableStateOf<Set<String>>(emptySet())
    private var eobListener: ListenerRegistration? = null

    fun fetchHistoryFromFirestore(
        repository: FirebaseEobRepository,
        userId: String,
        profile: UserProfile
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            eobListener?.remove()
            eobListener = null
            if (userId.isBlank()) return@launch
            eobListener = repository.observeEobs(
                userId = userId,
                onRecords = { records ->
                    viewModelScope.launch(Dispatchers.Default) {
                        val compacted = EobAnalyzer.compactDuplicateEobs(records)
                        withContext(Dispatchers.Main) {
                            applyRecords(compacted, profile)
                        }
                    }
                },
                onError = { message ->
                    viewModelScope.launch(Dispatchers.Main) {
                        firebaseStatus = firebaseStatus.copy(message = message)
                    }
                }
            )
        }
    }

    fun replaceRecords(newRecords: List<EobRecord>, profile: UserProfile) {
        viewModelScope.launch(Dispatchers.Default) {
            val compacted = EobAnalyzer.compactDuplicateEobs(newRecords)
            withContext(Dispatchers.Main) {
                applyRecords(compacted, profile)
            }
        }
    }

    private fun applyRecords(compacted: List<EobRecord>, profile: UserProfile) {
        _eobRecords.value = compacted
        val current = selectedRecord
        selectedRecord = if (current == null || compacted.none { it.id == current.id }) {
            compacted.firstOrNull()
        } else {
            compacted.firstOrNull { it.id == current.id }
        }
        regenerateAppeal(profile)
    }

    fun selectRecord(record: EobRecord, profile: UserProfile) {
        selectedRecord = record
        regenerateAppeal(profile)
        uploadNotice = ""
    }

    fun deleteRecord(record: EobRecord, profile: UserProfile) {
        _eobRecords.value = _eobRecords.value.filter { it.id != record.id }
        selectedRecord = _eobRecords.value.firstOrNull()
        regenerateAppeal(profile)
    }

    fun deleteNews(news: NewsRelease) {
        deletedNewsKeys = deletedNewsKeys + news.key()
        firebaseNews = firebaseNews.filterNot { it.key() == news.key() }
    }

    fun visibleNews(fallbackNews: List<NewsRelease>): List<NewsRelease> {
        return firebaseNews.ifEmpty { fallbackNews }.filterNot { it.key() in deletedNewsKeys }
    }

    fun addAppointment(date: String, provider: String, time: String, notes: String) {
        appointments.add(DoctorAppointment((appointments.maxOfOrNull { it.id } ?: 0) + 1, date, provider, time, notes))
    }

    fun removeAppointment(appointment: DoctorAppointment) {
        appointments.removeAll { it.id == appointment.id }
    }

    fun updateAppeal(text: String) {
        appealLetter = text
    }

    fun updateUploadNotice(message: String) {
        uploadNotice = message
    }

    fun regenerateAppeal(profile: UserProfile) {
        appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
    }

    fun uploadEobFile(
        repository: FirebaseEobRepository,
        userId: String,
        uri: Uri,
        sourceName: String,
        language: AppLanguage
    ) {
        repository.uploadEobFile(userId, uri, sourceName) { message ->
            uploadNotice = message.ifBlank { EobStrings.t(language, "libraryUploadStarted") }
        }
    }

    fun uploadEobBitmap(
        repository: FirebaseEobRepository,
        userId: String,
        bitmap: Bitmap,
        sourceName: String,
        language: AppLanguage
    ) {
        repository.uploadEobBitmap(userId, bitmap, sourceName) { message ->
            uploadNotice = message.ifBlank { EobStrings.t(language, "cameraScanStarted") }
        }
    }

    fun savePastedEob(repository: FirebaseEobRepository, userId: String, sourceName: String, profile: UserProfile) {
        if (uploadText.isBlank()) return
        val currentRecords = _eobRecords.value
        val analyzedRecord = EobAnalyzer.analyze(uploadText, sourceName, (currentRecords.maxOfOrNull { it.id } ?: 0) + 1)
        val duplicateIndex = currentRecords.indexOfFirst { EobAnalyzer.isSameEob(it, analyzedRecord) }
        val record = if (duplicateIndex >= 0) {
            uploadNotice = "Duplicate EOB found. The original copy was replaced with this upload."
            analyzedRecord.copy(id = currentRecords[duplicateIndex].id)
        } else {
            uploadNotice = "EOB added."
            analyzedRecord
        }
        val updatedRecords = currentRecords.toMutableList().apply {
            if (duplicateIndex >= 0) this[duplicateIndex] = record else add(record)
        }
        replaceRecords(updatedRecords, profile)
        if (userId.isNotBlank()) repository.saveEob(userId, record) { uploadNotice = it }
        uploadText = ""
    }

    override fun onCleared() {
        eobListener?.remove()
        eobListener = null
        super.onCleared()
    }
}

private fun NewsRelease.key(): String = "$company|$headline|$date"
