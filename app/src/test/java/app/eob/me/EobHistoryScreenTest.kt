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
        assertTrue(source.contains("historyListKey()"))
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

    @Test
    fun compactDuplicateEobsDeduplicatesByFirestoreIdAndHistoryListKey() {
        val sharedId = 1847362819
        val firestoreId = "1847362819"
        val sparse = EobRecord(
            id = sharedId,
            firestoreId = firestoreId,
            sourceName = "Veryfi",
            providerName = "Clinic A",
            insuranceName = "Aetna",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = "{}"
        )
        val detailed = sparse.copy(
            charges = listOf(
                app.eob.me.data.EobCharge(
                    cptCode = "99213",
                    cptDescription = "Office visit",
                    category = app.eob.me.data.CptCategory.OfficeVisit,
                    billedAmount = 120.0,
                    insurancePaidAmount = 80.0,
                    contractualAdjustmentAmount = 20.0,
                    copayAmount = 20.0,
                    deductibleAmount = 0.0,
                    coinsuranceAmount = 0.0,
                    serviceDate = "01/15/2026"
                )
            ),
            totalBilledAmount = 120.0,
            rawText = "{\"provider_name\":\"Clinic A\"}"
        )
        val collision = sparse.copy(
            firestoreId = "",
            providerName = "Clinic B",
            sourceName = "local-2"
        )

        val compacted = EobAnalyzer.compactDuplicateEobs(listOf(sparse, detailed, collision))

        assertEquals(2, compacted.size)
        assertEquals(1, compacted.count { it.firestoreId == firestoreId })
        assertTrue(compacted.first { it.firestoreId == firestoreId }.charges.isNotEmpty())
        assertEquals(compacted.size, compacted.map { it.historyListKey() }.distinct().size)
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
