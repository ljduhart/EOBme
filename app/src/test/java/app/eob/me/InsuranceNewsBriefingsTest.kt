package app.eob.me

import app.eob.me.data.EobInsuranceNews
import app.eob.me.data.MajorInsuranceCarrier
import app.eob.me.data.NewsRelease
import app.eob.me.network.InsuranceNewsRotation
import app.eob.me.network.RssNewsMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InsuranceNewsBriefingsTest {
    @Test
    fun articlesForYearIncludesOnlyMonthsThroughCurrentMonth() {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val throughMonth = Calendar.JULY
        val articles = EobInsuranceNews.articlesForYear(year = year, throughMonth = throughMonth)

        val expectedMonths = (Calendar.JANUARY..throughMonth).toSet()
        assertEquals(MajorInsuranceCarrier.entries.size * expectedMonths.size, articles.size)
        assertTrue(articles.all { it.monthIndex in expectedMonths })
        assertTrue(articles.none { it.monthIndex > throughMonth })
    }

    @Test
    fun articlesForYearReturnsEmptyForNonCurrentYear() {
        val wrongYear = Calendar.getInstance().get(Calendar.YEAR) - 1
        assertTrue(EobInsuranceNews.articlesForYear(year = wrongYear).isEmpty())
    }
}

class InsuranceNewsRotationTest {
    private fun release(company: String, headline: String, day: Int): NewsRelease {
        return NewsRelease(
            company = company,
            headline = headline,
            summary = "Summary",
            date = "06/${day.toString().padStart(2, '0')}/2026"
        )
    }

    @Test
    fun combineRotatedIntelligenceKeepsThreeBeckersAndFourHealthcareDiveStories() {
        val beckersPool = (1..9).map { index ->
            release(RssNewsMapper.BECKERS_COMPANY, "Becker $index", index)
        }
        val divePool = (1..10).map { index ->
            release(RssNewsMapper.HEALTHCARE_DIVE_COMPANY, "Dive $index", index)
        }

        val rotated = InsuranceNewsRotation.combineRotatedIntelligence(
            beckersPool = beckersPool,
            healthcareDivePool = divePool,
            slot = 0L
        )

        assertEquals(7, rotated.size)
        assertEquals(3, rotated.count { it.company == RssNewsMapper.BECKERS_COMPANY })
        assertEquals(4, rotated.count { it.company == RssNewsMapper.HEALTHCARE_DIVE_COMPANY })
    }

    @Test
    fun rotatedWindowAdvancesWithSlot() {
        val pool = (1..6).map { index ->
            release(RssNewsMapper.BECKERS_COMPANY, "Story $index", index)
        }

        val firstWindow = InsuranceNewsRotation.rotatedWindow(pool, visibleCount = 3, slot = 0L)
        val secondWindow = InsuranceNewsRotation.rotatedWindow(pool, visibleCount = 3, slot = 1L)

        assertEquals(3, firstWindow.size)
        assertEquals(3, secondWindow.size)
        assertTrue(firstWindow.map { it.headline }.toSet() != secondWindow.map { it.headline }.toSet())
    }

    @Test
    fun rotationSlotChangesEveryTwoHours() {
        val period = InsuranceNewsRotation.ROTATION_PERIOD_MS
        val slotAtStart = InsuranceNewsRotation.rotationSlot(0L)
        val slotAfterOnePeriod = InsuranceNewsRotation.rotationSlot(period)
        assertEquals(0L, slotAtStart)
        assertEquals(1L, slotAfterOnePeriod)
        assertTrue(InsuranceNewsRotation.millisUntilNextRotation(period - 1L) > 0L)
    }
}
