package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr184Test {
    @Test
    fun accountProfileSettingsUsesYellowLightbulbForHelpfulHints() {
        val contentSource = readSource("ui/screens/AccountProfileSettingsContent.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        assertTrue(contentSource.contains("HubHelpfulHintsIcon.Lightbulb"))
        assertTrue(contentSource.contains("accountProfileHelpfulHintsCd"))
        assertFalse(contentSource.contains("HubSettingsGearIcon"))
        assertTrue(settingsSource.contains("showHelpfulHintsDialog"))
        assertTrue(settingsSource.contains("settingsHelpfulHint1"))
    }

    @Test
    fun accountProfileAndDashboardUseEobStringsInsteadOfStringResources() {
        val accountSource = readSource("ui/screens/AccountProfileSettingsContent.kt")
        val dashboardSource = readSource("ui/screens/DashboardScreen.kt")
        val stringsXml = readFile("app/src/main/res/values/strings.xml")
        assertTrue(accountSource.contains("EobStrings.t(language"))
        assertFalse(accountSource.contains("stringResource(R.string.account_profile_"))
        assertTrue(dashboardSource.contains("language: AppLanguage"))
        assertTrue(dashboardSource.contains("EobStrings.t(language"))
        assertFalse(dashboardSource.contains("stringResource(R.string.expense_analytics_"))
        assertFalse(stringsXml.contains("account_profile_"))
        assertFalse(stringsXml.contains("expense_analytics_"))
    }

    @Test
    fun manageSubscriptionBillingTabsUseLocalizedKeys() {
        val source = readSource("ui/screens/ManageSubscriptionScreen.kt")
        assertTrue(source.contains("EobStrings.t(language, \"billingIntervalMonthly\")"))
        assertTrue(source.contains("EobStrings.t(language, \"billingIntervalAnnual\")"))
        assertFalse(source.contains("Text(\"Monthly\")"))
        assertFalse(source.contains("Text(\"Annual (Save up to 25%)\")"))
    }

    @Test
    fun helpfulHintsAndAccountProfileKeysResolveForEveryLanguage() {
        val keys = listOf(
            "settingsHelpfulHintsTitle",
            "settingsHelpfulHintsClose",
            "settingsHelpfulHint1",
            "settingsHelpfulHint5",
            "settingsHelpfulHint10",
            "accountProfileSectionTitle",
            "accountProfileHelpfulHintsCd",
            "accountProfileSaveProfile",
            "settingsManageSubscription",
            "billingManageSubscriptionHint",
            "expenseAnalytics",
            "expenseAnalyticsFacilitiesTitle",
            "expenseAnalyticsEmpty",
            "expenseAnalyticsStatusAudited"
        )
        AppLanguage.entries.forEach { language ->
            keys.forEach { key ->
                val value = EobStrings.t(language, key)
                assertTrue("$key missing for $language", value.isNotBlank())
                assertNotEquals(key, value)
            }
        }
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr184() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("HubHelpfulHintsIcon"))
        assertFalse(veryfiSource.contains("accountProfileHelpfulHintsCd"))
        assertFalse(splashSource.contains("settingsHelpfulHint1"))
        assertFalse(introSource.contains("expenseAnalyticsFacilitiesTitle"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readFile(path: String): String {
        val candidates = listOf(File(path), File("../$path"), File("app/$path"))
        return candidates.first { it.isFile }.readText()
    }
}
