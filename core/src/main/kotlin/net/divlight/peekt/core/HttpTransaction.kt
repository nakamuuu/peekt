package net.divlight.peekt.core

/**
 * Summary of one recorded HTTP round-trip, suitable for list UIs.
 *
 * Full headers and bodies are available via [PeektRecorder.getTransactionMessage] as [HttpTransactionMessage].
 *
 * @property id Stable row id for this transaction; use with [PeektRecorder.getTransactionMessage].
 * @property protocol Response protocol when available; null if the exchange failed before a response.
 * @property statusCode HTTP status when a response was received; null on network or pre-response failure.
 * @property tookMs Elapsed time from start to completion or failure, in milliseconds.
 * @property error Exception message when the request failed; null on success.
 */
data class HttpTransaction(
    val id: HttpTransactionId,
    val method: String,
    val url: String,
    val protocol: String?,
    val statusCode: Int?,
    val startedAtMillis: Long,
    val tookMs: Long?,
    val error: String?,
)
