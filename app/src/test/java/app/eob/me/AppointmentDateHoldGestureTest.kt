package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDateHoldGestureTest {
    @Test
    fun appointmentDateHoldUsesTwoSecondDelay() {
        val source = readSource("ui/components/home/AppointmentDateHoldGesture.kt")
        assertTrue(source.contains("2_000L"))
        assertTrue(source.contains("detectTapGestures"))
        assertTrue(source.contains("tryAwaitRelease"))
        assertTrue(source.contains("appointmentDateHoldClickable"))
    }

    @Test
    fun calendarPickersUseAppointmentDateHoldGesture() {
        val weekCalendarSource = readSource("ui/components/home/HomeWeekCalendar.kt")
        val monthCalendarSource = readSource("ui/components/CalendarComponents.kt")
        assertTrue(weekCalendarSource.contains("appointmentDateHoldClickable"))
        assertTrue(monthCalendarSource.contains("appointmentDateHoldClickable"))
        assertFalse(weekCalendarSource.contains(".clickable { onDateSelected(dateLabel) }"))
        assertFalse(monthCalendarSource.contains(".clickable { onDateSelected(dateLabel) }"))
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
