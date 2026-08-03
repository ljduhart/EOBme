package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.eob.me.data.local.entity.TaxVaultExpenseLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxVaultExpenseLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TaxVaultExpenseLedgerEntity): Long

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM tax_vault_expense_ledger " +
            "WHERE calendarYear = :year AND isFsaEligible = 1"
    )
    fun observeFsaEligibleTotalForYear(year: Int): Flow<Double>

    @Query(
        "SELECT * FROM tax_vault_expense_ledger WHERE calendarYear = :year " +
            "ORDER BY recordedAtMillis DESC"
    )
    fun observeEntriesForYear(year: Int): Flow<List<TaxVaultExpenseLedgerEntity>>

    @Query("DELETE FROM tax_vault_expense_ledger WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)
}
