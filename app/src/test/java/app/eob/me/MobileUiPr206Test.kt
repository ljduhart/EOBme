package app.eob.me

import app.eob.me.data.EobAnalyzer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MobileUiPr206Test {
    @Test
    fun quickActionsCardOmitsProviderTypeChips() {
        val appointmentsSource = readSource("ui/components/home/HomeAppointmentsSection.kt")
        val cardSection = appointmentsSource
            .substringAfter("appointments.sortedBy")
            .substringBefore("if (showDialog)")
        assertFalse(cardSection.contains("ProviderTypeChipBar"))
        assertTrue(appointmentsSource.contains("ProviderTypeChipBar"))
        assertTrue(appointmentsSource.contains("editAppointment"))
        assertTrue(appointmentsSource.contains("removeAppointment"))
    }

    @Test
    fun appointmentDateGuardWiredThroughHomeAndViewModel() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(homeSource.contains("isAppointmentDateAllowed"))
        assertTrue(navSource.contains("isAppointmentDateAllowed = eobViewModel::isAppointmentDateAllowed"))
        assertTrue(viewModelSource.contains("fun isAppointmentDateAllowed"))
        assertTrue(viewModelSource.contains("EobAnalyzer.isAppointmentDateOnOrAfterToday"))
    }

    @Test
    fun calendarBlocksPastDates() {
        val calendarSource = readSource("ui/components/CalendarComponents.kt")
        val weekSource = readSource("ui/components/home/HomeWeekCalendar.kt")
        assertTrue(calendarSource.contains("isDateSelectable"))
        assertTrue(weekSource.contains("isDateSelectable = isDateSelectable"))
        assertTrue(weekSource.contains("isDateSelectable(dateLabel)"))
    }

    @Test
    fun pastAppointmentDatesRejectedByAnalyzer() {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val yesterdayLabel = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(yesterday.time)
        assertFalse(EobAnalyzer.isAppointmentDateOnOrAfterToday(yesterdayLabel))

        val todayLabel = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Calendar.getInstance().time)
        assertTrue(EobAnalyzer.isAppointmentDateOnOrAfterToday(todayLabel))
    }

    @Test
    fun eobCameraLocksToDocumentDetectionWhenNotReceiptScan() {
        val cameraSource = readSource("ui/screens/CameraCaptureScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(cameraSource.contains("eobScanOnly: Boolean"))
        assertTrue(cameraSource.contains("if (!eobScanOnly)"))
        assertTrue(cameraSource.contains("cameraEobDocumentRequired"))
        assertTrue(navSource.contains("eobScanOnly = !uiState.vaultReceiptScanPending"))
    }

    @Test
    fun historyProviderSearchFieldIsNotHeightClamped() {
        val historySource = readSource("ui/screens/EobHistoryScreen.kt")
        val searchBlock = historySource.substringAfter("item(key = \"history_search\")")
            .substringBefore("item(key = \"history_header\")")
        assertFalse(searchBlock.contains("heightIn(max = 48.dp)"))
        assertTrue(searchBlock.contains("singleLine = true"))
    }

    @Test
    fun protectedVeryfiPipelineUntouchedForPr206() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        assertFalse(pipelineSource.contains("eobScanOnly"))
        assertFalse(pipelineSource.contains("isAppointmentDateAllowed"))
    }

    @Test
    fun forwardAndBackwardNavigationPathsRemainWired() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("eobScanOnly = !uiState.vaultReceiptScanPending"))
        assertTrue(navSource.contains("eobViewModel.clearVaultReceiptScanPending()"))
        assertTrue(navSource.contains("navController.popBackStack()"))
        assertTrue(navSource.contains("popUpTo(EobRoute.CameraCapture.route) { inclusive = true }"))
        assertTrue(navSource.contains("eobViewModel.clearHistoryProviderSearch()"))
        assertTrue(navSource.contains("eobViewModel.openProviderRecordHistory(providerName)"))
        assertTrue(navSource.contains("customCameraPermissionLauncher.launch(Manifest.permission.CAMERA)"))
        assertTrue(navSource.contains("navController.navigate(EobRoute.Home.route)"))
        assertFalse(navSource.contains("searchQuery = uiState.historyProviderSearch"))
    }

    @Test
    fun updateAppointmentAllowsMetadataEditsForExistingPastDates() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        val appointmentsSource = readSource("ui/components/home/HomeAppointmentsSection.kt")
        assertTrue(viewModelSource.contains("date != existing.date && !isAppointmentDateAllowed(date)"))
        assertTrue(appointmentsSource.contains("selectedDate == originalEditingDate"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
