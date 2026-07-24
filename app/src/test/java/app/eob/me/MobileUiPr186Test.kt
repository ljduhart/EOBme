package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr186Test {
    @Test
    fun expenseAnalyticsAndAccountBentoCardsUseReadableDarkModeTheme() {
        val themeSource = readSource("ui/theme/BentoReadableTheme.kt")
        val dashboardSource = readSource("ui/screens/DashboardScreen.kt")
        val accountSource = readSource("ui/screens/AccountProfileSettingsContent.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")

        assertTrue(themeSource.contains("EobCyberAccent"))
        assertTrue(themeSource.contains("EobCyberSurfaceVariant"))
        assertTrue(dashboardSource.contains("darkModeEnabled: Boolean"))
        assertTrue(dashboardSource.contains("BentoReadableTheme.expenseCardSurface"))
        assertTrue(dashboardSource.contains("BentoReadableTheme.primaryText"))
        assertTrue(accountSource.contains("darkModeEnabled: Boolean"))
        assertTrue(accountSource.contains("BentoReadableTheme.accountCardSurface"))
        assertTrue(navSource.contains("darkModeEnabled = uiState.hubSettings.darkModeEnabled"))
        assertTrue(settingsSource.contains("darkModeEnabled = hubSettings.darkModeEnabled"))
        assertFalse(dashboardSource.contains("containerColor = EobExpenseBentoSurface"))
        assertFalse(accountSource.contains("containerColor = EobBentoCardSurface"))
    }

    @Test
    fun claimAllocationUsesDetailedVerticalBarChart() {
        val dashboardSource = readSource("ui/screens/DashboardScreen.kt")
        assertTrue(dashboardSource.contains("ClaimAllocationDetailedBarChart"))
        assertTrue(dashboardSource.contains("animateFloatAsState"))
        assertTrue(dashboardSource.contains("SummaryBentoCard"))
        assertTrue(dashboardSource.contains("FacilitySpendingCard"))
        assertFalse(dashboardSource.contains("ClaimAllocationBar("))
        assertFalse(dashboardSource.contains("ClaimAllocationPieChart"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr186() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertFalse(pipelineSource.contains("BentoReadableTheme"))
        assertFalse(veryfiSource.contains("ClaimAllocationDetailedBarChart"))
        assertFalse(splashSource.contains("BentoReadableTheme"))
        assertFalse(introSource.contains("expenseCardSurface"))
        assertFalse(viewModelSource.contains("BentoReadableTheme"))
    }

    @Test
    fun androidManifestRetainsBillingAndCriticalPermissions() {
        val manifest = readFile("app/src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android.permission.INTERNET"))
        assertTrue(manifest.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue(manifest.contains("android.permission.CAMERA"))
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.contains("com.android.vending.BILLING"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readFile(path: String): String {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.first { it.isFile }.readText()
    }
}
