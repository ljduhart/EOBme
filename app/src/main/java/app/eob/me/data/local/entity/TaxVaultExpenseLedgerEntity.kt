package app.eob.me.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room ledger for Tax Vault / HSA–FSA year-to-date copay tracking (Rx and related expenses).
 */
@Entity(
    tableName = "tax_vault_expense_ledger",
    indices = [Index(value = ["calendarYear"]), Index(value = ["medicationId"])]
)
data class TaxVaultExpenseLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicationId: Long?,
    val description: String,
    val amount: Double,
    val isFsaEligible: Boolean,
    val recordedAtMillis: Long,
    val calendarYear: Int
)
