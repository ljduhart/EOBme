package app.eob.me.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.eob.me.data.local.dao.MedicalDictionaryDao
import app.eob.me.data.local.entity.MedicalDictionaryEntity

@Database(
    entities = [MedicalDictionaryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MedicalDictionaryDatabase : RoomDatabase() {
    abstract fun medicalDictionaryDao(): MedicalDictionaryDao

    companion object {
        private const val DATABASE_NAME = "medical_dictionary.db"
        private const val ASSET_PATH = "databases/medical_dictionary.db"

        @Volatile
        private var instance: MedicalDictionaryDatabase? = null

        fun getInstance(context: Context): MedicalDictionaryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicalDictionaryDatabase::class.java,
                    DATABASE_NAME
                )
                    .createFromAsset(ASSET_PATH)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
