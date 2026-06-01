package app.eob.me.ui.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobStrings
import app.eob.me.ui.components.CalendarPicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeWeekCalendar(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var visibleMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val appointmentDates = remember(appointments) { appointments.map { it.date }.toSet() }
    val weekDays = remember { currentWeekDays() }
    val weekRangeLabel = remember(weekDays) { weekRangeLabel(weekDays) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = EobStrings.t(language, "appointmentCalendar"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (expanded) {
                            EobStrings.t(language, "calendarFullMonthView")
                        } else {
                            weekRangeLabel
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
                OutlinedButton(onClick = { onExpandedChange(!expanded) }) {
                    Text(
                        if (expanded) {
                            EobStrings.t(language, "calendarWeekView")
                        } else {
                            EobStrings.t(language, "calendarExpand")
                        }
                    )
                }
            }

            if (expanded) {
                CalendarPicker(
                    visibleMonth = visibleMonth,
                    appointments = appointments,
                    onPreviousMonth = {
                        visibleMonth = (visibleMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    onNextMonth = {
                        visibleMonth = (visibleMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    },
                    onDateSelected = onDateSelected
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    weekDays.forEach { day ->
                        val dateLabel = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(day.time)
                        val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
                        val dayName = day.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US).orEmpty()
                        val hasAppointment = appointmentDates.contains(dateLabel)
                        val isToday = isSameDay(day, Calendar.getInstance())

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.72f)
                                .clickable { onDateSelected(dateLabel) },
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isToday -> MaterialTheme.colorScheme.primary
                                    hasAppointment -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.background
                                }
                            )
                        ) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayName.take(3),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        }
                                    )
                                    Text(
                                        text = if (hasAppointment) "$dayOfMonth •" else dayOfMonth.toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isToday) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun currentWeekDays(): List<Calendar> {
    val anchor = Calendar.getInstance()
    val start = (anchor.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.SUNDAY
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return (0..6).map { offset ->
        (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
    }
}

private fun weekRangeLabel(days: List<Calendar>): String {
    if (days.isEmpty()) return "This week"
    val formatter = SimpleDateFormat("MMM d", Locale.US)
    return "${formatter.format(days.first().time)} – ${formatter.format(days.last().time)}"
}

private fun isSameDay(first: Calendar, second: Calendar): Boolean {
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}
