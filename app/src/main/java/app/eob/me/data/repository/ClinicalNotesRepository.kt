package app.eob.me.data.repository

import android.content.Context
import app.eob.me.data.local.EobmeRoomDatabase
import app.eob.me.data.local.entity.ClinicalNote
import app.eob.me.data.local.entity.ProviderDirectoryEntity
import kotlinx.coroutines.flow.Flow

class ClinicalNotesRepository(context: Context) {
    private val database = EobmeRoomDatabase.getInstance(context.applicationContext)
    private val providerDao = database.providerDirectoryDao()
    private val noteDao = database.clinicalNoteDao()

    fun observeProviders(): Flow<List<ProviderDirectoryEntity>> = providerDao.observeProviders()

    fun observeNotesForProvider(providerId: Int): Flow<List<ClinicalNote>> {
        return noteDao.getNotesForProvider(providerId)
    }

    suspend fun syncProviderDirectory(entries: List<ProviderDirectoryEntity>) {
        if (entries.isEmpty()) return
        providerDao.upsertAll(entries)
    }

    suspend fun insertNote(note: ClinicalNote): Long = noteDao.insert(note)

    suspend fun updateNote(note: ClinicalNote) {
        noteDao.update(note)
    }

    suspend fun deleteNote(note: ClinicalNote) {
        noteDao.delete(note)
    }
}
