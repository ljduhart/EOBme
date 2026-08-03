package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.eob.me.data.local.entity.MedicationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY medicationName COLLATE NOCASE ASC")
    fun getAllMedications(): Flow<List<MedicationRecord>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: MedicationRecord): Long

    @Update
    suspend fun update(medication: MedicationRecord)

    @Delete
    suspend fun delete(medication: MedicationRecord)
}
