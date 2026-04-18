package net.divlight.peekt.core

/**
 * [HttpTransaction] plus decoded headers and optional body text for inspection UIs.
 *
 * @property requestBody Captured request body text, truncated per [PeektConfig.maxContentLength] when applicable.
 * @property responseBody Captured response body text, truncated per [PeektConfig.maxContentLength] when applicable.
 */
data class HttpTransactionMessage(
    val transaction: HttpTransaction,
    val requestHeaders: List<HttpHeader>,
    val responseHeaders: List<HttpHeader>,
    val requestBody: String?,
    val responseBody: String?,
)
