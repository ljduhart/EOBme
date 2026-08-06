package app.eob.me.data.dx

object ReverseDxRules {
    const val MATCH_THRESHOLD = 50

    fun resolveSearchState(trimmedQuery: String, entry: DxCptEntry?): ReverseDxSearchState {
        if (trimmedQuery.isEmpty()) {
            return ReverseDxSearchState.Idle
        }
        if (entry == null) {
            return ReverseDxSearchState.NotFound(trimmedQuery)
        }
        return if (entry.totalPotentialMatches < MATCH_THRESHOLD) {
            ReverseDxSearchState.Results(entry)
        } else {
            ReverseDxSearchState.ThresholdExceeded(entry)
        }
    }
}
