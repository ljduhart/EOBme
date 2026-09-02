package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import app.eob.me.data.local.entity.MedicalDictionaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDictionaryDao {
    @Query(
        """
        SELECT rowid, term, pronunciation, definition, detailedBreakdown, category
        FROM medical_dictionary
        WHERE medical_dictionary MATCH :matchQuery
        LIMIT 50
        """
    )
    fun searchTerms(matchQuery: String): Flow<List<MedicalDictionaryEntity>>

    @Query(
        """
        SELECT rowid, term, pronunciation, definition, detailedBreakdown, category
        FROM medical_dictionary
        WHERE term = :term
        LIMIT 1
        """
    )
    suspend fun getTerm(term: String): MedicalDictionaryEntity?
}
