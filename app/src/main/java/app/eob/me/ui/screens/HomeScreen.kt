package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.bento.BentoGridCell
import app.eob.me.ui.components.home.HomeAppointmentsSection
import app.eob.me.ui.components.home.HomeWeekCalendar

/**
 * Scrollable main hub — state from [app.eob.me.viewmodel.EobViewModel].
 */
@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: UserProfile,
    recordCount: Int,
    firebaseStatusLine: String,
    uploadNotice: String,
    appointments: List<DoctorAppointment>,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    onAddAppointment: (String, String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onBentoSelected: (HubBentoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var appointmentPrefillDate by remember { mutableStateOf("") }
    var openAppointmentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = EobStrings.tf(
                        language,
                        "welcomeUser",
                        profile.firstName.ifBlank { EobStrings.t(language, "member") }
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = EobStrings.tf(
                        language,
                        "homeRecordSummary",
                        recordCount,
                        if (recordCount == 1) {
                            EobStrings.t(language, "eobSingular")
                        } else {
                            EobStrings.t(language, "eobs")
                        },
                        firebaseStatusLine
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        item {
            HomeInsuranceCard(language = language, profile = profile, modifier = Modifier.fillMaxWidth())
        }

        if (uploadNotice.isNotBlank()) {
            item {
                Text(
                    text = uploadNotice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Text(
                text = EobStrings.t(language, "featuresSection"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        HubBentoDestination.gridRows.forEach { row ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    row.forEach { destination ->
                        BentoGridCell(
                            language = language,
                            destination = destination,
                            onClick = { onBentoSelected(destination) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            HomeWeekCalendar(
                language = language,
                appointments = appointments,
                expanded = calendarExpanded,
                onExpandedChange = onCalendarExpandedChange,
                onDateSelected = { date ->
                    appointmentPrefillDate = date
                    openAppointmentDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            HomeAppointmentsSection(
                language = language,
                appointments = appointments,
                prefillDate = if (openAppointmentDialog) appointmentPrefillDate else "",
                onPrefillHandled = { openAppointmentDialog = false },
                onAddAppointment = { date, provider, time, notes ->
                    onAddAppointment(date, provider, time, notes)
                    appointmentPrefillDate = ""
                    openAppointmentDialog = false
                },
                onRemoveAppointment = onRemoveAppointment,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HomeInsuranceCard(
    language: AppLanguage,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val silverBlueBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE8EEF5),
            Color(0xFF9BB8D9),
            Color(0xFF5B8DEF),
            Color(0xFFC0D4E8),
            Color(0xFFE8EEF5)
        )
    )
    val memberName = profile.fullName.ifBlank { EobStrings.t(language, "member") }
    val insuranceName = profile.insuranceName.ifBlank { EobStrings.t(language, "addInsuranceInfo") }
    val notSet = EobStrings.t(language, "valueNotSet")
    val insuranceId = profile.insuranceId.ifBlank { notSet }
    val groupNumber = profile.groupName.ifBlank { notSet }

    Card(
        modifier = modifier
            .border(width = 2.5.dp, brush = silverBlueBorder, shape = RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color(0xFFEEF4FA)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = EobStrings.t(language, "homeInsuranceCardTitle"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            InsuranceCardLine(
                label = EobStrings.t(language, "insuranceCardMemberLabel"),
                value = memberName
            )
            InsuranceCardLine(
                label = EobStrings.t(language, "insuranceNameField"),
                value = insuranceName
            )
            InsuranceCardLine(
                label = EobStrings.t(language, "insuranceId"),
                value = insuranceId
            )
            InsuranceCardLine(
                label = EobStrings.t(language, "groupName"),
                value = groupNumber
            )
        }
    }
}

@Composable
private fun InsuranceCardLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
