package net.divlight.peekt.core

/**
 * Single HTTP header name/value pair after optional redaction.
 */
data class HttpHeader(
    val name: String,
    val value: String,
)
