package app.eob.me

import app.eob.me.data.dx.CptCategory
import app.eob.me.data.dx.DxCptEntry
import app.eob.me.data.dx.ReverseDxRules
import app.eob.me.data.dx.ReverseDxSearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverseDxRulesTest {
    @Test
    fun thresholdBoundaryRoutesResultsBelowFifty() {
        val entry = sampleEntry(totalPotentialMatches = 49)
        val state = ReverseDxRules.resolveSearchState("E03.9", entry)
        assertTrue(state is ReverseDxSearchState.Results)
    }

    @Test
    fun thresholdBoundaryRoutesExceededAtFifty() {
        val entry = sampleEntry(totalPotentialMatches = 50)
        val state = ReverseDxRules.resolveSearchState("I10", entry)
        assertTrue(state is ReverseDxSearchState.ThresholdExceeded)
    }

    @Test
    fun missingEntryRoutesNotFound() {
        val state = ReverseDxRules.resolveSearchState("ZZZ.9", null)
        assertTrue(state is ReverseDxSearchState.NotFound)
        assertEquals("ZZZ.9", (state as ReverseDxSearchState.NotFound).query)
    }

    @Test
    fun blankQueryRoutesIdle() {
        val state = ReverseDxRules.resolveSearchState("", sampleEntry(10))
        assertEquals(ReverseDxSearchState.Idle, state)
    }

    private fun sampleEntry(totalPotentialMatches: Int): DxCptEntry {
        return DxCptEntry(
            dxCode = "SAMPLE",
            description = "Sample",
            categories = listOf(CptCategory("E&M Visits", "99212 - 99215")),
            totalPotentialMatches = totalPotentialMatches
        )
    }
}
