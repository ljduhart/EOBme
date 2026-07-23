package app.eob.me

import app.eob.me.data.InsuranceBriefingAssets
import app.eob.me.data.MajorInsuranceCarrier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileUiPr165Test {
    @Test
    fun insuranceBriefingAssetsMapAllFiveCarriers() {
        MajorInsuranceCarrier.entries.forEach { carrier ->
            assertTrue(
                "Missing briefing logo for $carrier",
                InsuranceBriefingAssets.logoResId(carrier) != 0
            )
        }
        assertEquals(MajorInsuranceCarrier.Humana, MajorInsuranceCarrier.entries.first { it.name == "Humana" })
    }

    @Test
    fun careTeamSmartCardsDropLongPressHintRow() {
        val source = readSource("ui/components/home/HomeCareTeamCards.kt")
        assertFalse(source.contains("careTeamLongPressEdit"))
        assertFalse(source.contains("careTeamTapToFlip"))
        assertTrue(source.contains("CareTeamProviderIcon"))
        assertTrue(source.contains("tint = CardInk.copy(alpha = 0.14f)"))
        assertTrue(source.contains("metricsLine.isNotBlank()"))
    }

    @Test
    fun logoutConfirmationDialogIsWiredInProfileAndSettings() {
        val profileSource = readSource("ui/screens/ProfileScreen.kt")
        val settingsSource = readSource("ui/screens/AccountProfileSettingsContent.kt")
        val dialogSource = readSource("ui/components/LogoutConfirmDialog.kt")
        listOf(
            "LogoutConfirmDialog",
            "showLogoutConfirm"
        ).forEach { snippet ->
            assertTrue("$snippet missing from logout flow", profileSource.contains(snippet))
            assertTrue("$snippet missing from settings logout flow", settingsSource.contains(snippet))
        }
        listOf(
            "logoutConfirmTitle",
            "logoutConfirmMessage",
            "logoutConfirmYes",
            "logoutConfirmNo"
        ).forEach { snippet ->
            assertTrue("$snippet missing from dialog", dialogSource.contains(snippet))
        }
    }

    @Test
    fun insuranceNewsBentoUsesReadableDarkTextWithoutExternalLogos() {
        val bentoSource = readSource("ui/components/bento/InsuranceNewsBentoCell.kt")
        val newsSource = readSource("ui/screens/NewsScreen.kt")
        assertFalse(bentoSource.contains("InsuranceBriefingLogoStrip"))
        assertFalse(bentoSource.contains("InsuranceBriefingAssets.logoResId"))
        assertTrue(bentoSource.contains("MaterialTheme.colorScheme.onSurface"))
        assertTrue(newsSource.contains("insuranceNewsReadableTextColor"))
        assertTrue(newsSource.contains("InsuranceNewsDarkModeText"))
        assertTrue(newsSource.contains("isHubDarkPresentation"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
