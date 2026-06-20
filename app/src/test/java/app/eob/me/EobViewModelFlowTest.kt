package app.eob.me

import app.eob.me.data.AppLanguage
import app.eob.me.data.AppealTarget
import app.eob.me.data.DoctorDisputeStrategy
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobStrings
import app.eob.me.data.AppLockTimeout
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.ImageCompressionLevel
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.SettingsTab
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
    fun insuranceCardDisplayAndEditsMapProfileCredentials() {
        val viewModel = EobViewModel()
        val seededProfile = profile.copy(
            insuranceName = "Blue Shield of California",
            insuranceId = "ABC123456789",
            groupName = "98765",
            pcpCopay = "20",
            specialistCopay = "40"
        )
        val display = viewModel.insuranceCardDisplay(seededProfile, AppLanguage.English)
        assertEquals("Blue Shield of California", display.insuranceName)
        assertEquals("ABC123456789", display.memberId)
        assertEquals("98765", display.groupNumber)
        assertEquals("20", display.pcpCopay)
        assertEquals("40", display.specialistCopay)
        assertEquals("Austin, TX", display.footerLocation)

        val updated = viewModel.applyInsuranceCardEdits(
            profile = seededProfile,
            insuranceName = " Aetna ",
            memberId = " MEM999 ",
            groupNumber = " GRP1 ",
            pcpCopay = "25",
            specialistCopay = "50"
        )
        assertEquals("Aetna", updated.insuranceName)
        assertEquals("MEM999", updated.insuranceId)
        assertEquals("GRP1", updated.groupName)
        assertEquals("25", updated.pcpCopay)
        assertEquals("50", updated.specialistCopay)
    }

    @Test
    fun setHistoryBentoFilterUpdatesUiState() {
        val viewModel = EobViewModel()
        viewModel.setHistoryBentoFilter(HistoryBentoFilter.Flagged)
        assertEquals(HistoryBentoFilter.Flagged, viewModel.uiState.value.historyBentoFilter)
    }

    @Test
    fun activateAppealGeneratorBentoSetsProcessingAndRegeneratesLetter() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        viewModel.activateAppealGeneratorBento(profile)
        assertTrue(viewModel.uiState.value.appealGeneratorBentoProcessing)
        assertTrue(viewModel.uiState.value.appealLetter.contains("Appeal Clinic"))
    }

    @Test
    fun acknowledgeAppealGeneratorBentoActivationClearsProcessing() {
        val viewModel = EobViewModel()
        viewModel.activateAppealGeneratorBento(profile)
        viewModel.acknowledgeAppealGeneratorBentoActivation()
        assertFalse(viewModel.uiState.value.appealGeneratorBentoProcessing)
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
    fun updateAppealIgnoredUntilEditEnabled() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        val generated = viewModel.uiState.value.appealLetter
        viewModel.updateAppeal("user draft")
        assertEquals(generated, viewModel.uiState.value.appealLetter)
        viewModel.enableAppealLetterEditing()
        viewModel.updateAppeal("user draft")
        assertEquals("user draft", viewModel.uiState.value.appealLetter)
    }

    @Test
    fun saveAppealLetterDisablesEditingAndRegenerateResetsEditing() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        viewModel.enableAppealLetterEditing()
        assertTrue(viewModel.uiState.value.appealLetterEditingEnabled)
        viewModel.saveAppealLetter()
        assertFalse(viewModel.uiState.value.appealLetterEditingEnabled)
        viewModel.enableAppealLetterEditing()
        viewModel.regenerateAppeal(profile)
        assertFalse(viewModel.uiState.value.appealLetterEditingEnabled)
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
    fun insuranceNewsBentoSnapshotReflectsViewModelNewsAndRecords() {
        val viewModel = EobViewModel()
        val snapshot = viewModel.insuranceNewsBentoSnapshot(AppLanguage.English)
        assertTrue(snapshot.tickerHeadlines.isNotEmpty())
        assertTrue(snapshot.previewHeadline.isNotBlank())
    }

    @Test
    fun deleteNewsBumpsNewsFeedRevision() {
        val viewModel = EobViewModel()
        val revisionBefore = viewModel.uiState.value.newsFeedRevision
        val item = NewsRelease(
            company = "Aetna",
            headline = "Test headline",
            summary = "Summary",
            date = "2026-01-01"
        )
        viewModel.deleteNews(item)
        assertEquals(revisionBefore + 1, viewModel.uiState.value.newsFeedRevision)
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
    fun saveAppPinReturnsBlankNoticeOnSuccess() {
        val viewModel = EobViewModel()
        val message = viewModel.saveAppPin("12345", "12345", AppLanguage.English)
        assertEquals("", message)
    }

    @Test
    fun saveAppPinReturnsValidationMessageOnMismatch() {
        val viewModel = EobViewModel()
        val message = viewModel.saveAppPin("12345", "54321", AppLanguage.English)
        assertEquals(EobStrings.t(AppLanguage.English, "settingsPinMismatch"), message)
    }

    @Test
    fun onAppealTargetSwitchedRegeneratesDoctorLetter() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        viewModel.onAppealTargetSwitched(AppealTarget.DOCTOR)
        assertEquals(AppealTarget.DOCTOR, viewModel.uiState.value.selectedAppealTarget)
        assertTrue(viewModel.uiState.value.appealLetter.contains("Appeal Clinic"))
        assertTrue(viewModel.uiState.value.appealLetter.contains("itemized billing statement"))
    }

    @Test
    fun onDisputeStrategySwitchedUpdatesDoctorAppealCopy() {
        val viewModel = EobViewModel()
        val record = sampleRecord(id = 1, provider = "Appeal Clinic")
        viewModel.replaceRecords(listOf(record), profile)
        waitForHubRecords(viewModel)
        viewModel.onAppealTargetSwitched(AppealTarget.DOCTOR)
        viewModel.onDisputeStrategySwitched(DoctorDisputeStrategy.FINANCIAL_HARDSHIP)
        assertEquals(DoctorDisputeStrategy.FINANCIAL_HARDSHIP, viewModel.uiState.value.selectedDisputeStrategy)
        assertTrue(viewModel.uiState.value.appealLetter.contains("financial hardship adjustment"))
    }

    @Test
    fun hubSettingsMutationsUpdateUiState() {
        val viewModel = EobViewModel()
        viewModel.setSettingsTab(SettingsTab.Security)
        assertEquals(SettingsTab.Security, viewModel.uiState.value.hubSettings.selectedTab)
        viewModel.setPinLockEnabled(true)
        assertTrue(viewModel.uiState.value.hubSettings.pinLockEnabled)
        viewModel.setAppLockTimeout(AppLockTimeout.FifteenMinutes)
        assertEquals(AppLockTimeout.FifteenMinutes, viewModel.uiState.value.hubSettings.appLockTimeout)
        viewModel.setUploadOverWifiOnly(true)
        assertTrue(viewModel.uiState.value.hubSettings.uploadOverWifiOnly)
        viewModel.setImageCompressionLevel(ImageCompressionLevel.High)
        assertEquals(ImageCompressionLevel.High, viewModel.imageCompressionLevel())
        viewModel.setAutoCropEnabled(false)
        assertFalse(viewModel.autoCropEnabled())
        viewModel.setDarkModeEnabled(true)
        assertTrue(viewModel.uiState.value.hubSettings.darkModeEnabled)
        viewModel.setDarkModeEnabled(false)
        assertFalse(viewModel.uiState.value.hubSettings.darkModeEnabled)
    }

    @Test
    fun appLockRequiresUnlockAfterBackgroundTimeout() {
        val viewModel = EobViewModel()
        viewModel.setPinLockEnabled(true)
        viewModel.setAppLockTimeout(AppLockTimeout.FiveMinutes)
        viewModel.onAppBackgrounded()
        val backgroundField = EobViewModel::class.java.getDeclaredField("lastBackgroundAt")
        backgroundField.isAccessible = true
        backgroundField.setLong(viewModel, System.currentTimeMillis() - 301_000L)
        viewModel.onAppForegrounded()
        assertTrue(viewModel.uiState.value.hubSettings.appLocked)
        viewModel.unlockApp()
        assertFalse(viewModel.uiState.value.hubSettings.appLocked)
    }

    @Test
    fun updateBillingNoticeLocalizesKnownKeys() {
        val viewModel = EobViewModel()
        viewModel.updateBillingNotice(AppLanguage.English, "billing_not_ready")
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingNotReady"),
            viewModel.uiState.value.hubSettings.settingsNotice
        )
    }

    @Test
    fun appLockDoesNotTriggerBeforeFirstBackground() {
        val viewModel = EobViewModel()
        viewModel.setPinLockEnabled(true)
        viewModel.setAppLockTimeout(AppLockTimeout.FiveMinutes)
        viewModel.onAppForegrounded()
        assertFalse(viewModel.uiState.value.hubSettings.appLocked)
    }

    private fun waitForHubRecords(viewModel: EobViewModel) {
        var attempts = 0
        while (viewModel.eobRecords.value.isEmpty() && attempts < 1_000) {
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
