package app.eob.me.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.eob.me.data.CareTeamCardDisplayState
import app.eob.me.data.CptBentoSnapshot
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.HistoryBentoSnapshot
import app.eob.me.data.InsuranceCardDisplay
import app.eob.me.data.InsuranceNewsBentoSnapshot
import app.eob.me.data.InvoiceProcessingPhase
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.data.ProviderDirectoryAssurance
import app.eob.me.data.UserProfile
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.CleanInsuranceCard
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
    insuranceCardDisplay: InsuranceCardDisplay,
    canEditInsuranceCard: Boolean,
    onSaveInsuranceCard: (
        insuranceName: String,
        memberId: String,
        groupNumber: String,
        pcpCopay: String,
        specialistCopay: String
    ) -> Unit,
    recordCount: Int,
    firebaseStatusLine: String,
    uploadNotice: String,
    appointments: List<DoctorAppointment>,
    preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
    careTeamCards: List<CareTeamCardDisplayState>,
    providerDirectoryAssurance: ProviderDirectoryAssurance,
    cptBentoSnapshot: CptBentoSnapshot,
    insuranceNewsBentoSnapshot: InsuranceNewsBentoSnapshot,
    ytdBentoSnapshot: YtdDeductibleBentoSnapshot,
    ytdBentoViewMode: YtdBentoViewMode,
    onYtdBentoViewModeSelected: (YtdBentoViewMode) -> Unit,
    calendarExpanded: Boolean,
    onCalendarExpandedChange: (Boolean) -> Unit,
    onSavePreferredDoctor: (PreferredDoctor) -> Unit,
    onAddAppointment: (String, String, String, String, CareTeamProviderType) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onUpdateAppointment: (Int, String, String, String, String, CareTeamProviderType) -> Unit,
    historySnapshot: HistoryBentoSnapshot,
    processingPhase: InvoiceProcessingPhase,
    isLoadingInvoice: Boolean,
    historyFilter: HistoryBentoFilter,
    providerAvatars: List<ProviderAvatarPreview>,
    onHistoryFilterSelected: (HistoryBentoFilter) -> Unit,
    onInvoiceFileDropFinished: () -> Unit,
    onBentoSelected: (HubBentoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var appointmentPrefillDate by remember { mutableStateOf("") }
    var openAppointmentDialog by remember { mutableStateOf(false) }
    var isEditingInsuranceCard by remember { mutableStateOf(false) }
    var draftInsuranceName by remember { mutableStateOf(profile.insuranceName) }
    var draftMemberId by remember { mutableStateOf(profile.insuranceId) }
    var draftGroupNumber by remember { mutableStateOf(profile.groupName) }
    var draftPcpCopay by remember { mutableStateOf(profile.pcpCopay) }
    var draftSpecialistCopay by remember { mutableStateOf(profile.specialistCopay) }

    LaunchedEffect(profile, isEditingInsuranceCard) {
        if (!isEditingInsuranceCard) {
            draftInsuranceName = profile.insuranceName
            draftMemberId = profile.insuranceId
            draftGroupNumber = profile.groupName
            draftPcpCopay = profile.pcpCopay
            draftSpecialistCopay = profile.specialistCopay
        }
    }

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
                    CleanInsuranceCard(
                        language = language,
                        display = insuranceCardDisplay,
                        isEditing = isEditingInsuranceCard,
                        draftInsuranceName = draftInsuranceName,
                        draftMemberId = draftMemberId,
                        draftGroupNumber = draftGroupNumber,
                        draftPcpCopay = draftPcpCopay,
                        draftSpecialistCopay = draftSpecialistCopay,
                        canEdit = canEditInsuranceCard,
                        onDraftInsuranceNameChange = { draftInsuranceName = it },
                        onDraftMemberIdChange = { draftMemberId = it },
                        onDraftGroupNumberChange = { draftGroupNumber = it },
                        onDraftPcpCopayChange = { draftPcpCopay = it },
                        onDraftSpecialistCopayChange = { draftSpecialistCopay = it },
                        onEditRequest = { isEditingInsuranceCard = true },
                        onSave = {
                            onSaveInsuranceCard(
                                draftInsuranceName,
                                draftMemberId,
                                draftGroupNumber,
                                draftPcpCopay,
                                draftSpecialistCopay
                            )
                            isEditingInsuranceCard = false
                        },
                        onCancel = {
                            draftInsuranceName = profile.insuranceName
                            draftMemberId = profile.insuranceId
                            draftGroupNumber = profile.groupName
                            draftPcpCopay = profile.pcpCopay
                            draftSpecialistCopay = profile.specialistCopay
                            isEditingInsuranceCard = false
                        },
                        modifier = Modifier.fillMaxWidth(0.92f)
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
                    careTeamCards = careTeamCards,
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
                                historySnapshot = historySnapshot,
                                processingPhase = processingPhase,
                                isLoadingInvoice = isLoadingInvoice,
                                historyFilter = historyFilter,
                                providerAvatars = providerAvatars,
                                providerDirectoryAssurance = providerDirectoryAssurance,
                                cptBentoSnapshot = cptBentoSnapshot,
                                insuranceNewsBentoSnapshot = insuranceNewsBentoSnapshot,
                                ytdBentoSnapshot = ytdBentoSnapshot,
                                ytdBentoViewMode = ytdBentoViewMode,
                                onYtdViewModeSelected = onYtdBentoViewModeSelected,
                                onClick = { onBentoSelected(destination) },
                                onHistoryFilterSelected = onHistoryFilterSelected,
                                onInvoiceFileDropFinished = onInvoiceFileDropFinished,
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
                    preferredDoctors = preferredDoctors,
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
