package com.eobme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eobme.app.data.local.entity.CptRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CptRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: CptRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CptRecordEntity>)

    @Query("SELECT * FROM cpt_records WHERE userId = :userId AND year = :year ORDER BY code ASC")
    fun observeByUserAndYear(userId: Long, year: Int): Flow<List<CptRecordEntity>>

    @Query("SELECT * FROM cpt_records WHERE userId = :userId AND year = :year ORDER BY code ASC")
    suspend fun getByUserAndYear(userId: Long, year: Int): List<CptRecordEntity>

    @Query("SELECT * FROM cpt_records WHERE userId = :userId ORDER BY code ASC")
    fun observeByUser(userId: Long): Flow<List<CptRecordEntity>>

    @Query("SELECT DISTINCT year FROM cpt_records WHERE userId = :userId ORDER BY year DESC")
    fun observeYears(userId: Long): Flow<List<Int>>

    @Query("SELECT * FROM cpt_records WHERE eobId = :eobId ORDER BY code ASC")
    suspend fun getByEob(eobId: Long): List<CptRecordEntity>

    @Query("SELECT * FROM cpt_records WHERE userId = :userId AND category = :category AND year = :year ORDER BY code ASC")
    fun observeByCategory(userId: Long, category: String, year: Int): Flow<List<CptRecordEntity>>
}
