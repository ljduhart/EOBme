package app.eob.me

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GlassmorphismQuickAccessTest {
    @Test
    fun quickAccessUsesProceduralCanvasIconsWithoutExternalAssets() {
        val source = readSource("ui/components/quickaccess/GlassmorphismQuickAccess.kt")
        assertTrue(source.contains("GlassmorphicQuickAccessPane"))
        assertTrue(source.contains("CanvasMedsIcon"))
        assertTrue(source.contains("CanvasNotepadIcon"))
        assertTrue(source.contains("CanvasDxCptIcon"))
        assertTrue(source.contains("CanvasDictionaryIcon"))
        assertTrue(source.contains("Modifier.blur"))
        assertTrue(source.contains("BlendMode"))
        assertFalse(source.contains("AsyncImage"))
        assertFalse(source.contains("coil"))
        assertFalse(source.contains("painterResource"))
    }

    @Test
    fun cleanInsuranceCardDelegatesQuickAccessToGlassHub() {
        val source = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(source.contains("GlassmorphismQuickAccessHub"))
        assertTrue(source.contains("onOpenMedicalDictionary"))
        assertTrue(source.contains("onOpenClinicalNotes"))
        assertTrue(source.contains("onOpenSmartRxVault"))
        assertTrue(source.contains("onOpenReverseDxLookup"))
    }

    @Test
    fun quickAccessPreservesNavHostOverlayWiring() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(cardSource.contains("GlassmorphismQuickAccessHub"))
        assertTrue(cardSource.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(navSource.contains("blockInsuranceCardBackNavigation = smartRxVaultVisible ||"))
        assertTrue(navSource.contains("medicalDictionaryVisible"))
        assertTrue(navSource.contains("onOpenMedicalDictionary = {"))
        assertTrue(navSource.contains("onOpenClinicalNotes = {"))
        assertTrue(navSource.contains("onOpenSmartRxVault = {"))
        assertTrue(navSource.contains("onOpenReverseDxLookup = {"))
    }

    @Test
    fun quickAccessGridDefinesFourTiles() {
        val source = readSource("ui/components/quickaccess/GlassmorphismQuickAccess.kt")
        assertEquals(4, source.split("icon = { Canvas").size - 1)
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
