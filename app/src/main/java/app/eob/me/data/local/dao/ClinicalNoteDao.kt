package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.eob.me.data.local.entity.ClinicalNote
import kotlinx.coroutines.flow.Flow

@Dao
interface ClinicalNoteDao {
    @Query("SELECT * FROM clinical_notes WHERE providerId = :providerId ORDER BY dateCreated DESC")
    fun getNotesForProvider(providerId: Int): Flow<List<ClinicalNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: ClinicalNote): Long

    @Update
    suspend fun update(note: ClinicalNote)

    @Delete
    suspend fun delete(note: ClinicalNote)
}
