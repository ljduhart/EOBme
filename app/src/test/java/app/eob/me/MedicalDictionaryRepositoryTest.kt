package app.eob.me

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.eob.me.data.local.MedicalDictionaryDatabase
import app.eob.me.data.repository.MedicalDictionaryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MedicalDictionaryRepositoryTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = context.getDatabasePath("medical_dictionary.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }
        val instanceField = MedicalDictionaryDatabase::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun assetDatabaseContainsAtLeastFifteenThousandTerms() = runBlocking {
        val database = MedicalDictionaryDatabase.getInstance(context)
        val count = database.openHelper.readableDatabase.use { db ->
            db.query("SELECT COUNT(*) FROM medical_dictionary").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
        }
        assertTrue("Expected at least 15000 terms, found $count", count >= 15_000)
    }

    @Test
    fun repositoryFindsHypertensionPrefixMatch() = runBlocking {
        val repository = MedicalDictionaryRepository(context)
        val results = repository.searchTerms("hypertens").first()
        assertTrue(results.any { it.term.equals("Hypertension", ignoreCase = true) })
    }

    @Test
    fun repositoryReturnsDetailedEntryByExactTerm() = runBlocking {
        val repository = MedicalDictionaryRepository(context)
        val entry = repository.getTerm("Hypertension")
        assertNotNull(entry)
        assertEquals("Hypertension", entry?.term)
        assertTrue(entry!!.definition.isNotBlank())
        assertTrue(entry.detailedBreakdown.contains("Clinical usage"))
    }
}
