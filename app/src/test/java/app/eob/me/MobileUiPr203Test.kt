package app.eob.me

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MobileUiPr203Test {
    @Test
    fun dxAssetContainsThirtyFiveCodesIncludingPrimaryCareAppend() {
        val jsonText = RuntimeEnvironment.getApplication().assets
            .open("dx_cpt_map.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(jsonText)
        assertEquals(35, root.length())
        assertTrue(root.has("R55"))
        assertTrue(root.has("E78.5"))
        assertTrue(root.has("Z01.419"))
    }

    @Test
    fun dxReverseLookupIconMatchesActiveProcessingMockup() {
        val iconSource = readSource("ui/components/InsuranceCardBackIcons.kt")
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        assertTrue(iconSource.contains("InsuranceCardDxReverseLookupIcon"))
        assertTrue(iconSource.contains("drawNeonGear"))
        assertTrue(iconSource.contains("[9|9|2|1|4]"))
        assertTrue(iconSource.contains("insuranceCardReverseDxProcessingCaption"))
        assertTrue(iconSource.contains("DxNeonCyan"))
        assertTrue(cardSource.contains("InsuranceCardDxReverseLookupIcon(language = language)"))
    }

    @Test
    fun reverseDxNavigationBackAndSheetExclusionUnchangedAfterPr203() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("BackHandler(enabled = reverseDxLookupVisible)"))
        assertTrue(navSource.contains("onOpenReverseDxLookup = {"))
        assertTrue(navSource.contains("launchEobScannerFromHub()"))
        assertTrue(navSource.contains("blockInsuranceCardBackNavigation = smartRxVaultVisible ||"))
        assertTrue(navSource.contains("reverseDxLookupVisible"))
        assertTrue(navSource.contains("ReverseDxLookupSessionOverlay"))
    }

    @Test
    fun appendedPrimaryCareDepressionCodeUsesThresholdPath() {
        val jsonText = RuntimeEnvironment.getApplication().assets
            .open("dx_cpt_map.json")
            .bufferedReader()
            .use { it.readText() }
        val matches = JSONObject(jsonText).getJSONObject("F32.A").getInt("totalPotentialMatches")
        assertTrue(matches >= 50)
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
