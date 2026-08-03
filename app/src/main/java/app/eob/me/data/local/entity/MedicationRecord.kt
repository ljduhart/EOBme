package app.eob.me.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicationName: String,
    val dosage: String,
    val quantity: Int,
    val refillDate: Long,
    val copayAmount: Double,
    val isFsaEligible: Boolean
)
