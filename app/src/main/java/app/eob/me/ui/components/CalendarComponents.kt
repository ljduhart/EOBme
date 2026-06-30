package app.eob.me.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorAppointment
import app.eob.me.ui.components.home.AppointmentDayColorBackground
import app.eob.me.ui.components.home.appointmentDateHoldClickable
import app.eob.me.ui.components.home.CalendarDayCellContent
import app.eob.me.ui.components.home.appointmentTypesOnDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarPicker(
    language: AppLanguage,
    visibleMonth: Calendar,
    appointments: List<DoctorAppointment>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val displayLocale = language.locale()
    val monthTitle = SimpleDateFormat("MMMM yyyy", displayLocale).format(visibleMonth.time)
    val daysInMonth = visibleMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = (visibleMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK) - 1
    val today = Calendar.getInstance()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onPreviousMonth) { Text("<") }
        Text(monthTitle, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = onNextMonth) { Text(">") }
    }
    Row(Modifier.fillMaxWidth()) {
        repeat(7) { index ->
            val labelDay = Calendar.getInstance(displayLocale).apply {
                firstDayOfWeek = Calendar.SUNDAY
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY + index)
            }
            val dayLabel = labelDay.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, displayLocale).orEmpty()
            Text(dayLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var dayNumber = 1
        repeat(6) { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { dayOfWeek ->
                    val isBlank = week == 0 && dayOfWeek < firstDayOffset || dayNumber > daysInMonth
                    if (isBlank) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateLabel = formatAppointmentDate(visibleMonth, dayNumber)
                        val typesOnDay = appointmentTypesOnDate(appointments, dateLabel)
                        val hasAppointment = typesOnDay.isNotEmpty()
                        val isToday = today.get(Calendar.YEAR) == visibleMonth.get(Calendar.YEAR) &&
                            today.get(Calendar.MONTH) == visibleMonth.get(Calendar.MONTH) &&
                            today.get(Calendar.DAY_OF_MONTH) == dayNumber
                        val displayDay = dayNumber
                        val cellTextColor = when {
                            hasAppointment -> Color.White
                            isToday -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .then(
                                    if (isToday) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                            androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .appointmentDateHoldClickable(onHoldComplete = { onDateSelected(dateLabel) }),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    hasAppointment -> Color.Transparent
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                if (hasAppointment) {
                                    AppointmentDayColorBackground(
                                        types = typesOnDay,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                CalendarDayCellContent(
                                    dayNumber = displayDay,
                                    hasAppointmentMarker = hasAppointment,
                                    textColor = cellTextColor
                                )
                            }
                        }
                        dayNumber++
                    }
                }
            }
        }
    }
}

fun formatAppointmentDate(month: Calendar, day: Int): String {
    val selected = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
    return SimpleDateFormat("MM/dd/yyyy", Locale.US).format(selected.time)
}
