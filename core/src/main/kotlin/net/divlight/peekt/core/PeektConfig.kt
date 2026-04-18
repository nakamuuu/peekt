package net.divlight.peekt.core

/**
 * Configuration for the OkHttp interceptor that records traffic into Peekt.
 *
 * @property maxContentLength Maximum number of bytes to retain per request body, and maximum bytes
 *   read from the response via [okhttp3.Response.peekBody]. Longer content is truncated with an ellipsis suffix.
 * @property redactHeaderNames Header names (compared case-insensitively) whose values are replaced with `"**"`
 *   before persistence.
 */
data class PeektConfig(
    val maxContentLength: Long = 500_000L,
    val redactHeaderNames: Set<String> = emptySet(),
)
