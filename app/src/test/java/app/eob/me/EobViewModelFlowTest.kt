package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.CptCategory
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.viewmodel.EobViewModel
import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EobViewModelFlowTest {
    private val profile = UserProfile(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane@example.com",
        city = "Austin",
        state = "TX"
    )

    @Test
    fun setHistoryBentoFilterUpdatesUiState() {
        val viewModel = EobViewModel()
        viewModel.setHistoryBentoFilter(HistoryBentoFilter.Flagged)
        assertEquals(HistoryBentoFilter.Flagged, viewModel.uiState.value.historyBentoFilter)
    }

    @Test
    fun acknowledgeInvoiceFileDropAnimationReturnsToIdle() {
        val viewModel = EobViewModel()
        viewModel.setLoadingInvoice(true)
        viewModel.acknowledgeInvoiceFileDropAnimation()
        assertEquals(InvoiceProcessingPhase.Idle, viewModel.uiState.value.invoiceProcessingPhase)
    }

    @Test
    fun replaceRecordsWhileLoadingTriggersFileDropReveal() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Flow Clinic")
        viewModel.setLoadingInvoice(true)
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        assertEquals(InvoiceProcessingPhase.FileDropReveal, viewModel.uiState.value.invoiceProcessingPhase)
        assertFalse(viewModel.uiState.value.isLoadingInvoice)
        assertEquals(record.id, viewModel.uiState.value.selectedRecord?.id)
        assertTrue(viewModel.uiState.value.appealLetter.isNotBlank())
    }

    @Test
    fun selectRecordUpdatesAppealLetter() {
        val viewModel = EobViewModel()
        val first = sampleRecord(id = 1, provider = "Alpha Clinic")
        val second = sampleRecord(id = 2, provider = "Beta Clinic")
        viewModel.replaceRecords(listOf(first, second), profile)
        waitForHubRecords(viewModel)
        viewModel.selectRecord(second, profile)
        assertEquals(second.id, viewModel.uiState.value.selectedRecord?.id)
        assertTrue(viewModel.uiState.value.appealLetter.contains("Beta Clinic"))
    }

    @Test
    fun deleteRecordSelectsNextRecord() {
        val viewModel = EobViewModel()
        val first = sampleRecord(id = 1, provider = "Alpha Clinic")
        val second = sampleRecord(id = 2, provider = "Beta Clinic")
        viewModel.replaceRecords(listOf(first, second), profile)
        waitForHubRecords(viewModel)
        viewModel.selectRecord(second, profile)
        viewModel.deleteRecord(second, profile)
        assertEquals(1, viewModel.eobRecords.value.size)
        assertEquals(first.id, viewModel.uiState.value.selectedRecord?.id)
    }

    @Test
    fun signInBeforeUploadMessageIsAvailableForUploadFlow() {
        val message = app.eob.me.data.EobStrings.t(AppLanguage.English, "signInBeforeUpload")
        assertTrue(message.contains("sign", ignoreCase = true))
    }

    @Test
    fun setHistoryPageCoercesToValidRange() {
        val viewModel = EobViewModel()
        viewModel.setHistoryPage(99)
        assertTrue(viewModel.uiState.value.historyPage <= 4)
        viewModel.setHistoryPage(-5)
        assertEquals(0, viewModel.uiState.value.historyPage)
    }

    @Test
    fun regenerateAppealUsesSelectedRecord() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        viewModel.updateAppeal("draft")
        viewModel.regenerateAppeal(profile)
        assertTrue(viewModel.uiState.value.appealLetter.contains("Appeal Clinic"))
        assertFalse(viewModel.uiState.value.appealLetter.contains("draft"))
    }

    @Test
    fun visibleNewsHidesDeletedItems() {
        val viewModel = EobViewModel()
        val item = NewsRelease(
            company = "CMS",
            headline = "Test headline",
            summary = "Summary",
            date = "2026-01-01"
        )
        viewModel.deleteNews(item)
        assertTrue(viewModel.visibleNews(listOf(item)).isEmpty())
    }

    @Test
    fun resetHubStateClearsBentoPhaseAndFilter() {
        val viewModel = EobViewModel()
        viewModel.setLoadingInvoice(true)
        viewModel.setHistoryBentoFilter(HistoryBentoFilter.Flagged)
        viewModel.resetHubState()
        assertEquals(InvoiceProcessingPhase.Idle, viewModel.uiState.value.invoiceProcessingPhase)
        assertEquals(HistoryBentoFilter.All, viewModel.uiState.value.historyBentoFilter)
        assertTrue(viewModel.eobRecords.value.isEmpty())
    }

    @Test
    fun historyBentoSnapshotReflectsRecords() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Snapshot Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        assertEquals(3, viewModel.historyBentoSnapshot().monthlySpend.size)
    }

    @Test
    fun applyRemoteRecordsDocumentsFileDropRevealPhase() {
        val source = resolveViewModelSource()
        assertTrue(source.contains("InvoiceProcessingPhase.FileDropReveal"))
        assertTrue(source.contains("acknowledgeInvoiceFileDropAnimation"))
    }

    @Test
    fun dashboardAndCptSummariesRouteThroughViewModel() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Flow Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)

        val metrics = viewModel.dashboardFinancialMetrics()
        assertTrue(metrics.grossBilled > 0.0)

        val providers = viewModel.dashboardProviderBreakdown(AppLanguage.English)
        assertEquals(1, providers.size)

        val cptRows = viewModel.cptSummaryRows(AppLanguage.English, CptCategory.OfficeVisit)
        assertTrue(cptRows.isNotEmpty())
    }

    private fun waitForHubRecords(viewModel: EobViewModel) {
        var attempts = 0
        while (viewModel.eobRecords.value.isEmpty() && attempts < 200) {
            shadowOf(Looper.getMainLooper()).idle()
            attempts++
        }
        assertTrue("Timed out waiting for hub EOB records", viewModel.eobRecords.value.isNotEmpty())
    }

    private fun idleMainLooper(times: Int) {
        repeat(times) { shadowOf(Looper.getMainLooper()).idle() }
    }

    private fun sampleRecord(id: Int, provider: String) = EobAnalyzer.analyze(
        rawText = """
            Provider: $provider
            Aetna
            01/15/2026
            99213 billed ${'$'}100.00 insurance paid ${'$'}40.00
        """.trimIndent(),
        sourceName = "test-$id",
        nextId = id
    )

    private fun resolveViewModelSource(): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/viewmodel/EobViewModel.kt"),
            java.io.File("app/src/main/java/app/eob/me/viewmodel/EobViewModel.kt")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
