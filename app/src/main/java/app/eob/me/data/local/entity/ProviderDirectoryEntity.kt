package app.eob.me.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local provider directory rows used to link [ClinicalNote] records to hub / EOB providers.
 */
@Entity(tableName = "provider_directory")
data class ProviderDirectoryEntity(
    @PrimaryKey val providerId: Int,
    val displayName: String,
    val roleLabel: String
)
