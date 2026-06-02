package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobStrings
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.UserProfile
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.bento.BentoGridCell
import app.eob.me.ui.components.home.HomeAppointmentsSection
import app.eob.me.ui.components.home.HomeCareTeamCards
import app.eob.me.ui.components.home.HomeWeekCalendar

private val MetallicMedicalBlue = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0A4D73),
        Color(0xFF1565A8),
        Color(0xFF1B7BBD),
        Color(0xFF2498EA),
        Color(0xFF1565A8),
        Color(0xFF0C3D5E)
    )
)

private val HomeOnBluePrimary = Color(0xFFFFFFFF)
private val HomeOnBlueSecondary = Color(0xFFD8ECFA)

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
    preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    onSavePreferredDoctor: (PreferredDoctor) -> Unit,
    onAddAppointment: (String, String, String, String, CareTeamProviderType) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onUpdateAppointment: (Int, String, String, String, String, CareTeamProviderType) -> Unit,
    onBentoSelected: (HubBentoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var appointmentPrefillDate by remember { mutableStateOf("") }
    var openAppointmentDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetallicMedicalBlue)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
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
                        color = HomeOnBluePrimary
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
                        color = HomeOnBlueSecondary
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    HomeInsuranceCard(
                        language = language,
                        profile = profile,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                }
            }

            if (uploadNotice.isNotBlank()) {
                item {
                    Text(
                        text = uploadNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = HomeOnBluePrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                HomeCareTeamCards(
                    language = language,
                    preferredDoctors = preferredDoctors,
                    onSaveDoctor = onSavePreferredDoctor,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = EobStrings.t(language, "featuresSection"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeOnBluePrimary,
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
                    onAddAppointment = { date, provider, time, notes, providerType ->
                        onAddAppointment(date, provider, time, notes, providerType)
                        appointmentPrefillDate = ""
                        openAppointmentDialog = false
                    },
                    onRemoveAppointment = onRemoveAppointment,
                    onUpdateAppointment = onUpdateAppointment,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HomeInsuranceCard(
    language: AppLanguage,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val darkSilverBlueBorder = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2C4A63),
            Color(0xFF4A6B85),
            Color(0xFF5A7A94),
            Color(0xFF3D5F75),
            Color(0xFF6B8FA8),
            Color(0xFF2A4558),
            Color(0xFF4D7290),
            Color(0xFF3A566C),
            Color(0xFF5C7E9A),
            Color(0xFF2C4A63)
        )
    )
    val cardShape = RoundedCornerShape(20.dp)
    val memberName = profile.fullName.ifBlank { EobStrings.t(language, "member") }
    val insuranceName = profile.insuranceName.ifBlank { EobStrings.t(language, "addInsuranceInfo") }
    val notSet = EobStrings.t(language, "valueNotSet")
    val insuranceId = profile.insuranceId.ifBlank { notSet }
    val groupNumber = profile.groupName.ifBlank { notSet }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 5.dp, brush = darkSilverBlueBorder, shape = cardShape)
            .padding(2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8FAFC).copy(alpha = 0.98f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFF0F6FC),
                                Color(0xFFE8F2FA)
                            )
                        )
                    )
                    .padding(17.dp),
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
