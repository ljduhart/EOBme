package app.eob.me

import app.eob.me.data.CptCategory
import app.eob.me.data.NewsRelease
import app.eob.me.viewmodel.rankNewsReleases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalizedNewsFeedTest {
    @Test
    fun newsReleaseDefaultsSupportRankingFields() {
        val release = NewsRelease(
            company = "CMS",
            headline = "Test",
            summary = "Summary",
            date = "2026"
        )
        assertEquals(emptyList<String>(), release.targetTags)
        assertEquals(1, release.baseRelevance)
    }

    @Test
    fun rankNewsReleasesPrefersHigherContextOverlap() {
        val local = NewsRelease(
            company = "Aetna",
            headline = "Local update",
            summary = "Summary",
            date = "2026",
            targetTags = listOf("Texas", "OfficeVisit"),
            baseRelevance = 1
        )
        val national = NewsRelease(
            company = "CMS",
            headline = "National update",
            summary = "Summary",
            date = "2026",
            targetTags = listOf("National"),
            baseRelevance = 1
        )
        val ranked = rankNewsReleases(
            releases = listOf(national, local),
            contextTags = setOf("Austin", "Texas", CptCategory.OfficeVisit.name)
        )
        assertEquals(local.headline, ranked.first().headline)
        assertEquals(national.headline, ranked.last().headline)
    }

    @Test
    fun rankNewsReleasesFiltersZeroDynamicScore() {
        val filtered = NewsRelease(
            company = "Humana",
            headline = "Hidden",
            summary = "Summary",
            date = "2026",
            targetTags = listOf("California"),
            baseRelevance = 0
        )
        val ranked = rankNewsReleases(
            releases = listOf(filtered),
            contextTags = setOf("Texas", CptCategory.Lab.name)
        )
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun repositoryContractDeclaresRegionalNewsObserver() {
        val source = readSource("data/repository/EobRepository.kt")
        assertTrue(source.contains("fun observeRegionalNews(userState: String): Flow<List<NewsRelease>>"))
    }

    @Test
    fun firestoreRegionalNewsUsesNewsReleasesCollectionAndTagQuery() {
        val source = readSource("data/FirebaseEobRepository.kt")
        assertTrue(source.contains("NEWS_RELEASES = \"news_releases\""))
        assertTrue(source.contains("whereArrayContainsAny(\"targetTags\", listOf(userState, \"National\"))"))
    }

    @Test
    fun eobViewModelExposesPersonalizedNewsFeedStateFlow() {
        val source = readSource("viewmodel/EobViewModel.kt")
        assertTrue(source.contains("val personalizedNewsFeed: StateFlow<List<NewsRelease>>"))
        assertTrue(source.contains("private val userContextTags: Flow<Set<String>>"))
        assertTrue(source.contains("rankNewsReleases(releases, contextTags)"))
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
