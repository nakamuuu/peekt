package net.divlight.peekt.http

import net.divlight.peekt.core.HttpBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response

/**
 * Samples an OkHttp [Response] body as [HttpBody].
 *
 * Text is read via [Response.peekBody]. Image bytes are kept only when they fit in the size cap.
 * Other binaries are not peeked.
 */
internal object ResponseBodySampler {
    /**
     * Classifies the response payload and optionally peeks up to [maxContentLength] bytes.
     */
    fun sample(response: Response, maxContentLength: Long): HttpBody {
        val mediaType = response.body.contentType()
            ?: response.header("Content-Type")?.toMediaTypeOrNull()
        val contentType = mediaType?.toString()
        val size = response.body.contentLength().takeIf { it >= 0L }
        if (MediaTypes.isCompressedEncoding(response.header("Content-Encoding"))) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        if (MediaTypes.isImage(mediaType)) {
            return sampleImage(response, contentType, size, maxContentLength)
        }
        if (!MediaTypes.isTextual(mediaType)) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        val text = response.peekBody(maxContentLength.coerceAtLeast(0L)).string()
        return HttpBody.Text(contentType, size, text)
    }

    private fun sampleImage(
        response: Response,
        contentType: String?,
        size: Long?,
        maxContentLength: Long,
    ): HttpBody.Binary {
        if (maxContentLength <= 0L) {
            val bytes = if (size == 0L) ByteArray(0) else null
            return HttpBody.Binary(contentType, size, bytes)
        }
        if (size != null && size > maxContentLength) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        val peekCount = if (size != null) {
            maxContentLength
        } else {
            maxContentLength.saturatingInc()
        }
        val bytes = response.peekBody(peekCount).bytes()
        if (size == null && bytes.size > maxContentLength) {
            return HttpBody.Binary(contentType, size, bytes = null)
        }
        return HttpBody.Binary(contentType, size, bytes)
    }

    private fun Long.saturatingInc(): Long {
        return if (this == Long.MAX_VALUE) this else this + 1L
    }
}
