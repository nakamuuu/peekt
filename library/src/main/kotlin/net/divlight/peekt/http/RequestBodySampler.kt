package net.divlight.peekt.http

import net.divlight.peekt.core.HttpBody
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ByteString
import kotlin.coroutines.cancellation.CancellationException

/**
 * Sampled request body plus the [RequestBody] that should be sent on the chain.
 *
 * [outgoing] is a replayable copy when the original was read for sampling, so a custom body that can
 * be written only once is not consumed before [okhttp3.Interceptor.Chain.proceed].
 */
internal data class PreparedRequestBody(
    val sampled: HttpBody,
    val outgoing: RequestBody,
)

/**
 * Samples an OkHttp [RequestBody] as [HttpBody] without retaining more than the configured byte limit.
 *
 * [RequestBody.isOneShot] and [RequestBody.isDuplex] bodies become [HttpBody.Skipped] so they can still be written
 * once by the chain. Image payloads that fit are stored as bytes; other binaries keep metadata only.
 * When the original body is read, [prepare] replaces it with a replayable copy for the network call.
 */
internal object RequestBodySampler {
    /**
     * Classifies and optionally reads [body]. [contentEncoding] omits compressed payloads so they are not decoded as text.
     */
    fun sample(body: RequestBody, maxContentLength: Long, contentEncoding: String? = null): HttpBody {
        return prepare(body, maxContentLength, contentEncoding).sampled
    }

    /**
     * Samples [body] and returns the [RequestBody] to forward to the chain.
     */
    fun prepare(
        body: RequestBody,
        maxContentLength: Long,
        contentEncoding: String? = null,
    ): PreparedRequestBody {
        val mediaType = body.contentType()
        val contentType = mediaType?.toString()
        val size = body.contentLength().takeIf { it >= 0L }
        if (body.isDuplex() || body.isOneShot()) {
            return PreparedRequestBody(HttpBody.Skipped(contentType, size), body)
        }
        if (MediaTypes.isCompressedEncoding(contentEncoding)) {
            return PreparedRequestBody(HttpBody.Binary(contentType, size, bytes = null), body)
        }
        if (MediaTypes.isImage(mediaType)) {
            return prepareImage(body, contentType, size, maxContentLength)
        }
        if (!MediaTypes.isTextual(mediaType)) {
            return PreparedRequestBody(HttpBody.Binary(contentType, size, bytes = null), body)
        }
        if (maxContentLength <= 0L) {
            val text = if (body.contentLength() == 0L) "" else "\n…"
            return PreparedRequestBody(HttpBody.Text(contentType, size, text), body)
        }
        return readReplayable(body, contentType, size) { bytes ->
            HttpBody.Text(contentType, size, utf8Sample(bytes, maxContentLength))
        }
    }

    private fun prepareImage(
        body: RequestBody,
        contentType: String?,
        size: Long?,
        maxContentLength: Long,
    ): PreparedRequestBody {
        if (maxContentLength <= 0L) {
            val bytes = if (body.contentLength() == 0L) ByteArray(0) else null
            return PreparedRequestBody(HttpBody.Binary(contentType, size, bytes), body)
        }
        if (size != null && size > maxContentLength) {
            return PreparedRequestBody(HttpBody.Binary(contentType, size, bytes = null), body)
        }
        return readReplayable(body, contentType, size) { bytes ->
            if (bytes.size.toLong() > maxContentLength) {
                HttpBody.Binary(contentType, size, bytes = null)
            } else {
                HttpBody.Binary(contentType, size, bytes.toByteArray())
            }
        }
    }

    private fun readReplayable(
        body: RequestBody,
        contentType: String?,
        size: Long?,
        sample: (ByteString) -> HttpBody,
    ): PreparedRequestBody {
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            val captured = buffer.readByteString()
            PreparedRequestBody(sample(captured), ReplayableRequestBody(body, captured))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            PreparedRequestBody(HttpBody.Binary(contentType, size, bytes = null), body)
        }
    }

    private fun utf8Sample(bytes: ByteString, maxContentLength: Long): String {
        val take = minOf(bytes.size.toLong(), maxContentLength.coerceAtLeast(0L)).toInt()
        if (take == bytes.size) return bytes.utf8()
        return dropIncompleteUtf8(bytes.substring(0, take)).utf8() + "\n…"
    }

    /**
     * Shortens [bytes] so the last UTF-8 sequence is complete. [bytes] is assumed to be a prefix of a larger payload.
     */
    private fun dropIncompleteUtf8(bytes: ByteString): ByteString {
        var end = bytes.size
        while (end > 0) {
            val b = bytes[end - 1].toInt() and 0xFF
            when {
                b and 0x80 == 0 -> break
                b and 0xC0 == 0x80 -> end--
                else -> {
                    val expected = when {
                        b and 0xE0 == 0xC0 -> 2
                        b and 0xF0 == 0xE0 -> 3
                        b and 0xF8 == 0xF0 -> 4
                        else -> 1
                    }
                    val available = bytes.size - (end - 1)
                    if (available < expected) end--
                    break
                }
            }
        }
        return if (end == bytes.size) bytes else bytes.substring(0, end)
    }

    private class ReplayableRequestBody(
        private val original: RequestBody,
        private val bytes: ByteString,
    ) : RequestBody() {
        override fun contentType() = original.contentType()

        override fun contentLength() = bytes.size.toLong()

        override fun writeTo(sink: BufferedSink) {
            sink.write(bytes)
        }
    }
}
