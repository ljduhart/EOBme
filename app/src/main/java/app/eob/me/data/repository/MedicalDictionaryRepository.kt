package app.eob.me.data.repository

import android.content.Context
import app.eob.me.data.local.MedicalDictionaryDatabase
import app.eob.me.data.local.entity.MedicalDictionaryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class MedicalDictionaryRepository(context: Context) {
    private val dao = MedicalDictionaryDatabase.getInstance(context).medicalDictionaryDao()

    fun searchTerms(query: String): Flow<List<MedicalDictionaryEntity>> {
        val matchQuery = buildMatchQuery(query)
        if (matchQuery.isBlank()) {
            return flowOf(emptyList())
        }
        return dao.searchTerms(matchQuery)
    }

    suspend fun getTerm(term: String): MedicalDictionaryEntity? {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return null
        return dao.getTerm(trimmed)
    }

    private fun buildMatchQuery(query: String): String {
        val tokens = query
            .trim()
            .lowercase()
            .replace(Regex("""[^\w\s-]"""), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }
}
