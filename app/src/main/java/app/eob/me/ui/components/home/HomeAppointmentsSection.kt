package app.eob.me.ui.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun HomeAppointmentsSection(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    prefillDate: String,
    onPrefillHandled: () -> Unit,
    onAddAppointment: (String, String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(prefillDate) {
        if (prefillDate.isNotBlank()) {
            selectedDate = prefillDate
            showDialog = true
            onPrefillHandled()
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = EobStrings.t(language, "quickActions"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedButton(onClick = { showDialog = true }) {
                Text(EobStrings.t(language, "addAppointment"))
            }
        }

        if (appointments.isEmpty()) {
            Text(
                text = EobStrings.t(language, "noAppointments"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            appointments.sortedBy { it.date }.forEach { appointment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${appointment.date} • ${appointment.providerName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (appointment.time.isNotBlank()) {
                            Text(appointment.time, style = MaterialTheme.typography.bodySmall)
                        }
                        if (appointment.notes.isNotBlank()) {
                            Text(appointment.notes, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = { onRemoveAppointment(appointment) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(EobStrings.t(language, "removeAppointment"))
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(EobStrings.t(language, "addAppointment")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text(EobStrings.t(language, "appointmentProvider")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text(EobStrings.t(language, "appointmentDate")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("MM/DD/YYYY") }
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(EobStrings.t(language, "time")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(EobStrings.t(language, "appointmentNotes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAppointment(selectedDate, provider, time, notes)
                        provider = ""
                        time = ""
                        notes = ""
                        selectedDate = ""
                        showDialog = false
                    },
                    enabled = selectedDate.isNotBlank() && provider.isNotBlank()
                ) {
                    Text(EobStrings.t(language, "saveAppointment"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(EobStrings.t(language, "close"))
                }
            }
        )
    }
}
