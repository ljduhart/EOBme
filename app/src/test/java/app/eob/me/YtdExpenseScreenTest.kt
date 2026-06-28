package app.eob.me

import app.eob.me.data.EobAnalyzer
import app.eob.me.data.UserProfile
import app.eob.me.data.YtdExpenseData
import app.eob.me.data.YtdMetricKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertTrue(source.contains("LaunchedEffect(Unit)"))
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
