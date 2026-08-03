package app.eob.me.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.eob.me.data.local.dao.MedicationDao
import app.eob.me.data.local.dao.MedicationDoseLogDao
import app.eob.me.data.local.dao.TaxVaultExpenseLedgerDao
import app.eob.me.data.local.entity.MedicationDoseLogEntity
import app.eob.me.data.local.entity.MedicationRecord
import app.eob.me.data.local.entity.TaxVaultExpenseLedgerEntity

@Database(
    entities = [
        MedicationRecord::class,
        TaxVaultExpenseLedgerEntity::class,
        MedicationDoseLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EobmeRoomDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun taxVaultExpenseLedgerDao(): TaxVaultExpenseLedgerDao
    abstract fun medicationDoseLogDao(): MedicationDoseLogDao

    companion object {
        private const val DATABASE_NAME = "eobme_room.db"

        @Volatile
        private var instance: EobmeRoomDatabase? = null

        fun getInstance(context: Context): EobmeRoomDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EobmeRoomDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
