package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobHistoryPaymentFilter
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EobHistoryScreenTest {
    @Test
    fun paymentFilterSeparatesPaidAndPendingRecords() {
        val paid = sampleRecord(id = 1, rawText = "billed \$100 insurance paid \$80")
        val pending = sampleRecord(id = 2, rawText = "billed \$100 insurance paid \$0")

        val paidOnly = EobAnalyzer.filterHistoryByPayment(listOf(paid, pending), EobHistoryPaymentFilter.Paid)
        val pendingOnly = EobAnalyzer.filterHistoryByPayment(listOf(paid, pending), EobHistoryPaymentFilter.Pending)

        assertEquals(1, paidOnly.size)
        assertEquals(1, pendingOnly.size)
        assertEquals(1, paidOnly.first().id)
        assertEquals(2, pendingOnly.first().id)
    }

    @Test
    fun groupHistoryByMonthBuildsStickyHeaderSections() {
        val january = sampleRecord(id = 1, rawText = "01/15/2026 billed \$50 insurance paid \$30")
        val february = sampleRecord(id = 2, rawText = "02/10/2026 billed \$40 insurance paid \$20")

        val sections = EobAnalyzer.groupHistoryByMonth(listOf(january, february), app.eob.me.data.AppLanguage.English)

        assertEquals(2, sections.size)
        assertTrue(sections[0].first.contains("2026"))
        assertEquals(1, sections[0].second.size)
        assertEquals(1, sections[1].second.size)
        assertTrue(sections[0].second.first().isFirstInMonth)
    }

    @Test
    fun eobHistoryScreenUsesWalletTimelineAndReceiptPatterns() {
        val source = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(source.contains("FilterChip"))
        assertTrue(source.contains("stickyHeader"))
        assertTrue(source.contains("SwipeToDismissBox"))
        assertTrue(source.contains("ExtendedFloatingActionButton"))
        assertTrue(source.contains("PathEffect.dashPathEffect"))
        assertTrue(source.contains("animateContentSize"))
        assertTrue(source.contains("key = { it.record.id }"))
    }

    @Test
    fun historyRouteDelegatesToEobHistoryScreen() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val screenSource = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(navSource.contains("EobHistoryScreen"))
        assertTrue(navSource.contains("historyTimelineSections"))
        assertTrue(navSource.contains("setHistoryPaymentFilter"))
        assertTrue(navSource.contains("selectRecord"))
        assertTrue(!navSource.contains("HistoryGridScreen"))
        assertTrue(!screenSource.contains("EobAnalyzer.filterHistoryByPayment"))
    }

    @Test
    fun eobViewModelExposesHistoryTimelineSections() {
        val source = readSource("viewmodel/EobViewModel.kt")
        assertTrue(source.contains("fun historyTimelineSections"))
        assertTrue(source.contains("fun setHistoryPaymentFilter"))
        assertTrue(source.contains("historyPaymentFilter"))
    }

    private fun sampleRecord(id: Int, rawText: String): EobRecord {
        return EobAnalyzer.analyze(
            rawText = "Provider: Clinic $id\nAetna\n$rawText",
            sourceName = "test-$id",
            nextId = id
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
