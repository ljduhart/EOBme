package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.data.UserProfile
import app.eob.me.data.YtdExpenseData
import app.eob.me.data.YtdMetricKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import app.eob.me.ui.screens.ytdSummaryTitleAnchorEndIndex
import org.junit.Test

class YtdExpenseScreenTest {
    @Test
    fun ytdExpenseDataMapsSummaryAndProfileLimits() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Downtown Medical
                Aetna
                01/15/2026
                99213 billed $200.00 insurance paid $120.00 contractual adjustment $30.00
                copay $20.00 deductible $15.00 coinsurance $5.00
            """.trimIndent(),
            sourceName = "test",
            nextId = 1
        )
        val profile = UserProfile(
            annualDeductibleLimit = 3_000.0,
            annualOutOfPocketMax = 9_000.0
        )

        val data = EobAnalyzer.ytdExpenseData(listOf(record), profile, preferredYear = 2026)

        assertEquals(2026, data.year)
        assertEquals(1, data.eobCount)
        assertEquals(200.0, data.totalBilled, 0.0)
        assertEquals(120.0, data.insurancePaid, 0.0)
        assertEquals(30.0, data.adjustments, 0.0)
        assertEquals(40.0, data.patientResponsibility, 0.0)
        assertEquals(20.0, data.copays, 0.0)
        assertEquals(15.0, data.deductibles, 0.0)
        assertEquals(5.0, data.coinsurance, 0.0)
        assertEquals(3_000.0, data.deductibleMax, 0.0)
        assertEquals(9_000.0, data.outOfPocketMax, 0.0)
    }

    @Test
    fun ytdExpenseDataUsesDefaultsWhenProfileLimitsMissing() {
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\n99213 billed $50.00 insurance paid $30.00",
            sourceName = "test",
            nextId = 2
        )
        val data = EobAnalyzer.ytdExpenseData(listOf(record), UserProfile(), preferredYear = 2026)

        assertEquals(2_000.0, data.deductibleMax, 0.0)
        assertEquals(8_550.0, data.outOfPocketMax, 0.0)
    }

    @Test
    fun ytdExpenseDataBuildsExpandableMetricSectionsPerEob() {
        val records = listOf(
            EobAnalyzer.analyze(
                rawText = """
                    Provider: Clinic A
                    Aetna
                    01/15/2026
                    99213 billed $200.00 insurance paid $120.00 contractual adjustment $30.00
                    copay $25.00 deductible $0.00 coinsurance $0.00
                """.trimIndent(),
                sourceName = "a",
                nextId = 1
            ),
            EobAnalyzer.analyze(
                rawText = """
                    Provider: Clinic B
                    Aetna
                    02/10/2026
                    99213 billed $180.00 insurance paid $100.00 contractual adjustment $20.00
                    copay $25.00 deductible $0.00 coinsurance $0.00
                """.trimIndent(),
                sourceName = "b",
                nextId = 2
            ),
            EobAnalyzer.analyze(
                rawText = """
                    Provider: Clinic C
                    Aetna
                    03/05/2026
                    99213 billed $160.00 insurance paid $90.00 contractual adjustment $15.00
                    copay $25.00 deductible $0.00 coinsurance $0.00
                """.trimIndent(),
                sourceName = "c",
                nextId = 3
            )
        )

        val data = EobAnalyzer.ytdExpenseData(records, UserProfile(), preferredYear = 2026)
        val copaySection = data.metricSections.first { it.kind == YtdMetricKind.Copay }

        assertEquals(3, copaySection.lineItems.size)
        assertEquals(75.0, copaySection.total, 0.0)
        assertEquals(75.0, data.copays, 0.0)
        copaySection.lineItems.forEach { lineItem ->
            assertEquals(25.0, lineItem.amount, 0.0)
        }
    }

    @Test
    fun ytdMetricSectionTotalsAlignWithSummaryForAllChargeTypes() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Downtown Medical
                Aetna
                01/15/2026
                99213 billed $200.00 insurance paid $120.00 contractual adjustment $30.00
                copay $20.00 deductible $15.00 coinsurance $5.00
            """.trimIndent(),
            sourceName = "test",
            nextId = 10
        )
        val data = EobAnalyzer.ytdExpenseData(listOf(record), UserProfile(), preferredYear = 2026)

        assertEquals(6, data.metricSections.size)
        val totalsByKind = data.metricSections.associate { it.kind to it.total }
        assertEquals(data.copays, totalsByKind[YtdMetricKind.Copay] ?: 0.0, 0.0)
        assertEquals(data.coinsurance, totalsByKind[YtdMetricKind.Coinsurance] ?: 0.0, 0.0)
        assertEquals(data.totalBilled, totalsByKind[YtdMetricKind.TotalBilled] ?: 0.0, 0.0)
        assertEquals(data.insurancePaid, totalsByKind[YtdMetricKind.InsurancePaid] ?: 0.0, 0.0)
        assertEquals(data.adjustments, totalsByKind[YtdMetricKind.Adjustments] ?: 0.0, 0.0)
        assertEquals(data.deductibles, totalsByKind[YtdMetricKind.Deductible] ?: 0.0, 0.0)

        val copaySection = data.metricSections.first { it.kind == YtdMetricKind.Copay }
        assertEquals(1, copaySection.lineItems.size)
        assertEquals("01/15/2026", copaySection.lineItems.first().serviceDate)
        assertEquals(20.0, copaySection.lineItems.first().amount, 0.0)
    }

    @Test
    fun ytdExpenseDataFiltersAllLastFiveYears() {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val inRangeRecord = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/$currentYear\n99213 billed \$100.00 insurance paid \$60.00",
            sourceName = "current",
            nextId = 1
        )
        val oldRecord = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/${currentYear - 6}\n99213 billed \$200.00 insurance paid \$120.00",
            sourceName = "old",
            nextId = 2
        )
        val priorYearRecord = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/${currentYear - 1}\n99213 billed \$150.00 insurance paid \$90.00",
            sourceName = "prior",
            nextId = 3
        )

        val data = EobAnalyzer.ytdExpenseData(
            records = listOf(inRangeRecord, oldRecord, priorYearRecord),
            profile = UserProfile(),
            yearSelection = app.eob.me.data.YtdExpenseYearSelection.AllLastFiveYears,
            currentCalendarYear = currentYear
        )

        assertTrue(data.aggregatesAllYears)
        assertEquals(2, data.eobCount)
        assertEquals(250.0, data.totalBilled, 0.0)
    }

    @Test
    fun ytdExpenseDataFiltersSingleSelectedYear() {
        val record2025 = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2025\n99213 billed \$100.00 insurance paid \$60.00",
            sourceName = "2025",
            nextId = 1
        )
        val record2024 = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2024\n99213 billed \$200.00 insurance paid \$120.00",
            sourceName = "2024",
            nextId = 2
        )

        val data = EobAnalyzer.ytdExpenseData(
            records = listOf(record2025, record2024),
            profile = UserProfile(),
            yearSelection = app.eob.me.data.YtdExpenseYearSelection.Year(2025),
            currentCalendarYear = 2026
        )

        assertFalse(data.aggregatesAllYears)
        assertEquals(2025, data.year)
        assertEquals(1, data.eobCount)
        assertEquals(100.0, data.totalBilled, 0.0)
    }

    @Test
    fun ytdExpenseScreenContainsYearDropdownUnderSummaryTitle() {
        val source = readSource("ui/screens/YtdExpenseScreen.kt")
        val headerIndex = source.indexOf("private fun YtdSummaryHeaderCard")
        val headerBlock = source.substring(headerIndex, (headerIndex + 3_500).coerceAtMost(source.length))
        val summaryIndex = headerBlock.indexOf("ytdYearlyExpenseSummary")
        val dropdownIndex = headerBlock.indexOf("ExposedDropdownMenuBox")
        assertTrue(summaryIndex >= 0)
        assertTrue(dropdownIndex > summaryIndex)
        assertTrue(headerBlock.contains("yearOptionLabel"))
        assertTrue(headerBlock.contains("aggregatesAllYears"))
        assertTrue(headerBlock.contains("ytdYearToggleWidthUnderSummary"))
        assertFalse(
            "Year toggle should not span the full card width",
            headerBlock.contains("ExposedDropdownMenuBox(\n                expanded = yearMenuExpanded,\n                onExpandedChange = { yearMenuExpanded = it },\n                modifier = Modifier\n                    .fillMaxWidth()")
        )
    }

    @Test
    fun ytdSummaryTitleAnchorEndsAtSummaryWord() {
        val title = "Yearly Expense Summary"
        assertEquals(title.length, ytdSummaryTitleAnchorEndIndex(title))
        assertEquals(22, ytdSummaryTitleAnchorEndIndex(title))
    }

    @Test
    fun yearlyExpenseRouteWiresYearSelectionThroughViewModel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("setYtdExpenseYearSelection"))
        assertTrue(navSource.contains("ytdExpenseYearOptions"))
        assertTrue(navSource.contains("resolvedYtdExpenseYearSelection"))
        assertTrue(viewModelSource.contains("fun ytdExpenseYearOptions"))
        assertTrue(viewModelSource.contains("fun setYtdExpenseYearSelection"))
        assertTrue(viewModelSource.contains("YtdExpenseYearSelection"))
    }

    @Test
    fun ytdExpenseScreenPlacesExpandableTabsBelowGaugeGraphs() {
        val source = readSource("ui/screens/YtdExpenseScreen.kt")
        val gaugeIndex = source.indexOf("ProgressGaugeCard(")
        val expandableIndex = source.indexOf("YtdExpandableMetricSections")
        assertTrue(gaugeIndex >= 0)
        assertTrue(expandableIndex > gaugeIndex)
    }

    @Test
    fun ytdExpenseScreenContainsExpandableMetricPatterns() {
        val source = readSource("ui/screens/YtdExpenseScreen.kt")
        assertTrue(source.contains("fun YtdExpandableMetricSections"))
        assertTrue(source.contains("fun ExpandableYtdMetricCard"))
        assertTrue(source.contains("AnimatedVisibility"))
        assertTrue(source.contains("animateContentSize"))
        assertTrue(source.contains("metricSections"))
    }

    @Test
    fun ytdExpenseScreenContainsGaugeAndMetricPatterns() {
        val source = readSource("ui/screens/YtdExpenseScreen.kt")
        assertTrue(source.contains("fun ProgressGaugeCard"))
        assertTrue(source.contains("fun MetricDetailCard"))
        assertTrue(source.contains("animateFloatAsState"))
        assertFalse(source.contains("PathEffect"))
        assertTrue(source.contains("Brush.linearGradient"))
        assertTrue(source.contains("verticalScroll"))
        assertTrue(source.contains("LaunchedEffect(data.year"))
    }

    @Test
    fun yearlyExpenseRouteDelegatesToYtdExpenseScreen() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("YtdExpenseScreen"))
        assertTrue(navSource.contains("ytdExpenseData"))
        assertTrue(!navSource.contains("YearlyExpenseScreen"))
        assertTrue(viewModelSource.contains("fun ytdExpenseData"))
        assertTrue(!fileExists("ui/screens/MainScreens.kt"))
    }

    private fun fileExists(relativePath: String): Boolean {
        return listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        ).any { it.isFile }
    }

    @Test
    fun ytdExpenseDataModelContainsRequiredFields() {
        val fields = YtdExpenseData::class.java.declaredFields.map { it.name }.toSet()
        listOf(
            "year",
            "eobCount",
            "totalBilled",
            "insurancePaid",
            "adjustments",
            "patientResponsibility",
            "copays",
            "deductibles",
            "coinsurance",
            "deductibleMax",
            "outOfPocketMax",
            "metricSections"
        ).forEach { name ->
            assertTrue("Missing YtdExpenseData field: $name", fields.contains(name))
        }
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
