package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr168Test {
    @Test
    fun eobHistoryHeaderUsesCompactWeightSplit() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(historySource.contains(".weight(0.16f)"))
        assertTrue(historySource.contains(".weight(0.84f)"))
        assertTrue(historySource.contains("FilledTonalButton"))
        assertFalse(historySource.contains("ExtendedFloatingActionButton"))
        assertTrue(navSource.contains("fillMaxHeight(0.10f)"))
    }

    @Test
    fun carrierCardShowsLogoOnlyWithBriefingLabel() {
        val source = readSource("ui/screens/NewsScreen.kt")
        val carrierBlock = source.substringAfter("fun CarrierCard")
            .substringBefore("@Composable\nfun NewsBriefingCard")
        assertTrue(carrierBlock.contains("InsuranceBriefingAssets.logoResId"))
        assertTrue(carrierBlock.contains("insuranceNewsMonthlyBriefingsLabel"))
        assertTrue(carrierBlock.contains("Arrangement.SpaceBetween"))
        assertTrue(carrierBlock.contains(".weight(1f)"))
        assertFalse(carrierBlock.contains("hubShortName"))
    }

    @Test
    fun insuranceBriefingLogoAssetsExistForAllCarriers() {
        val assetsSource = readSource("data/InsuranceBriefingAssets.kt")
        listOf(
            "briefing_logo_uhc",
            "briefing_logo_medicare",
            "briefing_logo_aetna",
            "briefing_logo_bcbs",
            "briefing_logo_humana"
        ).forEach { asset ->
            assertTrue("Missing asset mapping: $asset", assetsSource.contains(asset))
            val png = briefingDrawable(asset)
            assertTrue("Missing drawable file: $asset.png", png.isFile && png.length() > 0)
        }
    }

    private fun briefingDrawable(name: String): java.io.File {
        val candidates = listOf(
            java.io.File("src/main/res/drawable/$name.png"),
            java.io.File("app/src/main/res/drawable/$name.png")
        )
        return candidates.first { it.isFile }
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
