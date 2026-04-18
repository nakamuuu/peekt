package net.divlight.peekt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.divlight.peekt.datastore.HttpTransactionDao
import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage
import net.divlight.peekt.core.PeektRecorder

/**
 * [PeektRecorder] implementation backed by [HttpTransactionDao].
 *
 * Domain objects are mapped from Room entities on each emission or lookup.
 */
internal class RealPeektRecorder(
    private val dao: HttpTransactionDao,
) : PeektRecorder {
    override fun observeTransactions(): Flow<List<HttpTransaction>> {
        return dao.observeAll().map { entities ->
            entities.map { TransactionEntityMapper.toHttpTransaction(it) }
        }
    }

    override suspend fun getTransactionMessage(id: HttpTransactionId): HttpTransactionMessage? {
        return dao.getById(id.value)?.let { TransactionEntityMapper.toHttpTransactionMessage(it) }
    }

    override suspend fun clear() {
        dao.deleteAll()
    }
}
