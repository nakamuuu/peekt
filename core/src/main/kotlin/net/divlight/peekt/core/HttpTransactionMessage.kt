package net.divlight.peekt.core

/**
 * [HttpTransaction] plus decoded headers and optional bodies for inspection UIs.
 *
 * @property requestBody Captured request body, or `null` when the request had no body.
 * @property responseBody Captured response body, or `null` when the exchange failed before a response.
 */
data class HttpTransactionMessage(
    val transaction: HttpTransaction,
    val requestHeaders: List<HttpHeader>,
    val responseHeaders: List<HttpHeader>,
    val requestBody: HttpBody?,
    val responseBody: HttpBody?,
)
