package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.ui.history.HistoryPagination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPaginationFlowTest {
    @Test
    fun pageRecordsRespectsPageSize() {
        val records = (1..25).map { id ->
            EobAnalyzer.analyze(
                rawText = "Provider: Clinic $id\nAetna\n01/15/2026\n99213 billed \$50.00 insurance paid \$30.00",
                sourceName = "r$id",
                nextId = id
            )
        }
        val page0 = HistoryPagination.pageRecords(records, 0)
        assertEquals(HistoryPagination.PAGE_SIZE, page0.size)
        val page1 = HistoryPagination.pageRecords(records, 1)
        assertEquals(5, page1.size)
    }

    @Test
    fun availablePageCountCapsAtMaxPages() {
        val hugeCount = HistoryPagination.MAX_EOBS + 50
        assertEquals(HistoryPagination.MAX_PAGES, HistoryPagination.availablePageCount(hugeCount))
    }

    @Test
    fun emptyRecordsStillExposeOnePage() {
        assertEquals(1, HistoryPagination.availablePageCount(0))
        assertTrue(HistoryPagination.pageRecords(emptyList(), 0).isEmpty())
    }
}
