package net.divlight.peekt

import net.divlight.peekt.datastore.HttpTransactionEntity
import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage

/**
 * Maps [HttpTransactionEntity] rows to core API types, including header text decoding.
 */
internal object TransactionEntityMapper {
    /**
     * Maps a stored row to summary [HttpTransaction] fields (no header or body payload).
     */
    fun toHttpTransaction(entity: HttpTransactionEntity): HttpTransaction {
        return HttpTransaction(
            id = HttpTransactionId(entity.id),
            method = entity.method,
            url = entity.url,
            protocol = entity.protocol,
            statusCode = entity.statusCode,
            startedAtMillis = entity.startedAtMillis,
            tookMs = entity.tookMs,
            error = entity.error,
        )
    }

    /**
     * Builds a full transaction message, decoding stored request/response header text into [HttpHeader] lists.
     */
    fun toHttpTransactionMessage(entity: HttpTransactionEntity): HttpTransactionMessage {
        return HttpTransactionMessage(
            transaction = toHttpTransaction(entity),
            requestHeaders = HeadersTextCodec.decode(entity.requestHeadersText),
            responseHeaders = entity.responseHeadersText?.let { HeadersTextCodec.decode(it) }.orEmpty(),
            requestBody = entity.requestBody,
            responseBody = entity.responseBody,
        )
    }
}
