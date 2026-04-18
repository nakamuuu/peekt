package net.divlight.peekt

import okhttp3.Headers

/**
 * Replaces sensitive header values before they are persisted or displayed.
 *
 * Matching is case-insensitive on the header name; values for matched names become a fixed placeholder.
 */
internal object HeaderRedactor {
    /**
     * Returns a new [Headers] instance with redacted values for names present in [redactNames].
     *
     * When [redactNames] is empty, [headers] is returned as-is.
     */
    fun redact(headers: Headers, redactNames: Set<String>): Headers {
        if (redactNames.isEmpty()) return headers
        val lower = redactNames.map { it.lowercase() }.toSet()
        val builder = Headers.Builder()
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val value = if (name.lowercase() in lower) "**" else headers.value(i)
            builder.add(name, value)
        }
        return builder.build()
    }
}
