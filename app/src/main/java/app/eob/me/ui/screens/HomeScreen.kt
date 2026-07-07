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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary
import app.eob.me.ui.theme.EobHomeGradientBase
import app.eob.me.ui.theme.EobHomeGradientDeep
import app.eob.me.ui.theme.EobHomeGradientMid
import app.eob.me.ui.theme.EobHomeGradientSurface
import app.eob.me.ui.theme.EobHomeLightGradientBase
import app.eob.me.ui.theme.EobHomeLightGradientMid
import app.eob.me.ui.theme.EobHomeLightGradientTop
import app.eob.me.ui.theme.EobLightTextPrimary
import app.eob.me.ui.theme.EobLightTextSecondary
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobmeFeatureGate
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
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.TaxVaultBudgetSummary
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.TaxVaultVisibilityMode
import app.eob.me.data.UserProfile
import app.eob.me.data.YtdBentoViewMode
import app.eob.me.data.YtdDeductibleBentoSnapshot
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.CleanInsuranceCard
import app.eob.me.ui.components.bento.BentoGridCell
import app.eob.me.ui.components.home.HomeAppointmentsSection
import app.eob.me.ui.components.home.HomeCareTeamCards
import app.eob.me.ui.components.home.HomeWeekCalendar
import androidx.compose.foundation.layout.BoxWithConstraints
import app.eob.me.ui.components.home.TaxVaultVerticalFilterCard
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.text.style.TextOverflow

private val DarkHomeBackground = Brush.verticalGradient(
    colors = listOf(
        EobHomeGradientDeep,
        EobHomeGradientBase,
        EobHomeGradientSurface,
        EobHomeGradientMid,
        EobHomeGradientBase,
        EobHomeGradientDeep
    )
)

private val LightHomeBackground = Brush.verticalGradient(
    colors = listOf(
        EobHomeLightGradientTop,
        EobHomeLightGradientMid,
        EobHomeLightGradientBase,
        EobHomeLightGradientMid,
        EobHomeLightGradientTop
    )
)

/**
 * Scrollable main hub — state from [app.eob.me.viewmodel.EobViewModel].
 */
@Composable
fun HomeScreen(
    language: AppLanguage,
    darkModeEnabled: Boolean,
    profile: UserProfile,
    insuranceCardDisplay: InsuranceCardDisplay,
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
    appealGeneratorBentoProcessing: Boolean,
    onAppealGeneratorProcessingFinished: () -> Unit,
    onBentoSelected: (HubBentoDestination) -> Unit,
    taxVaultFilterState: TaxVaultFilterState,
    taxVaultVisibilityMode: TaxVaultVisibilityMode,
    taxVaultBudgetSummary: TaxVaultBudgetSummary,
    subscriptionTier: SubscriptionTier,
    onPremiumFeatureLocked: () -> Unit,
    onTaxVaultFilterSelected: (TaxVaultFilterState) -> Unit,
    onTaxVaultVisibilityModeSelected: (TaxVaultVisibilityMode) -> Unit,
    onVaultDoorUnlocked: () -> Unit,
    onInsurancePrescriptionsChange: (String) -> Unit,
    onInsuranceDoctorNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var appointmentPrefillDate by remember { mutableStateOf("") }
    var openAppointmentDialog by remember { mutableStateOf(false) }

    val homeBackground = if (darkModeEnabled) DarkHomeBackground else LightHomeBackground
    val homePrimaryText = if (darkModeEnabled) EobCyberTextPrimary else EobLightTextPrimary
    val homeSecondaryText = if (darkModeEnabled) EobCyberTextSecondary else EobLightTextSecondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(homeBackground)
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
                        color = homePrimaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
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
                        color = homeSecondaryText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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
                        currentPrescriptions = profile.currentPrescriptions,
                        doctorQuickNotes = profile.doctorQuickNotes,
                        onCurrentPrescriptionsChange = onInsurancePrescriptionsChange,
                        onDoctorQuickNotesChange = onInsuranceDoctorNotesChange,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            if (uploadNotice.isNotBlank()) {
                item {
                    Text(
                        text = uploadNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = homePrimaryText,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            item {
                HomeCareTeamCards(
                    language = language,
                    careTeamCards = careTeamCards,
                    preferredDoctors = preferredDoctors,
                    onSaveDoctor = onSavePreferredDoctor,
                    smartCardSummariesEnabled = EobmeFeatureGate.hasSmartCardSummaries(subscriptionTier),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = EobStrings.t(language, "featuresSection"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = homePrimaryText,
                    modifier = Modifier.padding(bottom = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HubBentoDestination.gridRows.forEach { row ->
                item {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val bentoSpacing = (maxWidth * 0.065f).coerceIn(14.dp, 28.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(bentoSpacing)
                        ) {
                            row.forEach { destination ->
                                BentoGridCell(
                                    language = language,
                                    destination = destination,
                                    historySnapshot = historySnapshot,
                                    taxVaultActive = taxVaultFilterState != TaxVaultFilterState.OFF,
                                    taxVaultBudgetSummary = taxVaultBudgetSummary,
                                    taxVaultFilterState = taxVaultFilterState,
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
                                    appealGeneratorBentoProcessing = appealGeneratorBentoProcessing,
                                    subscriptionTier = subscriptionTier,
                                    onLockedClick = onPremiumFeatureLocked,
                                    onClick = { onBentoSelected(destination) },
                                    onHistoryFilterSelected = onHistoryFilterSelected,
                                    onInvoiceFileDropFinished = onInvoiceFileDropFinished,
                                    onAppealGeneratorProcessingFinished = onAppealGeneratorProcessingFinished,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                TaxVaultVerticalFilterCard(
                    language = language,
                    darkModeEnabled = darkModeEnabled,
                    isGoldTier = subscriptionTier.isGold(),
                    filterState = taxVaultFilterState,
                    visibilityMode = taxVaultVisibilityMode,
                    budgetSummary = taxVaultBudgetSummary,
                    onFilterSelected = onTaxVaultFilterSelected,
                    onVisibilityModeSelected = onTaxVaultVisibilityModeSelected,
                    onVaultDoorUnlocked = onVaultDoorUnlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
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
