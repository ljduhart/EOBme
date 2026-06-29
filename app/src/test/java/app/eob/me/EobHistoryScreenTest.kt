package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobHistoryPaymentFilter
import app.eob.me.data.EobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(sections[0].header.contains("2026"))
        assertEquals(1, sections[0].rows.size)
        assertEquals(1, sections[1].rows.size)
        assertTrue(sections[0].rows.first().isFirstInMonth)
        assertEquals(sections[0].lazySectionKey(), "section-${sections[0].monthSortKey}")
    }

    @Test
    fun groupHistoryByMonthUsesUniqueLazySectionKeysForInvalidDates() {
        val unknownA = sampleRecord(id = 1, rawText = "billed \$50").copy(
            serviceDate = "Date not recognized",
            serviceDateSortKey = Int.MAX_VALUE,
            firestoreId = "unknown-a"
        )
        val unknownB = sampleRecord(id = 2, rawText = "billed \$40").copy(
            serviceDate = "not-a-date",
            serviceDateSortKey = 0,
            firestoreId = "unknown-b"
        )

        val sections = EobAnalyzer.groupHistoryByMonth(listOf(unknownA, unknownB), app.eob.me.data.AppLanguage.English)
        val sectionKeys = sections.map { it.lazySectionKey() }
        val itemKeys = sections.flatMap { section ->
            section.rows.map { row -> section.lazyItemKey(row.record) }
        }

        assertEquals(sectionKeys.size, sectionKeys.distinct().size)
        assertEquals(itemKeys.size, itemKeys.distinct().size)
    }

    @Test
    fun eobHistoryScreenUsesStableLazyColumnKeys() {
        val source = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(source.contains("lazySectionKey()"))
        assertTrue(source.contains("lazyItemKey(row.record)"))
        assertFalse(source.contains("stickyHeader(key = \"header-\$header\")"))
        assertFalse(source.contains("key = { it.record.id }"))
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
    fun viewModelHistoryTimelinePathProducesUniqueLazyKeys() {
        val records = listOf(
            EobRecord(
                id = 1,
                firestoreId = "veryfi-doc-a",
                sourceName = "Veryfi",
                providerName = "Clinic A",
                insuranceName = "Aetna",
                serviceDate = "Date not recognized",
                serviceDateSortKey = Int.MAX_VALUE,
                charges = emptyList(),
                duplicateChargeWarnings = emptyList(),
                rawText = "{}"
            ),
            EobRecord(
                id = 2,
                firestoreId = "veryfi-doc-b",
                sourceName = "Veryfi",
                providerName = "Clinic B",
                insuranceName = "Cigna",
                serviceDate = "not-a-date",
                serviceDateSortKey = 0,
                charges = emptyList(),
                duplicateChargeWarnings = emptyList(),
                rawText = "{}"
            )
        )
        val sections = EobAnalyzer.groupHistoryByMonth(
            EobAnalyzer.compactDuplicateEobs(records),
            app.eob.me.data.AppLanguage.English
        )
        val sectionKeys = sections.map { it.lazySectionKey() }
        val itemKeys = sections.flatMap { section ->
            section.rows.map { row -> section.lazyItemKey(row.record) }
        }

        assertTrue(sectionKeys.isNotEmpty())
        assertEquals(sectionKeys.size, sectionKeys.distinct().size)
        assertEquals(itemKeys.size, itemKeys.distinct().size)
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

    @Test
    fun historyAppealPillsVisibleOnlyForSelectedExpandedRecord() {
        val source = readSource("ui/screens/EobHistoryScreen.kt")
        assertTrue(source.contains("HistoryAppealPillButtons"))
        assertTrue(source.contains("isSelected"))
        assertTrue(source.contains("if (isSelected)"))
        assertTrue(source.contains("Color(0xFF2979FF)"))
        assertTrue(source.contains("Color(0xFFE53935)"))
        assertTrue(source.contains("onAppealDoctor"))
        assertTrue(source.contains("onAppealInsurance"))
    }

    @Test
    fun historyRouteWiresAppealPillsThroughViewModel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("openAppealForRecord"))
        assertTrue(navSource.contains("onAppealDoctor"))
        assertTrue(navSource.contains("onAppealInsurance"))
        assertTrue(navSource.contains("EobRoute.Appeal.route"))
        assertTrue(navSource.contains("selectedRecord = uiState.selectedRecord"))
        assertTrue(viewModelSource.contains("fun openAppealForRecord"))
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
