package net.divlight.peekt

import net.divlight.peekt.core.HttpHeader
import okhttp3.Headers

/**
 * Serializes [okhttp3.Headers] to and from the multiline text format stored by the persistence layer.
 *
 * Each line is `name: value` followed by a newline. [decode] ignores lines without a colon after the first character.
 */
internal object HeadersTextCodec {
    /**
     * Builds a newline-terminated text representation of [headers], in declaration order.
     */
    fun encode(headers: Headers): String {
        return buildString {
            for (i in 0 until headers.size) {
                append(headers.name(i))
                append(": ")
                append(headers.value(i))
                append('\n')
            }
        }
    }

    /**
     * Parses [text] into [HttpHeader] entries.
     *
     * Blank [text] yields an empty list. Lines that do not contain `':'` after the first character are skipped.
     */
    fun decode(text: String): List<HttpHeader> {
        if (text.isBlank()) return emptyList()
        return text.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) null
                else HttpHeader(
                    name = line.substring(0, idx).trim(),
                    value = line.substring(idx + 1).trim(),
                )
            }
            .toList()
    }
}
