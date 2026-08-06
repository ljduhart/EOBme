package app.eob.me.data.dx

data class CptCategory(
    val name: String,
    val range: String
)

data class DxCptEntry(
    val dxCode: String,
    val description: String,
    val categories: List<CptCategory>,
    val totalPotentialMatches: Int
)

sealed interface ReverseDxSearchState {
    data object Idle : ReverseDxSearchState
    data object Loading : ReverseDxSearchState
    data class Results(val entry: DxCptEntry) : ReverseDxSearchState
    data class ThresholdExceeded(val entry: DxCptEntry) : ReverseDxSearchState
    data class NotFound(val query: String) : ReverseDxSearchState
}

data class ReverseDxUiState(
    val query: String = "",
    val searchState: ReverseDxSearchState = ReverseDxSearchState.Idle
)
