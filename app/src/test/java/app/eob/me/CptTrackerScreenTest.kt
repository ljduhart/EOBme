package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.BentoSnapshotExtractor
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CptTrackerScreenTest {
    @Test
    fun buildCptFlashcardEntriesAggregatesChargesByCode() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Downtown Medical
                Aetna
                01/15/2026
                99213 billed $200.00 insurance paid $120.00 contractual adjustment $30.00
                copay $20.00 deductible $15.00 coinsurance $5.00
                99213 billed $150.00 insurance paid $90.00 contractual adjustment $20.00
            """.trimIndent(),
            sourceName = "test",
            nextId = 1
        )

        val entries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.OfficeVisit
        )

        assertEquals(1, entries.size)
        assertEquals("99213", entries.first().code)
        assertEquals("$350.00", entries.first().totalBilled)
        assertTrue(entries.first().shortName.isNotBlank())
        assertTrue(entries.first().definition.isNotBlank())
    }

    @Test
    fun cptTrackerScreenContainsFlashcardGridPatterns() {
        val source = readSource("ui/screens/CptTrackerScreen.kt")
        assertTrue(source.contains("fun CptTrackerScreen"))
        assertTrue(source.contains("fun CptCategoryTabs"))
        assertTrue(source.contains("categoryThemeColor"))
        assertTrue(source.contains("CptOfficeVisitBlue"))
        assertTrue(source.contains("CptLabGreen"))
        assertTrue(source.contains("CptHospitalRed"))
        assertTrue(source.contains("CptDmeBlack"))
        assertTrue(source.contains("CptInjectionYellow"))
        assertTrue(source.contains("CptOtherPurple"))
        assertTrue(source.contains("animateColorAsState"))
        assertTrue(source.contains("fun FlashcardItem"))
        assertTrue(source.contains("LazyVerticalGrid"))
        assertTrue(source.contains("GridCells.Fixed(2)"))
        assertTrue(source.contains("animateFloatAsState"))
        assertTrue(source.contains("cameraDistance = 12f * density.density"))
        assertTrue(source.contains("rotation <= 90f"))
        assertTrue(source.contains(".border(width = 4.dp"))
        assertTrue(source.contains("CptEducationalBanner"))
        assertTrue(source.contains("ElevatedCard"))
        assertFalse(source.contains("CptCountScreen"))
    }

    @Test
    fun cptRouteDelegatesToViewModelFlashcardEntries() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("CptTrackerScreen"))
        assertTrue(navSource.contains("cptFlashcardEntries"))
        assertTrue(!navSource.contains("CptCountScreen"))
        assertTrue(viewModelSource.contains("fun cptFlashcardEntries"))
    }

    @Test
    fun eobViewModelExposesCptFlashcardEntries() {
        val viewModel = EobViewModel()
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\n80053 billed $48.00 insurance paid $22.00",
            sourceName = "test",
            nextId = 2
        )
        val entries = viewModel.cptFlashcardEntries(
            records = listOf(record),
            category = CptCategory.Lab,
            language = app.eob.me.data.AppLanguage.English
        )
        assertEquals(1, entries.size)
        assertEquals("80053", entries.first().code)
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
