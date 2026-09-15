package net.divlight.peekt.http

import okhttp3.MediaType

/**
 * Classifies request and response media types and content encodings for body sampling.
 */
internal object MediaTypes {
    /**
     * True for media types that are reasonably decoded as UTF-8 text. A null type is treated as textual
     * so a limited sample still happens.
     */
    fun isTextual(mediaType: MediaType?): Boolean {
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

    /**
     * True when [mediaType] is an image type.
     */
    fun isImage(mediaType: MediaType?): Boolean {
        if (mediaType == null) return false
        return mediaType.type.lowercase() == "image"
    }

    /**
     * True when [contentEncoding] names a compressed coding such as gzip or brotli.
     */
    fun isCompressedEncoding(contentEncoding: String?): Boolean {
        if (contentEncoding.isNullOrBlank()) return false
        return contentEncoding.split(',').any { token ->
            when (token.trim().lowercase()) {
                "gzip", "x-gzip", "br", "deflate", "zstd" -> true
                else -> false
            }
        }
    }
}
