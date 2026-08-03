package app.eob.me.data

enum class MedicationDoseSlot(val storageKey: String) {
    Morning("morning"),
    Afternoon("afternoon"),
    Evening("evening");

    companion object {
        fun fromStorageKey(key: String): MedicationDoseSlot? {
            return entries.firstOrNull { it.storageKey == key }
        }
    }
}

data class MedicationDoseToggleKey(
    val medicationId: Long,
    val slot: MedicationDoseSlot
)

data class RxMedicationUiModel(
    val id: Long,
    val medicationName: String,
    val dosage: String,
    val quantity: Int,
    val refillDateMillis: Long,
    val nextRefillLabel: String,
    val copayAmount: Double,
    val isFsaEligible: Boolean,
    val morningTaken: Boolean,
    val afternoonTaken: Boolean,
    val eveningTaken: Boolean
)

data class RxVaultUiState(
    val medications: List<RxMedicationUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val showAddForm: Boolean = false,
    val draftName: String = "",
    val draftDosage: String = "",
    val draftQuantity: String = "30",
    val draftCopay: String = "",
    val draftFsaEligible: Boolean = true,
    val errorMessage: String = ""
)
