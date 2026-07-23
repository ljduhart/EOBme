package app.eob.me

import app.eob.me.data.ExpenseAnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DashboardScreenTest {
    @Test
    fun dashboardUsesSegmentedAllocationBarInsteadOfPieChart() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("ClaimAllocationBar"))
        assertFalse(source.contains("ClaimAllocationPieChart"))
        assertFalse(source.contains("drawArc"))
    }

    @Test
    fun dashboardUsesExpandableFacilityCardsWithAnimations() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("FacilitySpendingCard"))
        assertTrue(source.contains("AnimatedVisibility"))
        assertTrue(source.contains("animateFloatAsState"))
        assertTrue(source.contains("FilterChip"))
    }

    @Test
    fun dashboardUsesStringResourcesForDisplayText() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("stringResource(R.string.expense_analytics_"))
        assertFalse(source.contains("EobStrings.t(language"))
    }

    @Test
    fun titleCaseProviderNameFormatsMixedCaseInput() {
        assertEquals("City Clinic", ExpenseAnalyticsCalculator.titleCaseProviderName("city clinic"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.exists() }
        return file.readText()
    }
}
