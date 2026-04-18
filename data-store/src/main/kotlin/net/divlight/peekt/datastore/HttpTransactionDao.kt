package net.divlight.peekt.datastore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access for persisted [HttpTransactionEntity] rows.
 */
@Dao
interface HttpTransactionDao {
    /**
     * Inserts or replaces transaction row.
     *
     * @return The SQLite row id assigned to the inserted or replaced row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HttpTransactionEntity): Long

    /**
     * Updates an existing row identified by [HttpTransactionEntity.id].
     */
    @Update
    suspend fun update(entity: HttpTransactionEntity)

    /**
     * Observes all transactions, newest first (by [HttpTransactionEntity.startedAtMillis] descending).
     */
    @Query("SELECT * FROM http_transactions ORDER BY started_at_millis DESC")
    fun observeAll(): Flow<List<HttpTransactionEntity>>

    /**
     * Returns the transaction with the given id, or null if none exists.
     */
    @Query("SELECT * FROM http_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HttpTransactionEntity?

    /**
     * Removes every row from the http_transactions table.
     */
    @Query("DELETE FROM http_transactions")
    suspend fun deleteAll()
}
