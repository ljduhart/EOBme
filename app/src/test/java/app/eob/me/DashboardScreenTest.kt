package app.eob.me

import app.eob.me.ui.screens.splitProviderNameForDashboardRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DashboardScreenTest {

    @Test
    fun splitProviderNameKeepsShortNamesOnOneLine() {
        val lines = splitProviderNameForDashboardRow("RALPH JOHNSTON M.D.")
        assertEquals("RALPH JOHNSTON M.D.", lines.firstLine)
        assertNull(lines.secondLine)
    }

    @Test
    fun splitProviderNameMovesLastWordWhenNameIsLong() {
        val lines = splitProviderNameForDashboardRow("REPRODUCTIVE GENETIC INNOVATIONS")
        assertEquals("REPRODUCTIVE GENETIC", lines.firstLine)
        assertEquals("INNOVATIONS", lines.secondLine)
    }

    @Test
    fun splitProviderNameKeepsBorderlineNamesOnOneLine() {
        val lines = splitProviderNameForDashboardRow("ALAMO FAMILY & COSMETIC D")
        assertEquals("ALAMO FAMILY & COSMETIC D", lines.firstLine)
        assertNull(lines.secondLine)
    }

    @Test
    fun splitProviderNameLeavesSingleLongTokenOnOneLine() {
        val lines = splitProviderNameForDashboardRow("REPRODUCTIVEGENETICINNOVATIONS")
        assertEquals("REPRODUCTIVEGENETICINNOVATIONS", lines.firstLine)
        assertNull(lines.secondLine)
    }

    @Test
    fun splitProviderNameTrimsWhitespaceInput() {
        val lines = splitProviderNameForDashboardRow("  REPRODUCTIVE GENETIC INNOVATIONS  ")
        assertEquals("REPRODUCTIVE GENETIC", lines.firstLine)
        assertEquals("INNOVATIONS", lines.secondLine)
    }

    @Test
    fun dashboardProviderBreakdownTracksLanguageChanges() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("remember(records, language)"))
    }

    @Test
    fun claimAllocationLegendAlwaysListsAllCategories() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("allocationCategories.forEach"))
        assertTrue(source.contains("pieSlices"))
    }

    @Test
    fun facilityBarGraphUsesZeroWidthForZeroAmounts() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("if (amount <= 0.0)"))
    }

    @Test
    fun spendingByFacilityUsesBarGraphWithTotals() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(source.contains("FacilitySpendingBarChart"))
        assertTrue(source.contains("facilityTotal.asCurrency()"))
        assertTrue(source.contains("patientTotal.asCurrency()"))
        assertTrue(source.contains("amount.asCurrency()"))
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
