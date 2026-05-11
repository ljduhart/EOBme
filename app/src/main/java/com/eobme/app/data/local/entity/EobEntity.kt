package com.eobme.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "eobs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("dateOfService")]
)
data class EobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val imageUri: String,
    val dateOfService: Long,
    val providerName: String = "",
    val insuranceName: String = "",
    val billedAmount: Double = 0.0,
    val insurancePaid: Double = 0.0,
    val contractualAdjustment: Double = 0.0,
    val copay: Double = 0.0,
    val deductible: Double = 0.0,
    val coinsurance: Double = 0.0,
    val rawOcrText: String = "",
    val uploadDate: Long = System.currentTimeMillis(),
    val uploadSource: String = "camera"
)
