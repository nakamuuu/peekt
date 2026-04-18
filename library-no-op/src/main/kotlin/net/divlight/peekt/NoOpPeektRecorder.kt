package net.divlight.peekt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage
import net.divlight.peekt.core.PeektRecorder

/**
 * No-op [PeektRecorder] that does not persist data to storage or retain it in memory.
 */
internal object NoOpPeektRecorder : PeektRecorder {
    override fun observeTransactions(): Flow<List<HttpTransaction>> {
        return flowOf(emptyList())
    }

    override suspend fun getTransactionMessage(id: HttpTransactionId): HttpTransactionMessage? {
        return null
    }

    override suspend fun clear() {
    }
}
