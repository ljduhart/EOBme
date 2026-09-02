package app.eob.me.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4
@Entity(tableName = "medical_dictionary")
data class MedicalDictionaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Int,
    val term: String,
    val pronunciation: String,
    val definition: String,
    val detailedBreakdown: String,
    val category: String
)
