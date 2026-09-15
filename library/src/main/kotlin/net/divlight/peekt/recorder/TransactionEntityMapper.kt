package net.divlight.peekt.recorder

import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage
import net.divlight.peekt.datastore.HttpTransactionEntity
import net.divlight.peekt.http.HeadersTextCodec
import net.divlight.peekt.http.HttpBodyCodec

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
            requestBody = HttpBodyCodec.decode(
                kind = entity.requestBodyKind,
                text = entity.requestBody,
                bytes = entity.requestBodyBytes,
                contentType = entity.requestContentType,
                size = entity.requestBodySize,
            ),
            responseBody = HttpBodyCodec.decode(
                kind = entity.responseBodyKind,
                text = entity.responseBody,
                bytes = entity.responseBodyBytes,
                contentType = entity.responseContentType,
                size = entity.responseBodySize,
            ),
        )
    }
}
