package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr183Test {
    @Test
    fun expenseAnalyticsStateIsExposedFromEobViewModel() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("val expenseAnalyticsState: StateFlow<ExpenseAnalyticsState>"))
        assertTrue(viewModelSource.contains("fun setExpenseAnalyticsSort"))
        assertTrue(viewModelSource.contains("fun toggleExpenseAnalyticsFacilityExpanded"))
        assertTrue(viewModelSource.contains("fun markExpenseAnalyticsClaimAppealed"))
        assertTrue(viewModelSource.contains("fun recordForExpenseAnalyticsClaim"))
    }

    @Test
    fun dashboardUsesModernExpenseAnalyticsLayout() {
        val dashboardSource = readSource("ui/screens/DashboardScreen.kt")
        val stringsSource = readSource("data/EobStrings.kt")
        assertTrue(dashboardSource.contains("CenterAlignedTopAppBar"))
        assertTrue(dashboardSource.contains("ClaimAllocationBar"))
        assertTrue(dashboardSource.contains("SummaryBentoCard"))
        assertTrue(dashboardSource.contains("ExpenseAnalyticsSortChips"))
        assertTrue(dashboardSource.contains("FacilitySpendingCard"))
        assertTrue(dashboardSource.contains("AnimatedVisibility"))
        assertTrue(dashboardSource.contains("animateFloatAsState"))
        assertTrue(dashboardSource.contains("EobExpenseCarrierCovered"))
        assertTrue(dashboardSource.contains("EobExpensePatientResponsibility"))
        assertTrue(dashboardSource.contains("EobExpenseNetworkSavings"))
        assertFalse(dashboardSource.contains("HolographicGlassCard"))
        assertFalse(dashboardSource.contains("ClaimAllocationPieChart"))
        assertTrue(stringsSource.contains("\"expenseAnalyticsSortHighestPatientShare\""))
        assertFalse(dashboardSource.contains("App Store"))
        assertFalse(dashboardSource.contains("iOS"))
    }

    @Test
    fun expenseAnalyticsModelsDefineFacilityAndClaimStructures() {
        val modelsSource = readSource("data/ExpenseAnalyticsModels.kt")
        assertTrue(modelsSource.contains("data class FacilitySpending"))
        assertTrue(modelsSource.contains("data class MedicalClaim"))
        assertTrue(modelsSource.contains("sealed interface ClaimStatus"))
        assertTrue(modelsSource.contains("AuditedCorrect"))
        assertTrue(modelsSource.contains("PotentialError"))
        assertTrue(modelsSource.contains("Appealed"))
    }

    @Test
    fun dashboardRouteHidesHubHeaderAndWiresCallbacks() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("currentRoute != EobRoute.Dashboard.route"))
        assertTrue(navSource.contains("expenseAnalyticsState"))
        assertTrue(navSource.contains("onInspectClaimSource"))
        assertTrue(navSource.contains("onAppealClaim"))
        assertTrue(navSource.contains("setExpenseAnalyticsSort"))
    }

    @Test
    fun protectedPipelineAndOpeningScreensRemainUntouchedForPr183() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val veryfiSource = readSource("network/VeryfiDocumentClient.kt")
        val splashSource = readSource("ui/screens/SplashScreen.kt")
        val introSource = readSource("ui/screens/IntroScreen.kt")
        assertFalse(pipelineSource.contains("ExpenseAnalyticsState"))
        assertFalse(veryfiSource.contains("FacilitySpendingCard"))
        assertFalse(splashSource.contains("expense_analytics_page_title"))
        assertFalse(introSource.contains("ClaimAllocationBar"))
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
