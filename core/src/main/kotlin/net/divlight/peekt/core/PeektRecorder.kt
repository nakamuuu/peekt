package net.divlight.peekt.core

import kotlinx.coroutines.flow.Flow

/**
 * Read API for HTTP transactions captured by Peekt.
 */
interface PeektRecorder {
    /**
     * Emits the current list of recorded transactions whenever it changes, newest first.
     */
    fun observeTransactions(): Flow<List<HttpTransaction>>

    /**
     * Returns headers and bodies for the transaction with [id], or null if it does not exist.
     */
    suspend fun getTransactionMessage(id: HttpTransactionId): HttpTransactionMessage?

    /**
     * Deletes every persisted transaction.
     */
    suspend fun clear()
}
