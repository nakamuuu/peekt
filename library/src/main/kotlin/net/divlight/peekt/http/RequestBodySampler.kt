package net.divlight.peekt.http

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.ByteString
import okio.Sink
import okio.Timeout
import okio.buffer
import kotlin.coroutines.cancellation.CancellationException

/**
 * Samples a bounded UTF-8 preview of an OkHttp [RequestBody] without retaining the full payload.
 *
 * [RequestBody.isOneShot] and [RequestBody.isDuplex] bodies are skipped so they can still be written once by the chain.
 * Non-textual content types are skipped so large binaries are not streamed through the interceptor.
 */
internal object RequestBodySampler {
    /**
     * Returns sampled text, truncated to [maxContentLength] bytes with an ellipsis when longer, or null when skipped.
     */
    fun sample(body: RequestBody, maxContentLength: Long): String? {
        if (body.isDuplex() || body.isOneShot()) return null
        if (!isTextual(body.contentType())) return null
        if (maxContentLength <= 0L) {
            return if (body.contentLength() == 0L) "" else "\n…"
        }
        return try {
            readLimitedUtf8(body, maxContentLength)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * True for media types that are reasonably decoded as UTF-8 text. A null type is treated as textual
     * so a limited sample still happens; only [maxContentLength] bytes are retained.
     */
    private fun isTextual(mediaType: MediaType?): Boolean {
        if (mediaType == null) return true
        val type = mediaType.type.lowercase()
        val subtype = mediaType.subtype.lowercase()
        return type == "text" ||
            subtype == "json" ||
            subtype == "xml" ||
            subtype == "html" ||
            subtype == "javascript" ||
            subtype == "x-www-form-urlencoded" ||
            subtype.endsWith("+json") ||
            subtype.endsWith("+xml")
    }

    private fun readLimitedUtf8(body: RequestBody, maxContentLength: Long): String {
        val preview = Buffer()
        var remaining = maxContentLength
        var truncated = false
        val sink = object : Sink {
            override fun write(source: Buffer, byteCount: Long) {
                if (remaining > 0L) {
                    val take = minOf(byteCount, remaining)
                    preview.write(source, take)
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
        sink.buffer().use { body.writeTo(it) }
        val captured = preview.readByteString()
        if (!truncated) return captured.utf8()
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
}
