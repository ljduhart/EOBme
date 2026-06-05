package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.BentoSnapshotExtractor
import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.PriceTrendDirection
import app.eob.me.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BentoSnapshotExtractorTest {
    @Test
    fun cptSnapshotBuildsTranslatorAndRingProgress() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Clinic
                Aetna
                01/15/2026
                99213 billed ${'$'}180.00 insurance paid ${'$'}90.00
            """.trimIndent(),
            sourceName = "cpt",
            nextId = 1
        )
        val snapshot = BentoSnapshotExtractor.buildCptBentoSnapshot(
            language = AppLanguage.English,
            records = listOf(record),
            selectedCategory = CptCategory.OfficeVisit
        )
        assertTrue(snapshot.translatorLine.contains("99213"))
        assertTrue(snapshot.ringProgress >= 0f)
        assertTrue(snapshot.priceTrendPoints.isNotEmpty())
    }

    @Test
    fun ytdSnapshotUsesProfileDeductibleLimits() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Clinic
                Aetna
                01/15/2026
                99213 billed ${'$'}200.00 insurance paid ${'$'}80.00 deductible ${'$'}40.00 copay ${'$'}10.00
            """.trimIndent(),
            sourceName = "ytd",
            nextId = 1
        )
        val profile = UserProfile(
            firstName = "A",
            lastName = "B",
            email = "a@b.com",
            city = "X",
            state = "Y",
            annualDeductibleLimit = 1500.0,
            annualOutOfPocketMax = 5000.0
        )
        val snapshot = BentoSnapshotExtractor.buildYtdDeductibleBentoSnapshot(
            records = listOf(record),
            profile = profile
        )
        assertEquals(1500.0, snapshot.deductibleLimit, 0.01)
        assertEquals(5000.0, snapshot.outOfPocketMax, 0.01)
        assertTrue(snapshot.deductiblePaidYtd >= 0.0)
        assertEquals(12, snapshot.monthlyBilledNormalized.size)
    }

    @Test
    fun sanitizedPlanLimitsClampInvalidProfileValues() {
        val profile = UserProfile(
            firstName = "A",
            lastName = "B",
            email = "a@b.com",
            city = "X",
            state = "Y",
            annualDeductibleLimit = -500.0,
            annualOutOfPocketMax = 999_999.0
        )
        val safe = profile.sanitizedPlanLimits()
        assertEquals(0.0, safe.annualDeductibleLimit, 0.01)
        assertEquals(200_000.0, safe.annualOutOfPocketMax, 0.01)
    }

    @Test
    fun highBilledCptTrendsAboveFair() {
        val record = EobAnalyzer.analyze(
            rawText = """
                Provider: Clinic
                Aetna
                01/15/2026
                99215 billed ${'$'}450.00 insurance paid ${'$'}120.00
            """.trimIndent(),
            sourceName = "high",
            nextId = 1
        )
        val snapshot = BentoSnapshotExtractor.buildCptBentoSnapshot(
            language = AppLanguage.English,
            records = listOf(record),
            selectedCategory = CptCategory.OfficeVisit
        )
        assertEquals(PriceTrendDirection.AboveFair, snapshot.trendDirection)
    }
}
