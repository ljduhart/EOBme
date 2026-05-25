package app.eob.me.app

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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

class EobViewModel : ViewModel() {
    val records = mutableStateListOf(EobAnalyzer.analyze(EobStrings.sampleEobText, "Sample camera scan", 1))
    val appointments = mutableStateListOf<DoctorAppointment>()
    var selectedRecord by mutableStateOf<EobRecord?>(records.firstOrNull())
        private set
    var uploadText by mutableStateOf("")
    var uploadNotice by mutableStateOf("")
        private set
    var selectedCptCategory by mutableStateOf(CptCategory.OfficeVisit)
    var appealLetter by mutableStateOf(AppealLetterGenerator.generate(UserProfile(), selectedRecord))
        private set
    var firebaseStatus by mutableStateOf(FirebaseSyncStatus(isConfigured = false))
    var firebaseNews by mutableStateOf<List<NewsRelease>>(emptyList())

    fun replaceRecords(newRecords: List<EobRecord>, profile: UserProfile) {
        val compacted = EobAnalyzer.compactDuplicateEobs(newRecords)
        records.clear()
        records.addAll(compacted)
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

    fun addAppointment(date: String, provider: String, time: String, notes: String) {
        appointments.add(DoctorAppointment((appointments.maxOfOrNull { it.id } ?: 0) + 1, date, provider, time, notes))
    }

    fun removeAppointment(appointment: DoctorAppointment) {
        appointments.removeAll { it.id == appointment.id }
    }

    fun updateAppeal(text: String) {
        appealLetter = text
    }

    fun regenerateAppeal(profile: UserProfile) {
        appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
    }

    fun syncFirebaseStatus(repository: FirebaseEobRepository, userId: String) {
        firebaseStatus = repository.status().copy(userId = userId)
    }

    fun updateUploadNotice(message: String) {
        uploadNotice = message
    }

    fun importEobFromText(rawText: String, sourceName: String, profile: UserProfile, language: AppLanguage): EobRecord {
        val analyzedRecord = EobAnalyzer.analyze(rawText, sourceName, (records.maxOfOrNull { it.id } ?: 0) + 1)
        val duplicateIndex = records.indexOfFirst { EobAnalyzer.isSameEob(it, analyzedRecord) }
        val record = if (duplicateIndex >= 0) {
            uploadNotice = EobStrings.t(language, "duplicateReplaced")
            analyzedRecord.copy(id = records[duplicateIndex].id)
        } else {
            uploadNotice = EobStrings.t(language, "eobAdded")
            analyzedRecord
        }
        if (duplicateIndex >= 0) {
            records[duplicateIndex] = record
        } else {
            records.add(record)
        }
        replaceRecords(records, profile)
        selectedRecord = record
        return record
    }

    fun uploadEobFile(
        repository: FirebaseEobRepository,
        userId: String,
        uri: Uri,
        sourceName: String,
        language: AppLanguage
    ) {
        repository.uploadEobFile(userId, uri, sourceName) { message ->
            if (message.isNotBlank()) uploadNotice = message
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
            if (message.isNotBlank()) uploadNotice = message
        }
    }

    fun savePastedEob(
        repository: FirebaseEobRepository,
        userId: String,
        sourceName: String,
        profile: UserProfile,
        language: AppLanguage
    ) {
        if (uploadText.isBlank()) return
        val record = importEobFromText(uploadText, sourceName, profile, language)
        if (userId.isNotBlank()) repository.saveEob(userId, record) { uploadNotice = it }
        uploadText = ""
    }
}
