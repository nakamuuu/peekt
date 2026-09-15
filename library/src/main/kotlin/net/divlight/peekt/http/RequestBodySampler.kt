package net.divlight.peekt.http

import net.divlight.peekt.core.HttpBody
import okhttp3.RequestBody
import okio.Buffer
import okio.ByteString
import okio.Sink
import okio.Timeout
import okio.buffer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Samples an OkHttp [RequestBody] as [HttpBody] without retaining more than the configured byte limit.
 *
 * [RequestBody.isOneShot] and [RequestBody.isDuplex] bodies become [HttpBody.Skipped] so they can still be written
 * once by the chain. Image payloads that fit are stored as bytes; other binaries keep metadata only.
 */
internal object RequestBodySampler {
    /**
     * Classifies and optionally reads [body]. [contentEncoding] omits compressed payloads so they are not decoded as text.
     */
    fun sample(body: RequestBody, maxContentLength: Long, contentEncoding: String? = null): HttpBody {
        val contentType = body.contentType()?.toString()
        val size = body.contentLength().takeIf { it >= 0L }
        if (body.isDuplex() || body.isOneShot()) {
            return HttpBody.Skipped(contentType, size)
        }
        if (MediaTypes.isCompressedEncoding(contentEncoding)) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        if (MediaTypes.isImage(body.contentType())) {
            return sampleImage(body, contentType, size, maxContentLength)
        }
        if (!MediaTypes.isTextual(body.contentType())) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        if (maxContentLength <= 0L) {
            val text = if (body.contentLength() == 0L) "" else "\n…"
            return HttpBody.Text(contentType, size, text)
        }
        return try {
            HttpBody.Text(contentType, size, readLimitedUtf8(body, maxContentLength))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            HttpBody.Binary(contentType, size, bytes = null)
        }
    }

    private fun sampleImage(
        body: RequestBody,
        contentType: String?,
        size: Long?,
        maxContentLength: Long,
    ): HttpBody.Binary {
        if (maxContentLength <= 0L) {
            val bytes = if (body.contentLength() == 0L) ByteArray(0) else null
            return HttpBody.Binary(contentType, size, bytes)
        }
        if (size != null && size > maxContentLength) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        return try {
            HttpBody.Binary(contentType, size, readLimitedBytes(body, maxContentLength))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            HttpBody.Binary(contentType, size, bytes = null)
        }
    }

    private fun readLimitedBytes(body: RequestBody, maxContentLength: Long): ByteArray? {
        val preview = LimitedPreview(maxContentLength)
        preview.sink.buffer().use { body.writeTo(it) }
        return preview.bytesOrNull()
    }

    private fun readLimitedUtf8(body: RequestBody, maxContentLength: Long): String {
        val preview = LimitedPreview(maxContentLength)
        preview.sink.buffer().use { body.writeTo(it) }
        val captured = preview.buffer.readByteString()
        if (!preview.truncated) return captured.utf8()
        return dropIncompleteUtf8(captured).utf8() + "\n…"
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

    private class LimitedPreview(maxContentLength: Long) {
        val buffer = Buffer()
        var remaining = maxContentLength
        var truncated = false
        val sink = object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                if (remaining > 0L) {
                    val take = minOf(byteCount, remaining)
                    buffer.write(source, take)
                    remaining -= take
                    if (byteCount > take) {
                        truncated = true
                        source.skip(byteCount - take)
                    }
                } else {
                    truncated = true
                    source.skip(byteCount)
                }
            }

            override fun flush() = Unit

            override fun timeout() = Timeout.NONE

            override fun close() = Unit
        }

        fun bytesOrNull(): ByteArray? {
            return if (truncated) null else buffer.readByteArray()
        }
    }
}
