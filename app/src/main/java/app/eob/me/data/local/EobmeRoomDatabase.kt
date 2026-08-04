package app.eob.me.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.eob.me.data.local.dao.ClinicalNoteDao
import app.eob.me.data.local.dao.MedicationDao
import app.eob.me.data.local.dao.MedicationDoseLogDao
import app.eob.me.data.local.dao.ProviderDirectoryDao
import app.eob.me.data.local.dao.TaxVaultExpenseLedgerDao
import app.eob.me.data.local.entity.ClinicalNote
import app.eob.me.data.local.entity.MedicationDoseLogEntity
import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.local.entity.ProviderDirectoryEntity
import app.eob.me.data.local.entity.TaxVaultExpenseLedgerEntity

@Database(
    entities = [
        MedicationRecord::class,
        TaxVaultExpenseLedgerEntity::class,
        MedicationDoseLogEntity::class,
        ProviderDirectoryEntity::class,
        ClinicalNote::class
    ],
    version = 2,
    exportSchema = false
)
abstract class EobmeRoomDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun taxVaultExpenseLedgerDao(): TaxVaultExpenseLedgerDao
    abstract fun medicationDoseLogDao(): MedicationDoseLogDao
    abstract fun providerDirectoryDao(): ProviderDirectoryDao
    abstract fun clinicalNoteDao(): ClinicalNoteDao

    companion object {
        private const val DATABASE_NAME = "eobme_room.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS provider_directory (
                        providerId INTEGER NOT NULL PRIMARY KEY,
                        displayName TEXT NOT NULL,
                        roleLabel TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS clinical_notes (
                        noteId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        providerId INTEGER NOT NULL,
                        dateCreated INTEGER NOT NULL,
                        questionsToAsk TEXT NOT NULL,
                        providerAnswers TEXT NOT NULL,
                        FOREIGN KEY(providerId) REFERENCES provider_directory(providerId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_clinical_notes_providerId ON clinical_notes(providerId)"
                )
            }
        }

        @Volatile
        private var instance: EobmeRoomDatabase? = null

        fun getInstance(context: Context): EobmeRoomDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EobmeRoomDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
