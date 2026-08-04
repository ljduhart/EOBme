package app.eob.me.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clinical_notes",
    foreignKeys = [
        ForeignKey(
            entity = ProviderDirectoryEntity::class,
            parentColumns = ["providerId"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["providerId"])]
)
data class ClinicalNote(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0L,
    val providerId: Int,
    val dateCreated: Long,
    val questionsToAsk: String,
    val providerAnswers: String
)
