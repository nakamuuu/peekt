package net.divlight.peekt.interceptor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.divlight.peekt.core.HttpBody
import net.divlight.peekt.core.PeektConfig
import net.divlight.peekt.datastore.HttpTransactionDao
import net.divlight.peekt.datastore.HttpTransactionEntity
import net.divlight.peekt.http.HeaderRedactor
import net.divlight.peekt.http.HeadersTextCodec
import net.divlight.peekt.http.HostFilter
import net.divlight.peekt.http.HttpBodyCodec
import net.divlight.peekt.http.RequestBodySampler
import net.divlight.peekt.http.ResponseBodySampler
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * OkHttp [Interceptor] that persists one row per request: insert before the network call, update on success or failure.
 *
 * Request and response bodies are classified as [HttpBody] up to [PeektConfig.maxContentLength]. When the
 * request body is read for sampling, a replayable copy is forwarded so a custom body that can be written
 * only once is not consumed before [Interceptor.Chain.proceed]. Database work runs on [Dispatchers.IO]
 * inside [runBlocking] because OkHttp interceptors are synchronous. Persistence failures are swallowed so
 * they never replace the chain result or the original network exception.
 */
internal class PeektInterceptor(
    private val dao: HttpTransactionDao,
    private val config: PeektConfig,
) : Interceptor {
    private val hostFilter = HostFilter(config.includedHosts)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!hostFilter.shouldRecord(request.url.host)) {
            return chain.proceed(request)
        }

        val startedAt = System.currentTimeMillis()
        val redactedRequestHeaders = HeaderRedactor.redact(request.headers, config.redactHeaderNames)
        val preparedBody = request.body?.let {
            RequestBodySampler.prepare(it, config.maxContentLength, request.header("Content-Encoding"))
        }
        val requestBody = preparedBody?.sampled
        val requestToProceed = when {
            preparedBody == null || preparedBody.outgoing === request.body -> request
            else -> request.newBuilder().method(request.method, preparedBody.outgoing).build()
        }
        val pending = transactionEntity(
            id = 0,
            request = request,
            requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
            requestBody = requestBody,
            responseHeadersText = null,
            responseBody = null,
            protocol = null,
            statusCode = null,
            startedAtMillis = startedAt,
            tookMs = null,
            error = null,
        )
        val id = persist {
            val insertedId = dao.insert(pending)
            val maxTransactions = config.maxTransactions
            if (maxTransactions != null) {
                dao.deleteAllExceptLatest(maxTransactions)
            }
            insertedId
        }
        return try {
            val response = chain.proceed(requestToProceed)
            if (id != null) {
                val tookMs = System.currentTimeMillis() - startedAt
                val redactedResponseHeaders = HeaderRedactor.redact(response.headers, config.redactHeaderNames)
                val responseBody = ResponseBodySampler.sample(response, config.maxContentLength)
                persist {
                    dao.update(
                        transactionEntity(
                            id = id,
                            request = request,
                            requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
                            requestBody = requestBody,
                            responseHeadersText = HeadersTextCodec.encode(redactedResponseHeaders),
                            responseBody = responseBody,
                            protocol = response.protocol.toString(),
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
                        transactionEntity(
                            id = id,
                            request = request,
                            requestHeadersText = HeadersTextCodec.encode(redactedRequestHeaders),
                            requestBody = requestBody,
                            responseHeadersText = null,
                            responseBody = null,
                            protocol = null,
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

    private fun transactionEntity(
        id: Long,
        request: Request,
        requestHeadersText: String,
        requestBody: HttpBody?,
        responseHeadersText: String?,
        responseBody: HttpBody?,
        protocol: String?,
        statusCode: Int?,
        startedAtMillis: Long,
        tookMs: Long?,
        error: String?,
    ): HttpTransactionEntity {
        return HttpTransactionEntity(
            id = id,
            method = request.method,
            url = request.url.toString(),
            protocol = protocol,
            requestHeadersText = requestHeadersText,
            responseHeadersText = responseHeadersText,
            requestBody = HttpBodyCodec.text(requestBody),
            requestBodyBytes = HttpBodyCodec.bytes(requestBody),
            requestContentType = HttpBodyCodec.contentType(requestBody),
            requestBodySize = HttpBodyCodec.size(requestBody),
            requestBodyKind = HttpBodyCodec.kind(requestBody),
            responseBody = HttpBodyCodec.text(responseBody),
            responseBodyBytes = HttpBodyCodec.bytes(responseBody),
            responseContentType = HttpBodyCodec.contentType(responseBody),
            responseBodySize = HttpBodyCodec.size(responseBody),
            responseBodyKind = HttpBodyCodec.kind(responseBody),
            statusCode = statusCode,
            startedAtMillis = startedAtMillis,
            tookMs = tookMs,
            error = error,
        )
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
}
