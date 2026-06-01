package app.eob.me.ui.history

object HistoryPagination {
    const val PAGE_SIZE = 20
    const val GRID_COLUMNS = 5
    const val MAX_PAGES = 5
    const val MAX_EOBS = PAGE_SIZE * MAX_PAGES

    const val MAX_PAGE_INDEX = MAX_PAGES - 1

    fun availablePageCount(recordCount: Int): Int {
        if (recordCount <= 0) return 1
        val neededPages = ((recordCount - 1) / PAGE_SIZE) + 1
        return neededPages.coerceIn(1, MAX_PAGES)
    }

    fun pageRecords(records: List<app.eob.me.data.EobRecord>, page: Int): List<app.eob.me.data.EobRecord> {
        val safePage = page.coerceIn(0, MAX_PAGE_INDEX)
        val start = safePage * PAGE_SIZE
        return records.drop(start).take(PAGE_SIZE)
    }
}
