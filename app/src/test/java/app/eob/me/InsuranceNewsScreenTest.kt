package app.eob.me

import app.eob.me.data.MajorInsuranceCarrier
import app.eob.me.data.NewsRelease
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsuranceNewsScreenTest {
    @Test
    fun newsReleaseResolvesArticleUrlFromDedicatedField() {
        val release = NewsRelease(
            company = "Healthcare Dive",
            headline = "Headline",
            summary = "Summary text",
            date = "06/18/2026",
            articleUrl = "https://example.com/article"
        )
        assertEquals("https://example.com/article", release.resolvedArticleUrl())
        assertEquals("Summary text", release.displaySummary())
    }

    @Test
    fun newsReleaseFallsBackToUrlEmbeddedInSummary() {
        val release = NewsRelease(
            company = "Healthcare Dive",
            headline = "Headline",
            summary = "Summary text\n\nhttps://example.com/legacy",
            date = "06/18/2026"
        )
        assertEquals("https://example.com/legacy", release.resolvedArticleUrl())
        assertEquals("Summary text", release.displaySummary())
    }

    @Test
    fun insuranceNewsScreenContainsCarrierHubAndBriefingPatterns() {
        val source = readSource("ui/screens/NewsScreen.kt")
        assertTrue(source.contains("fun CarrierCard"))
        assertTrue(source.contains("fun NewsBriefingCard"))
        assertTrue(source.contains("fun openCustomTab"))
        assertTrue(source.contains("rememberInfiniteTransition"))
        assertTrue(source.contains("CustomTabsIntent"))
        assertTrue(source.contains("LazyRow"))
        assertTrue(source.contains("insuranceNewsMonthlyBriefingsCount"))
        assertTrue(source.contains("canOpenArticle"))
        assertTrue(source.contains("if (canOpenArticle)"))
        assertFalse(source.contains("HolographicGlassCard"))
        assertFalse(source.contains("HomeInsuranceNewsSection"))
    }

    @Test
    fun newsRouteDelegatesToFilteredCarrierHubState() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("insuranceCarrierHubItems"))
        assertTrue(navSource.contains("filteredNewsReleases"))
        assertTrue(navSource.contains("setSelectedNewsCarrier"))
        assertTrue(navSource.contains("openInsuranceArticle"))
        assertTrue(navSource.contains("selectedNewsCarrier"))
        assertTrue(viewModelSource.contains("fun insuranceCarrierHubItems"))
        assertTrue(viewModelSource.contains("fun filteredNewsReleases"))
        assertTrue(viewModelSource.contains("fun setSelectedNewsCarrier"))
    }

    @Test
    fun filteredNewsReleasesMatchesCarrierOnFallbackFeed() {
        val viewModel = EobViewModel()
        viewModel.setSelectedNewsCarrier(MajorInsuranceCarrier.Aetna)
        val filtered = viewModel.filteredNewsReleases(
            fallbackNews = listOf(
                NewsRelease(
                    company = "UnitedHealthcare",
                    headline = "UHC update",
                    summary = "United update",
                    date = "06/01/2026"
                ),
                NewsRelease(
                    company = "Aetna",
                    headline = "Aetna update",
                    summary = "Aetna update",
                    date = "06/02/2026"
                )
            )
        )
        assertEquals(1, filtered.size)
        assertEquals("Aetna", filtered.first().company)
    }

    @Test
    fun openCustomTabRejectsUnsafeSchemes() {
        val source = readSource("ui/screens/NewsScreen.kt")
        assertTrue(source.contains("scheme != \"https\""))
        assertTrue(source.contains("scheme != \"http\""))
    }

    @Test
    fun carrierHubItemsExposeAllMajorCarriers() {
        val viewModel = EobViewModel()
        val items = viewModel.insuranceCarrierHubItems()
        assertEquals(MajorInsuranceCarrier.entries.size, items.size)
        assertTrue(items.all { it.monthlyBriefingCount > 0 })
    }

    @Test
    fun filteredNewsReleasesFallsBackWhenNoCarrierMatch() {
        val viewModel = EobViewModel()
        viewModel.setSelectedNewsCarrier(MajorInsuranceCarrier.Medicaid)
        val filtered = viewModel.filteredNewsReleases(
            fallbackNews = listOf(
                NewsRelease(
                    company = "Healthcare Dive",
                    headline = "Industry-wide payer update",
                    summary = "Neutral industry coverage",
                    date = "06/18/2026",
                    articleUrl = "https://example.com/story"
                )
            )
        )
        assertEquals(1, filtered.size)
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
