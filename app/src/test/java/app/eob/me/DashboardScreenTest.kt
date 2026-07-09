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
    fun spendingByFacilityRowReservesHorizontalAmountSpace() {
        val source = readSource("ui/screens/DashboardScreen.kt")
        val rowStart = source.indexOf("items(providerBreakdown")
        val rowEnd = source.indexOf("LinearProgressIndicator", rowStart)
        val rowBlock = source.substring(rowStart, rowEnd)
        assertTrue(rowBlock.contains("splitProviderNameForDashboardRow"))
        assertTrue(rowBlock.contains("widthIn(min = 72.dp)"))
        assertTrue(rowBlock.contains("softWrap = false"))
        assertTrue(rowBlock.contains("maxLines = 1"))
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
