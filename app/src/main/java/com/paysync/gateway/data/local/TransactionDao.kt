package com.paysync.gateway.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPending(): List<TransactionEntity>
    @Query("UPDATE transactions SET status = :status, response = :response WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, response: String?)
    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
