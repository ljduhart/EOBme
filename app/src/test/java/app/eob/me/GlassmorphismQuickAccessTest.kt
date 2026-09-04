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
        assertTrue(source.contains("supportsGlassBlurEffects"))
        assertTrue(source.contains("VERSION_CODES.S"))
        assertTrue(source.contains("optionalBlur"))
        assertFalse(source.contains("BlendMode.Screen"))
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
        assertFalse(source.contains("!!"))
    }

    @Test
    fun quickAccessPreservesNavHostOverlayWiring() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(cardSource.contains("GlassmorphismQuickAccessHub"))
        assertTrue(cardSource.contains("modifier = Modifier.fillMaxWidth()"))
        assertTrue(navSource.contains("blockInsuranceCardBackNavigation = smartRxVaultVisible ||"))
        assertTrue(navSource.contains("medicalDictionaryVisible"))
        assertTrue(navSource.contains("onOpenMedicalDictionary = {"))
        assertTrue(navSource.contains("onOpenClinicalNotes = {"))
        assertTrue(navSource.contains("onOpenSmartRxVault = {"))
        assertTrue(navSource.contains("onOpenReverseDxLookup = {"))
        assertFalse(navSource.substringAfter("onOpenMedicalDictionary = {")
            .substringBefore("blockInsuranceCardBackNavigation")
            .contains("!!"))
        assertTrue(viewModelSource.contains("fun clearMedicalDictSession"))
    }

    @Test
    fun quickAccessUsesTranslucentFallbackWithoutBlurOnLegacyApi() {
        val source = readSource("ui/components/quickaccess/GlassmorphismQuickAccess.kt")
        assertTrue(source.contains("GlassPaneFallbackFill"))
        assertTrue(source.contains("Color.White.copy(alpha = 0.10f)"))
        assertTrue(source.contains("if (blurSupported) GlassPaneFill else GlassPaneFallbackFill"))
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
