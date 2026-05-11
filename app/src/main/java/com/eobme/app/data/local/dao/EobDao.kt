package com.eobme.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.eobme.app.data.local.entity.EobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eob: EobEntity): Long

    @Update
    suspend fun update(eob: EobEntity)

    @Query("SELECT * FROM eobs WHERE userId = :userId ORDER BY dateOfService ASC")
    fun observeByUser(userId: Long): Flow<List<EobEntity>>

    @Query("SELECT * FROM eobs WHERE userId = :userId ORDER BY dateOfService ASC")
    suspend fun getByUser(userId: Long): List<EobEntity>

    @Query("SELECT * FROM eobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EobEntity?

    @Query("SELECT * FROM eobs WHERE id = :id")
    fun observeById(id: Long): Flow<EobEntity?>

    @Query("DELETE FROM eobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM eobs 
        WHERE userId = :userId 
        AND imageUri = :imageUri 
        AND dateOfService = :dateOfService 
        AND providerName = :providerName
        LIMIT 1
    """)
    suspend fun findDuplicate(
        userId: Long,
        imageUri: String,
        dateOfService: Long,
        providerName: String
    ): EobEntity?
}
