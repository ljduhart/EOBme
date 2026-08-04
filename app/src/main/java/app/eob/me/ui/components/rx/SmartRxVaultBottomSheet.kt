package app.eob.me.ui.components.rx

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.MedicationDoseSlot
import app.eob.me.data.RxMedicationUiModel
import app.eob.me.data.RxVaultUiState
import app.eob.me.ui.theme.EobBrandGlow
import app.eob.me.ui.theme.EobCyberAccent
import app.eob.me.ui.theme.EobCyberBackgroundDeep
import app.eob.me.ui.theme.EobCyberSurface
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary

private val RxVaultBackground = EobCyberBackgroundDeep
private val RxBentoSurface = Color(0xFF0D1B2E)
private val RxSilverBorder = Color(0xFFC0C8D4)
private val RxGlowAccent = EobBrandGlow.copy(alpha = 0.55f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRxVaultBottomSheet(
    language: AppLanguage,
    visible: Boolean,
    state: RxVaultUiState,
    fsaYtdTotal: Double,
    onDismiss: () -> Unit,
    onToggleDose: (medicationId: Long, slot: MedicationDoseSlot, taken: Boolean) -> Unit,
    onShowAddForm: (Boolean) -> Unit,
    onDraftName: (String) -> Unit,
    onDraftDosage: (String) -> Unit,
    onDraftQuantity: (String) -> Unit,
    onDraftCopay: (String) -> Unit,
    onDraftFsaEligible: (Boolean) -> Unit,
    onSaveMedication: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    if (!visible) return

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RxVaultBackground,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .background(RxSilverBorder.copy(alpha = 0.65f), RoundedCornerShape(50))
            )
        }
    ) {
        SmartRxVaultSheetContent(
            language = language,
            state = state,
            fsaYtdTotal = fsaYtdTotal,
            onToggleDose = onToggleDose,
            onShowAddForm = onShowAddForm,
            onDraftName = onDraftName,
            onDraftDosage = onDraftDosage,
            onDraftQuantity = onDraftQuantity,
            onDraftCopay = onDraftCopay,
            onDraftFsaEligible = onDraftFsaEligible,
            onSaveMedication = onSaveMedication,
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun SmartRxVaultSheetContent(
    language: AppLanguage,
    state: RxVaultUiState,
    fsaYtdTotal: Double,
    onToggleDose: (medicationId: Long, slot: MedicationDoseSlot, taken: Boolean) -> Unit,
    onShowAddForm: (Boolean) -> Unit,
    onDraftName: (String) -> Unit,
    onDraftDosage: (String) -> Unit,
    onDraftQuantity: (String) -> Unit,
    onDraftCopay: (String) -> Unit,
    onDraftFsaEligible: (Boolean) -> Unit,
    onSaveMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val addFormListIndex = 4

    LaunchedEffect(state.showAddForm) {
        if (state.showAddForm) {
            listState.animateScrollToItem(addFormListIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "rx_vault_title") {
            Text(
                text = EobStrings.t(language, "rxVaultSheetTitle"),
                color = EobCyberTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        item(key = "rx_vault_ytd") {
            Text(
                text = EobStrings.tf(language, "rxVaultFsaYtdTotal", formatCurrency(fsaYtdTotal)),
                color = EobCyberTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        item(key = "rx_vault_timeline") {
            RxDailyTimelineRow(
                language = language,
                medications = state.medications,
                onToggleDose = onToggleDose
            )
        }
        item(key = "rx_vault_list_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = EobStrings.t(language, "rxVaultMedicationListTitle"),
                    color = EobCyberTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = { onShowAddForm(!state.showAddForm) }) {
                    Text(EobStrings.t(language, if (state.showAddForm) "cancel" else "rxVaultAddMedication"))
                }
            }
        }
        if (state.showAddForm) {
            item(key = "rx_vault_add_form") {
                RxAddMedicationForm(
                    language = language,
                    state = state,
                    onDraftName = onDraftName,
                    onDraftDosage = onDraftDosage,
                    onDraftQuantity = onDraftQuantity,
                    onDraftCopay = onDraftCopay,
                    onDraftFsaEligible = onDraftFsaEligible,
                    onSaveMedication = onSaveMedication
                )
            }
        }
        items(state.medications, key = { it.id }) { medication ->
            RxVaultBentoCard(
                language = language,
                medication = medication,
                onToggleDose = onToggleDose
            )
        }
    }
}

@Composable
private fun RxDailyTimelineRow(
    language: AppLanguage,
    medications: List<RxMedicationUiModel>,
    onToggleDose: (medicationId: Long, slot: MedicationDoseSlot, taken: Boolean) -> Unit
) {
    val slots = listOf(
        MedicationDoseSlot.Morning to "rxVaultSlotMorning",
        MedicationDoseSlot.Afternoon to "rxVaultSlotAfternoon",
        MedicationDoseSlot.Evening to "rxVaultSlotEvening"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = EobStrings.t(language, "rxVaultTodaySchedule"),
            color = EobCyberTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(slots) { (slot, labelKey) ->
                val anyTaken = medications.any { med ->
                    when (slot) {
                        MedicationDoseSlot.Morning -> med.morningTaken
                        MedicationDoseSlot.Afternoon -> med.afternoonTaken
                        MedicationDoseSlot.Evening -> med.eveningTaken
                    }
                }
                FilterChip(
                    selected = anyTaken,
                    onClick = {
                        val targetTaken = !anyTaken
                        medications.forEach { med ->
                            val current = when (slot) {
                                MedicationDoseSlot.Morning -> med.morningTaken
                                MedicationDoseSlot.Afternoon -> med.afternoonTaken
                                MedicationDoseSlot.Evening -> med.eveningTaken
                            }
                            if (current != targetTaken) {
                                onToggleDose(med.id, slot, targetTaken)
                            }
                        }
                    },
                    label = { Text(EobStrings.t(language, labelKey)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EobCyberAccent.copy(alpha = 0.25f),
                        containerColor = EobCyberSurface,
                        selectedLabelColor = EobCyberTextPrimary,
                        labelColor = EobCyberTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = anyTaken,
                        borderColor = RxSilverBorder.copy(alpha = 0.5f),
                        selectedBorderColor = RxGlowAccent
                    )
                )
            }
        }
    }
}

@Composable
private fun RxVaultBentoCard(
    language: AppLanguage,
    medication: RxMedicationUiModel,
    onToggleDose: (medicationId: Long, slot: MedicationDoseSlot, taken: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = RxGlowAccent)
            .background(
                brush = Brush.verticalGradient(listOf(RxBentoSurface, EobCyberBackgroundDeep)),
                shape = RoundedCornerShape(16.dp)
            )
            .border(BorderStroke(1.5.dp, RxSilverBorder.copy(alpha = 0.85f)), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = medication.medicationName,
            color = EobCyberTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = EobStrings.tf(language, "rxVaultDosageLine", medication.dosage),
            color = EobCyberTextSecondary
        )
        Text(
            text = EobStrings.tf(language, "rxVaultNextRefill", medication.nextRefillLabel),
            color = EobCyberAccent,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DoseChip(
                language = language,
                labelKey = "rxVaultSlotMorning",
                selected = medication.morningTaken,
                onClick = {
                    onToggleDose(medication.id, MedicationDoseSlot.Morning, !medication.morningTaken)
                }
            )
            DoseChip(
                language = language,
                labelKey = "rxVaultSlotAfternoon",
                selected = medication.afternoonTaken,
                onClick = {
                    onToggleDose(medication.id, MedicationDoseSlot.Afternoon, !medication.afternoonTaken)
                }
            )
            DoseChip(
                language = language,
                labelKey = "rxVaultSlotEvening",
                selected = medication.eveningTaken,
                onClick = {
                    onToggleDose(medication.id, MedicationDoseSlot.Evening, !medication.eveningTaken)
                }
            )
        }
    }
}

@Composable
private fun DoseChip(
    language: AppLanguage,
    labelKey: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(EobStrings.t(language, labelKey)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = EobCyberAccent.copy(alpha = 0.22f),
            containerColor = EobCyberSurface
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = RxSilverBorder.copy(alpha = 0.45f),
            selectedBorderColor = RxGlowAccent
        )
    )
}

@Composable
private fun RxAddMedicationForm(
    language: AppLanguage,
    state: RxVaultUiState,
    onDraftName: (String) -> Unit,
    onDraftDosage: (String) -> Unit,
    onDraftQuantity: (String) -> Unit,
    onDraftCopay: (String) -> Unit,
    onDraftFsaEligible: (Boolean) -> Unit,
    onSaveMedication: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = EobCyberTextPrimary,
        unfocusedTextColor = EobCyberTextPrimary,
        focusedBorderColor = EobCyberAccent,
        unfocusedBorderColor = RxSilverBorder.copy(alpha = 0.5f)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, RxSilverBorder.copy(alpha = 0.6f)), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.errorMessage.isNotBlank()) {
            Text(
                text = EobStrings.t(language, state.errorMessage),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
        OutlinedTextField(
            value = state.draftName,
            onValueChange = onDraftName,
            label = { Text(EobStrings.t(language, "rxVaultFieldName")) },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.draftDosage,
            onValueChange = onDraftDosage,
            label = { Text(EobStrings.t(language, "rxVaultFieldDosage")) },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.draftQuantity,
            onValueChange = onDraftQuantity,
            label = { Text(EobStrings.t(language, "rxVaultFieldQuantity")) },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.draftCopay,
            onValueChange = onDraftCopay,
            label = { Text(EobStrings.t(language, "rxVaultFieldCopay")) },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = EobStrings.t(language, "rxVaultFieldFsaEligible"),
                color = EobCyberTextSecondary
            )
            Switch(checked = state.draftFsaEligible, onCheckedChange = onDraftFsaEligible)
        }
        Button(onClick = onSaveMedication, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "rxVaultSaveMedication"))
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format(java.util.Locale.US, "$%.2f", amount)
}
