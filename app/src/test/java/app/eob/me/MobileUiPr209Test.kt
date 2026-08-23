package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MobileUiPr209Test {
    @Test
    fun calendarWeekViewToggleKeepsLabelOnSingleHorizontalLine() {
        val source = readSource("ui/components/home/HomeWeekCalendar.kt")
        assertTrue(source.contains("Column(modifier = Modifier.weight(1f))"))
        assertTrue(source.contains("calendarWeekView"))
        assertTrue(source.contains("labelLarge"))
        assertTrue(source.contains("maxLines = 1"))
        assertTrue(source.contains("softWrap = false"))
    }

    @Test
    fun quickActionAppointmentButtonsShareIdenticalOutlinedLayout() {
        val source = readSource("ui/components/home/HomeAppointmentsSection.kt")
        assertTrue(source.contains("AppointmentOutlinedActionButton"))
        assertTrue(source.contains("editAppointment"))
        assertTrue(source.contains("removeAppointment"))
        val buttonSource = source.substringAfter("private fun AppointmentOutlinedActionButton")
        assertTrue(buttonSource.contains("heightIn(min = 40.dp)"))
        assertTrue(buttonSource.contains("maxLines = 1"))
        assertTrue(buttonSource.contains("softWrap = false"))
    }

    @Test
    fun protectedPipelineAndNavigationRemainUntouchedForPr209() {
        val pipelineSource = readSource("data/DocumentScanPipelineRepository.kt")
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertFalse(pipelineSource.contains("AppointmentOutlinedActionButton"))
        assertTrue(homeSource.contains("HomeWeekCalendar"))
        assertTrue(homeSource.contains("HomeAppointmentsSection"))
        assertTrue(navSource.contains("HomeScreen("))
    }

    @Test
    fun appointmentCalendarAndQuickActionsNavigationRemainsWired() {
        val homeSource = readSource("ui/screens/HomeScreen.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(homeSource.contains("onExpandedChange = onCalendarExpandedChange"))
        assertTrue(homeSource.contains("appointmentPrefillDate = date"))
        assertTrue(homeSource.contains("openAppointmentDialog = true"))
        assertTrue(navSource.contains("eobViewModel::setCalendarExpanded"))
        assertTrue(navSource.contains("eobViewModel.addAppointment"))
        assertTrue(navSource.contains("eobViewModel.removeAppointment"))
        assertTrue(viewModelSource.contains("fun setCalendarExpanded"))
        assertTrue(viewModelSource.contains("fun removeAppointment"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            File("src/main/java/app/eob/me/$relativePath"),
            File("app/src/main/java/app/eob/me/$relativePath")
        )
        return candidates.first { it.isFile }.readText()
    }
}
