package app.eob.me.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.eob.me.data.local.entity.ProviderDirectoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderDirectoryDao {
    @Query("SELECT * FROM provider_directory ORDER BY displayName COLLATE NOCASE ASC")
    fun observeProviders(): Flow<List<ProviderDirectoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<ProviderDirectoryEntity>)
}
