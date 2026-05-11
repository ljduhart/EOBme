package com.eobme.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cpt_records",
    foreignKeys = [
        ForeignKey(
            entity = EobEntity::class,
            parentColumns = ["id"],
            childColumns = ["eobId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eobId"), Index("userId"), Index("year")]
)
data class CptRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eobId: Long,
    val userId: Long,
    val code: String,
    val description: String = "",
    val category: String = "OTHER",
    val dateOfService: Long,
    val year: Int
)
