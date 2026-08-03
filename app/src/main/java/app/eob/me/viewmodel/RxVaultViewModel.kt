package app.eob.me.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.eob.me.data.MedicationDoseSlot
import app.eob.me.data.RxMedicationUiModel
import app.eob.me.data.RxVaultUiState
import app.eob.me.data.asCurrency
import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.repository.RxVaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RxVaultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RxVaultRepository(application.applicationContext)

    private val dayStartMillis = MutableStateFlow(RxVaultRepository.startOfDayMillis())
    private val draftState = MutableStateFlow(RxVaultUiState(isLoading = false))

    private val doseLogsForDay = dayStartMillis.flatMapLatest { day ->
        repository.observeTodayDoseLogs(day)
    }

    val uiState: StateFlow<RxVaultUiState> = combine(
        repository.observeMedications(),
        doseLogsForDay,
        draftState
    ) { medications, doseLogs, draft ->
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        val models = medications.map { medication ->
            RxMedicationUiModel(
                id = medication.id,
                medicationName = medication.medicationName,
                dosage = medication.dosage,
                quantity = medication.quantity,
                refillDateMillis = medication.refillDate,
                nextRefillLabel = dateFormat.format(Date(medication.refillDate)),
                copayAmount = medication.copayAmount,
                isFsaEligible = medication.isFsaEligible,
                morningTaken = doseLogs.any {
                    it.medicationId == medication.id &&
                        it.slot == MedicationDoseSlot.Morning.storageKey &&
                        it.taken
                },
                afternoonTaken = doseLogs.any {
                    it.medicationId == medication.id &&
                        it.slot == MedicationDoseSlot.Afternoon.storageKey &&
                        it.taken
                },
                eveningTaken = doseLogs.any {
                    it.medicationId == medication.id &&
                        it.slot == MedicationDoseSlot.Evening.storageKey &&
                        it.taken
                }
            )
        }
        draft.copy(
            medications = models,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RxVaultUiState())

    val fsaLedgerYtdTotal: StateFlow<Double> = repository.observeFsaLedgerTotalForCurrentYear()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun refreshDayBoundary() {
        dayStartMillis.value = RxVaultRepository.startOfDayMillis()
    }

    fun toggleDose(medicationId: Long, slot: MedicationDoseSlot, taken: Boolean) {
        viewModelScope.launch {
            repository.setDoseTaken(
                medicationId = medicationId,
                dayStartMillis = dayStartMillis.value,
                slot = slot,
                taken = taken
            )
        }
    }

    fun setShowAddForm(show: Boolean) {
        draftState.update { it.copy(showAddForm = show, errorMessage = "") }
    }

    fun updateDraftName(value: String) {
        draftState.update { it.copy(draftName = value) }
    }

    fun updateDraftDosage(value: String) {
        draftState.update { it.copy(draftDosage = value) }
    }

    fun updateDraftQuantity(value: String) {
        draftState.update { it.copy(draftQuantity = value) }
    }

    fun updateDraftCopay(value: String) {
        draftState.update { it.copy(draftCopay = value) }
    }

    fun updateDraftFsaEligible(value: Boolean) {
        draftState.update { it.copy(draftFsaEligible = value) }
    }

    fun saveDraftMedication() {
        val draft = draftState.value
        val name = draft.draftName.trim()
        val dosage = draft.draftDosage.trim()
        val quantity = draft.draftQuantity.trim().toIntOrNull()
        val copay = draft.draftCopay.trim().replace("$", "").toDoubleOrNull() ?: 0.0
        if (name.isBlank() || dosage.isBlank() || quantity == null || quantity <= 0) {
            draftState.update { it.copy(errorMessage = "invalid_medication") }
            return
        }
        val refillDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(quantity.toLong())
        val record = MedicationRecord(
            medicationName = name,
            dosage = dosage,
            quantity = quantity,
            refillDate = refillDate,
            copayAmount = copay,
            isFsaEligible = draft.draftFsaEligible
        )
        viewModelScope.launch {
            repository.insertMedication(record)
            draftState.update {
                it.copy(
                    showAddForm = false,
                    draftName = "",
                    draftDosage = "",
                    draftQuantity = "30",
                    draftCopay = "",
                    draftFsaEligible = true,
                    errorMessage = ""
                )
            }
        }
    }

    fun deleteMedication(medication: RxMedicationUiModel) {
        viewModelScope.launch {
            repository.deleteMedication(
                MedicationRecord(
                    id = medication.id,
                    medicationName = medication.medicationName,
                    dosage = medication.dosage,
                    quantity = medication.quantity,
                    refillDate = medication.refillDateMillis,
                    copayAmount = medication.copayAmount,
                    isFsaEligible = medication.isFsaEligible
                )
            )
        }
    }

    fun formatCopay(amount: Double): String = amount.asCurrency()
}
