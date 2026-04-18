package net.divlight.peekt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.divlight.peekt.datastore.HttpTransactionDao
import net.divlight.peekt.datastore.HttpTransactionEntity
import net.divlight.peekt.core.PeektConfig
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.ByteString

/**
 * OkHttp [Interceptor] that persists one row per request: insert before the network call, update on success or failure.
 *
 * Request bodies are buffered so they can be captured and replayed to the chain. Response bodies are read via
 * [Response.peekBody] up to [PeektConfig.maxContentLength]. Database work runs on [Dispatchers.IO] inside
 * [runBlocking] because OkHttp interceptors are synchronous.
 */
internal class PeektInterceptor(
    private val dao: HttpTransactionDao,
    private val config: PeektConfig,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val startedAt = System.currentTimeMillis()
        var request = chain.request()
        val redactedRequestHeaders = HeaderRedactor.redact(request.headers, config.redactHeaderNames)
        var requestBodyText: String? = null
        val body = request.body
        if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            val full = buffer.readByteString()
            requestBodyText = utf8Preview(full, config.maxContentLength)
            val newBody = full.toRequestBody(body.contentType())
            request = request.newBuilder().method(request.method, newBody).build()
        }
        val pending = HttpTransactionEntity(
            id = 0,
            method = request.method,
            url = request.url.toString(),
            protocol = null,
            requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
            responseHeadersText = null,
            requestBody = requestBodyText,
            responseBody = null,
            statusCode = null,
            startedAtMillis = startedAt,
            tookMs = null,
            error = null,
        )
        val id = runBlocking(Dispatchers.IO) {
            dao.insert(pending)
        }
        return try {
            val response = chain.proceed(request)
            val tookMs = System.currentTimeMillis() - startedAt
            val redactedResponseHeaders = HeaderRedactor.redact(response.headers, config.redactHeaderNames)
            val responseBodyText = response.peekBody(config.maxContentLength).string()
            val updated = HttpTransactionEntity(
                id = id,
                method = request.method,
                url = request.url.toString(),
                protocol = response.protocol.toString(),
                requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
                responseHeadersText = HeadersTextCodec.encode(redactedResponseHeaders),
                requestBody = requestBodyText,
                responseBody = responseBodyText,
                statusCode = response.code,
                startedAtMillis = startedAt,
                tookMs = tookMs,
                error = null,
            )
            runBlocking(Dispatchers.IO) {
                dao.update(updated)
            }
            response
        } catch (e: Exception) {
            val tookMs = System.currentTimeMillis() - startedAt
            val failed = HttpTransactionEntity(
                id = id,
                method = request.method,
                url = request.url.toString(),
                protocol = null,
                requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
                responseHeadersText = null,
                requestBody = requestBodyText,
                responseBody = null,
                statusCode = null,
                startedAtMillis = startedAt,
                tookMs = tookMs,
                error = e.message,
            )
            runBlocking(Dispatchers.IO) {
                dao.update(failed)
            }
            throw e
        }
    }

    /**
     * Returns UTF-8 text for [bytes], truncated to [max] bytes with an ellipsis suffix when longer.
     */
    private fun utf8Preview(bytes: ByteString, max: Long): String {
        if (bytes.size <= max) {
            return bytes.utf8()
        }
        val limit = max.toInt().coerceAtLeast(0)
        return bytes.substring(0, limit).utf8() + "\n…"
    }
}
