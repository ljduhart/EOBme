package com.eobme.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eobme.app.data.local.dao.CptRecordDao
import com.eobme.app.data.local.dao.EobDao
import com.eobme.app.data.local.dao.InsuranceCardDao
import com.eobme.app.data.local.dao.UserDao
import com.eobme.app.data.local.entity.CptRecordEntity
import com.eobme.app.data.local.entity.EobEntity
import com.eobme.app.data.local.entity.InsuranceCardEntity
import com.eobme.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EobEntity::class,
        InsuranceCardEntity::class,
        CptRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eobDao(): EobDao
    abstract fun insuranceCardDao(): InsuranceCardDao
    abstract fun cptRecordDao(): CptRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "eobme_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
