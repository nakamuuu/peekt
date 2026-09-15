package net.divlight.peekt.http

import net.divlight.peekt.core.HttpBody

/**
 * Maps [HttpBody] to and from the columns stored on a transaction row.
 */
internal object HttpBodyCodec {
    const val KIND_TEXT = "text"
    const val KIND_BINARY = "binary"
    const val KIND_SKIPPED = "skipped"

    fun kind(body: HttpBody?): String? {
        return when (body) {
            is HttpBody.Text -> KIND_TEXT
            is HttpBody.Binary -> KIND_BINARY
            is HttpBody.Skipped -> KIND_SKIPPED
            null -> null
        }
    }

    fun text(body: HttpBody?): String? = (body as? HttpBody.Text)?.text

    fun bytes(body: HttpBody?): ByteArray? = (body as? HttpBody.Binary)?.bytes

    fun contentType(body: HttpBody?): String? = body?.contentType

    fun size(body: HttpBody?): Long? = body?.size

    /**
     * Rebuilds a [HttpBody] from stored columns. Unknown or blank [kind] yields `null`.
     */
    fun decode(
        kind: String?,
        text: String?,
        bytes: ByteArray?,
        contentType: String?,
        size: Long?,
    ): HttpBody? {
        return when (kind) {
            KIND_TEXT -> HttpBody.Text(contentType, size, text.orEmpty())
            KIND_BINARY -> HttpBody.Binary(contentType, size, bytes)
            KIND_SKIPPED -> HttpBody.Skipped(contentType, size)
            else -> null
        }
    }
}
