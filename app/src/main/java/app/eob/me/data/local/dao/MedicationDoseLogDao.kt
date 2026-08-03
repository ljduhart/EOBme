package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.eob.me.data.local.entity.MedicationDoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDoseLogDao {
    @Query(
        "SELECT * FROM medication_dose_logs WHERE dayStartMillis = :dayStartMillis"
    )
    fun observeLogsForDay(dayStartMillis: Long): Flow<List<MedicationDoseLogEntity>>

    @Query(
        "SELECT * FROM medication_dose_logs WHERE medicationId = :medicationId " +
            "AND dayStartMillis = :dayStartMillis AND slot = :slot LIMIT 1"
    )
    suspend fun findLog(medicationId: Long, dayStartMillis: Long, slot: String): MedicationDoseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: MedicationDoseLogEntity)

    @Query(
        "SELECT taken FROM medication_dose_logs WHERE medicationId = :medicationId " +
            "AND dayStartMillis = :dayStartMillis AND slot = :slot LIMIT 1"
    )
    suspend fun isDoseTaken(medicationId: Long, dayStartMillis: Long, slot: String): Boolean?
}
