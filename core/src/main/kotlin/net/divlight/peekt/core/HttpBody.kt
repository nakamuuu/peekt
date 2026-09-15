package net.divlight.peekt.core

/**
 * Captured request or response body metadata, plus payload when it can be shown as text or image bytes.
 *
 * Absence of a body is represented by `null` on [HttpTransactionMessage], not by a [HttpBody] subclass.
 */
sealed interface HttpBody {
    /**
     * Declared media type, from the body or the `Content-Type` header.
     */
    val contentType: String?

    /**
     * Declared content length in bytes when known; `null` when the source reports an unknown length.
     */
    val size: Long?

    /**
     * UTF-8 body text, truncated per [PeektConfig.maxContentLength] when longer.
     */
    data class Text(
        override val contentType: String?,
        override val size: Long?,
        val text: String,
    ) : HttpBody

    /**
     * Non-textual body. [bytes] is present only when an image payload fits in [PeektConfig.maxContentLength].
     */
    data class Binary(
        override val contentType: String?,
        override val size: Long?,
        val bytes: ByteArray?,
    ) : HttpBody {
        /**
         * True when image or other binary bytes were not retained.
         */
        val isOmitted: Boolean get() = bytes == null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Binary) return false
            if (contentType != other.contentType) return false
            if (size != other.size) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = contentType?.hashCode() ?: 0
            result = 31 * result + (size?.hashCode() ?: 0)
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    /**
     * Body exists but was not sampled, typically because it is one-shot or duplex.
     */
    data class Skipped(
        override val contentType: String?,
        override val size: Long?,
    ) : HttpBody
}
