package net.divlight.peekt

import kotlinx.coroutines.CancellationException
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
 * [runBlocking] because OkHttp interceptors are synchronous. Persistence failures are swallowed so they never
 * replace the chain result or the original network exception.
 */
internal class PeektInterceptor(
    private val dao: HttpTransactionDao,
    private val config: PeektConfig,
) : Interceptor {
    private val hostFilter = HostFilter(config.includedHosts)

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (!hostFilter.shouldRecord(request.url.host)) {
            return chain.proceed(request)
        }

        val startedAt = System.currentTimeMillis()
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
        val id = persist { dao.insert(pending) }
        return try {
            val response = chain.proceed(request)
            if (id != null) {
                val tookMs = System.currentTimeMillis() - startedAt
                val redactedResponseHeaders = HeaderRedactor.redact(response.headers, config.redactHeaderNames)
                val responseBodyText = response.peekBody(config.maxContentLength).string()
                persist {
                    dao.update(
                        HttpTransactionEntity(
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
                        ),
                    )
                }
            }
            response
        } catch (e: Exception) {
            if (id != null) {
                val tookMs = System.currentTimeMillis() - startedAt
                persist {
                    dao.update(
                        HttpTransactionEntity(
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
                        ),
                    )
                }
            }
            throw e
        }
    }

    /**
     * Runs [block] on [Dispatchers.IO]. Returns null when persistence throws so the HTTP chain is unaffected.
     */
    private inline fun <T> persist(crossinline block: suspend () -> T): T? {
        return try {
            runBlocking(Dispatchers.IO) { block() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
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
