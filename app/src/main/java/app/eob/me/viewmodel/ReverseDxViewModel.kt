package app.eob.me.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.dx.ReverseDxSearchState
import app.eob.me.data.dx.ReverseDxUiState
import app.eob.me.data.repository.DxCptRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReverseDxViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DxCptRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(ReverseDxUiState())
    val uiState: StateFlow<ReverseDxUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        searchDx(value)
    }

    fun searchDx(input: String) {
        searchJob?.cancel()
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchState = ReverseDxSearchState.Idle) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(searchState = ReverseDxSearchState.Loading) }
            delay(SEARCH_DEBOUNCE_MS)
            val entry = repository.getDxDetails(trimmed)
            _uiState.update { current ->
                if (current.query.trim() != trimmed) {
                    current
                } else if (entry == null) {
                    current.copy(searchState = ReverseDxSearchState.NotFound(trimmed))
                } else if (entry.totalPotentialMatches < MATCH_THRESHOLD) {
                    current.copy(searchState = ReverseDxSearchState.Results(entry))
                } else {
                    current.copy(searchState = ReverseDxSearchState.ThresholdExceeded(entry))
                }
            }
        }
    }

    fun clearSession() {
        searchJob?.cancel()
        _uiState.value = ReverseDxUiState()
    }

    companion object {
        const val MATCH_THRESHOLD = 50
        private const val SEARCH_DEBOUNCE_MS = 220L
    }
}
