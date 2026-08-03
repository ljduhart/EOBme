package app.eob.me.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_dose_logs",
    indices = [Index(value = ["medicationId", "dayStartMillis", "slot"], unique = true)]
)
data class MedicationDoseLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicationId: Long,
    val dayStartMillis: Long,
    val slot: String,
    val taken: Boolean
)
