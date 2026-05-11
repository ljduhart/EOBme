package com.eobme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eobme.app.data.local.entity.InsuranceCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsuranceCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: InsuranceCardEntity): Long

    @Update
    suspend fun update(card: InsuranceCardEntity)

    @Query("SELECT * FROM insurance_cards WHERE userId = :userId LIMIT 1")
    fun observeByUser(userId: Long): Flow<InsuranceCardEntity?>

    @Query("SELECT * FROM insurance_cards WHERE userId = :userId LIMIT 1")
    suspend fun getByUser(userId: Long): InsuranceCardEntity?

    @Query("DELETE FROM insurance_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}
