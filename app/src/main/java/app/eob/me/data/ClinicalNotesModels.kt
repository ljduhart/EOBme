package app.eob.me.data

import app.eob.me.data.local.entity.ClinicalNote
import app.eob.me.data.local.entity.ProviderDirectoryEntity

data class ClinicalProviderOption(
    val providerId: Int,
    val displayLabel: String
)

data class ClinicalNotesUiState(
    val providers: List<ClinicalProviderOption> = emptyList(),
    val selectedProviderId: Int? = null,
    val questionsToAsk: String = "",
    val providerAnswers: String = "",
    val isListening: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String = "",
    val savedNotesForProvider: List<ClinicalNote> = emptyList()
)
