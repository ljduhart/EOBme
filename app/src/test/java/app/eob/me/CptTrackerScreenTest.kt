package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.BentoSnapshotExtractor
import app.eob.me.data.EobStrings
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
        assertEquals("01/15/2026", entries.first().serviceDates)
        assertTrue(entries.first().shortName.isNotBlank())
        assertTrue(entries.first().definition.isNotBlank())
    }

    @Test
    fun flashcardEntriesPreferChargeDescriptionForUnknownCodes() {
        val analyzed = EobAnalyzer.analyze(
            rawText = """
                Provider: Dental Studio
                Aetna
                01/15/2026
                D5225 billed $1438.00 insurance paid $423.35
            """.trimIndent(),
            sourceName = "test",
            nextId = 3
        )
        val record = analyzed.copy(
            charges = listOf(
                analyzed.charges.first().copy(
                    cptCode = "D5225",
                    cptDescription = "Maxillary Partial Denture - Flexible Base",
                    category = CptCategory.Other
                )
            )
        )

        val entries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.Other
        )

        assertEquals(1, entries.size)
        assertEquals("D5225", entries.first().code)
        assertEquals("Maxillary Partial Denture - Flexible Base", entries.first().definition)
        assertEquals("Maxillary Partial Denture -", entries.first().shortName)
    }

    @Test
    fun flashcardEntriesUseKnowledgeBaseDefinitionForKnownCodes() {
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\n99213 billed $120.00",
            sourceName = "test",
            nextId = 4
        )

        val entries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.OfficeVisit
        )

        assertEquals("Established patient office visit, low complexity.", entries.first().definition)
    }

    @Test
    fun unknownInjectionCodesRouteToInjectionCategoryTab() {
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\nJ9999 billed $40.00",
            sourceName = "test",
            nextId = 5
        )

        val entries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.Injection
        )

        assertEquals(1, entries.size)
        assertEquals("J9999", entries.first().code)
    }

    @Test
    fun flashcardEntriesRouteChestXrayToXRayCategoryTab() {
        val record = EobAnalyzer.analyze(
            rawText = "Provider: Imaging Center\nAetna\n01/15/2026\n71046 billed $180.00 insurance paid $90.00",
            sourceName = "test",
            nextId = 7
        )

        val entries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = AppLanguage.English,
            records = listOf(record),
            category = CptCategory.XRay
        )

        assertEquals(1, entries.size)
        assertEquals("71046", entries.first().code)
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
        assertTrue(source.contains("CptXRayChromeSilver"))
        assertTrue(source.contains("CptDmeBlack"))
        assertTrue(source.contains("CptInjectionYellow"))
        assertTrue(source.contains("CptOtherPurple"))
        assertTrue(source.contains("animateColorAsState"))
        assertTrue(source.contains("fun FlashcardItem"))
        assertTrue(source.contains("LazyVerticalGrid"))
        assertTrue(source.contains("GridCells.Adaptive(minSize = 150.dp)"))
        assertTrue(source.contains("aspectRatio(5f / 6f)"))
        assertFalse(source.contains("GridCells.Fixed(2)"))
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

    @Test
    fun flashcardEntriesExcludeChargesWithoutValidCptCode() {
        val analyzed = EobAnalyzer.analyze(
            rawText = "Provider: Clinic\nAetna\n01/15/2026\n99213 billed $120.00",
            sourceName = "test",
            nextId = 6
        )
        val template = analyzed.charges.first()
        val record = analyzed.copy(
            charges = listOf(
                template.copy(cptCode = "99213", category = CptCategory.OfficeVisit, billedAmount = 120.0),
                template.copy(cptCode = "", category = CptCategory.OfficeVisit, billedAmount = 50.0),
                template.copy(cptCode = "LAB", category = CptCategory.Lab, billedAmount = 30.0),
                template.copy(cptCode = "9921", category = CptCategory.OfficeVisit, billedAmount = 10.0)
            )
        )

        val officeEntries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.OfficeVisit
        )
        val labEntries = BentoSnapshotExtractor.buildCptFlashcardEntries(
            language = app.eob.me.data.AppLanguage.English,
            records = listOf(record),
            category = CptCategory.Lab
        )

        assertEquals(1, officeEntries.size)
        assertEquals("99213", officeEntries.first().code)
        assertEquals("$120.00", officeEntries.first().totalBilled)
        assertTrue(labEntries.isEmpty())
    }

    @Test
    fun cptFlashcardStringKeysExistForAllLanguages() {
        AppLanguage.entries.forEach { language ->
            assertEquals("DOS", EobStrings.t(language, "cptFlashcardDosLabel"))
            assertEquals("Billed", EobStrings.t(language, "cptFlashcardBilledTitle"))
        }
    }

    @Test
    fun cptFlashcardBackReservesHorizontalBilledAmountSpace() {
        val source = readSource("ui/screens/CptTrackerScreen.kt")
        val backIndex = source.indexOf("private fun FlashcardBack")
        val backEnd = source.indexOf("internal fun categoryThemeColor", backIndex)
        val backBlock = source.substring(backIndex, backEnd)
        assertTrue(backBlock.contains("widthIn(min = 64.dp)"))
        assertTrue(backBlock.contains("maxLines = 1"))
    }

    @Test
    fun cptFlashcardBackShowsDosAndBilledInVerticalStacks() {
        val source = readSource("ui/screens/CptTrackerScreen.kt")
        assertTrue(source.contains("cptFlashcardDosLabel"))
        assertTrue(source.contains("cptFlashcardBilledTitle"))
        assertTrue(source.contains("entry.serviceDates"))
        assertTrue(source.contains("entry.totalBilled"))
        val backIndex = source.indexOf("private fun FlashcardBack")
        val backEnd = source.indexOf("internal fun categoryThemeColor", backIndex)
        val backBlock = source.substring(backIndex, backEnd)
        assertTrue(backBlock.contains("verticalArrangement = Arrangement.spacedBy(2.dp)"))
        assertTrue(backBlock.indexOf("cptFlashcardDosLabel") < backBlock.indexOf("cptFlashcardBilledTitle"))
    }

    @Test
    fun cptTrackerRouteDoesNotExposeDeleteControls() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val cptRouteStart = navSource.indexOf("composable(EobRoute.CptCount.route)")
        val nextComposable = navSource.indexOf("composable(", cptRouteStart + 1)
        val cptRouteBlock = navSource.substring(cptRouteStart, nextComposable)
        assertTrue(cptRouteBlock.contains("CptTrackerScreen"))
        assertFalse(cptRouteBlock.contains("EobDeleteBar"))
        assertFalse(cptRouteBlock.contains("onDeleteEob"))
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
