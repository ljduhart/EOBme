package app.eob.me.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.ClinicalNotesUiState
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.ProviderSummary
import app.eob.me.data.clinical.ClinicalNotesProviderCatalog
import app.eob.me.data.local.entity.ClinicalNote
import app.eob.me.data.repository.ClinicalNotesRepository
import app.eob.me.speech.SpeechRecognizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClinicalNotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClinicalNotesRepository(application.applicationContext)
    private val speechManager = SpeechRecognizerManager(application.applicationContext)

    private val selectedProviderId = MutableStateFlow<Int?>(null)
    private val draftQuestions = MutableStateFlow("")
    private val draftAnswers = MutableStateFlow("")
    private val isSaving = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow("")

    private val notesForProvider = selectedProviderId.flatMapLatest { providerId ->
        if (providerId == null) {
            flowOf(emptyList())
        } else {
            repository.observeNotesForProvider(providerId)
        }
    }

    private data class SpeechNotesMeta(
        val listening: Boolean,
        val transcript: String,
        val savedNotes: List<ClinicalNote>,
        val saving: Boolean,
        val error: String
    )

    val uiState: StateFlow<ClinicalNotesUiState> = combine(
        repository.observeProviders(),
        selectedProviderId,
        draftQuestions,
        draftAnswers,
        combine(
            speechManager.listening,
            speechManager.transcript,
            notesForProvider,
            isSaving,
            errorMessage
        ) { listening, transcript, savedNotes, saving, error ->
            SpeechNotesMeta(listening, transcript, savedNotes, saving, error)
        }
    ) { providers, selectedId, questions, answers, meta ->
        val options = ClinicalNotesProviderCatalog.toOptions(providers)
        val displayAnswers = if (meta.listening && meta.transcript.isNotBlank()) {
            meta.transcript
        } else {
            answers
        }
        ClinicalNotesUiState(
            providers = options,
            selectedProviderId = selectedId,
            questionsToAsk = questions,
            providerAnswers = displayAnswers,
            isListening = meta.listening,
            isSaving = meta.saving,
            errorMessage = meta.error,
            savedNotesForProvider = meta.savedNotes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClinicalNotesUiState())

    fun bootstrapProviderDirectory(
        language: AppLanguage,
        preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
        providerSummaries: List<ProviderSummary>
    ) {
        viewModelScope.launch {
            val entries = ClinicalNotesProviderCatalog.buildDirectoryEntries(
                language = language,
                preferredDoctors = preferredDoctors,
                providerSummaries = providerSummaries
            )
            repository.syncProviderDirectory(entries)
            if (selectedProviderId.value == null) {
                entries.firstOrNull()?.providerId?.let { selectProvider(it) }
            }
        }
    }

    fun selectProvider(providerId: Int) {
        selectedProviderId.value = providerId
        draftQuestions.value = ""
        draftAnswers.value = ""
        errorMessage.value = ""
        speechManager.destroy()
    }

    fun updateQuestions(value: String) {
        draftQuestions.value = value
    }

    fun updateAnswers(value: String) {
        draftAnswers.value = value
        if (!speechManager.listening.value) {
            speechManager.setTranscriptBase(value)
        }
    }

    fun onSpeechTranscriptCommitted(transcript: String) {
        draftAnswers.value = transcript
    }

    fun toggleSpeechRecognition() {
        if (speechManager.listening.value) {
            speechManager.stopListening()
            onSpeechTranscriptCommitted(speechManager.transcript.value)
            return
        }
        speechManager.beginDictationSession(draftAnswers.value)
        speechManager.startListening()
    }

    fun saveClinicalNote() {
        val providerId = selectedProviderId.value
        val questions = draftQuestions.value.trim()
        val answers = draftAnswers.value.trim()
        if (providerId == null) {
            errorMessage.value = "clinicalNotesProviderRequired"
            return
        }
        if (questions.isBlank() && answers.isBlank()) {
            errorMessage.value = "clinicalNotesContentRequired"
            return
        }
        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = ""
            repository.insertNote(
                ClinicalNote(
                    providerId = providerId,
                    dateCreated = System.currentTimeMillis(),
                    questionsToAsk = questions,
                    providerAnswers = answers
                )
            )
            draftQuestions.value = ""
            draftAnswers.value = ""
            speechManager.setTranscriptBase("")
            isSaving.value = false
        }
    }

    fun clearSessionDrafts() {
        draftQuestions.value = ""
        draftAnswers.value = ""
        errorMessage.value = ""
        speechManager.destroy()
    }

    override fun onCleared() {
        speechManager.destroy()
        super.onCleared()
    }
}
