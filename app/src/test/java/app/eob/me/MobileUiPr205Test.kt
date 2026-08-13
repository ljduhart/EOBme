package app.eob.me

import android.os.Looper
import app.eob.me.data.EobRecord
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.UserProfile
import app.eob.me.viewmodel.EobViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MobileUiPr205Test {
    @Test
    fun viewRecordsUsesSilentProviderFilterWithoutPrefillingSearchBox() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertFalse(navSource.contains("searchQuery = uiState.historyProviderSearch"))
        assertTrue(navSource.contains("val providerFilter = uiState.historyProviderSearch"))
        assertTrue(navSource.contains("providerFilter = providerFilter"))
        assertTrue(navSource.contains("historyRecordsForDisplay("))
    }

    @Test
    fun historyRecordsForDisplayAppliesSilentProviderFilterWhenSearchIsBlank() {
        val viewModel = EobViewModel()
        viewModel.replaceRecords(
            listOf(
                sampleRecord(1, "Downtown Medical"),
                sampleRecord(2, "Regional Clinic")
            ),
            profile = UserProfile()
        )
        waitForHubRecords(viewModel, expectedCount = 2)

        val filtered = viewModel.historyRecordsForDisplay(
            filter = HistoryBentoFilter.All,
            searchQuery = "",
            providerFilter = "Regional Clinic"
        )

        assertEquals(1, filtered.size)
        assertEquals("Regional Clinic", filtered.first().providerName)
    }

    @Test
    fun typedSearchOverridesSilentProviderFilter() {
        val viewModel = EobViewModel()
        viewModel.replaceRecords(
            listOf(
                sampleRecord(1, "Downtown Medical"),
                sampleRecord(2, "Regional Clinic")
            ),
            profile = UserProfile()
        )
        waitForHubRecords(viewModel, expectedCount = 2)

        val filtered = viewModel.historyRecordsForDisplay(
            filter = HistoryBentoFilter.All,
            searchQuery = "Downtown",
            providerFilter = "Regional Clinic"
        )

        assertEquals(1, filtered.size)
        assertEquals("Downtown Medical", filtered.first().providerName)
    }

    @Test
    fun openProviderRecordHistoryStoresSilentProviderFilter() {
        val viewModel = EobViewModel()
        viewModel.openProviderRecordHistory("Regional Clinic")
        assertEquals("Regional Clinic", viewModel.uiState.value.historyProviderSearch)
        assertEquals(0, viewModel.uiState.value.historyPage)
    }

    @Test
    fun insuranceCardBackIconsBlurForFreeTierOnly() {
        val cardSource = readSource("ui/components/CleanInsuranceCard.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")

        assertTrue(cardSource.contains("insuranceCardBackIconsBlurred"))
        assertTrue(cardSource.contains("InsuranceCardBackIconContent"))
        assertTrue(cardSource.contains("Modifier.blur(16.dp)"))
        assertTrue(homeSource.contains("insuranceCardBackIconsBlurred"))
        assertTrue(
            navSource.contains(
                "insuranceCardBackIconsBlurred = uiState.hubSettings.subscriptionTier == SubscriptionTier.Free"
            )
        )
    }

    @Test
    fun bentoHistoryNavigationClearsSilentProviderFilter() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("HubBentoDestination.EobHistory"))
        assertTrue(navSource.contains("clearHistoryProviderSearch()"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr205() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("insuranceCardBackIconsBlurred"))
        assertFalse(pipelineSource.contains("providerFilter"))
    }

    private fun waitForHubRecords(viewModel: EobViewModel, expectedCount: Int) {
        val deadlineMs = System.currentTimeMillis() + 10_000
        while (viewModel.eobRecords.value.size < expectedCount && System.currentTimeMillis() < deadlineMs) {
            shadowOf(Looper.getMainLooper()).idle()
        }
        assertEquals(expectedCount, viewModel.eobRecords.value.size)
    }

    private fun sampleRecord(id: Int, providerName: String): EobRecord {
        return EobRecord(
            id = id,
            sourceName = "Test",
            providerName = providerName,
            insuranceName = "Test Insurance",
            serviceDate = "01/15/2026",
            serviceDateSortKey = 20260115,
            charges = emptyList(),
            duplicateChargeWarnings = emptyList(),
            rawText = "Test"
        )
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
