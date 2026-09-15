package net.divlight.peekt.core

/**
 * Configuration for the OkHttp interceptor that records traffic into Peekt.
 *
 * @property maxTransactions Maximum number of stored transactions to keep after each insert, newest first.
 *   `null` disables this cap.
 * @property clearingStrategy When to delete stored transactions automatically.
 * @property includedHosts Hosts to record (compared case-insensitively). A request is recorded when its host
 *   equals an entry or is a subdomain of one. A leading `.` on an entry is ignored. Empty records every host.
 * @property maxContentLength Maximum number of bytes to retain per textual body, and the size cap for
 *   stored image bytes. Longer text is truncated with an ellipsis. Images over this limit, and other
 *   binaries, keep metadata only.
 * @property redactHeaderNames Header names (compared case-insensitively) whose values are replaced with `"**"`
 *   before persistence.
 */
data class PeektConfig(
    val maxTransactions: Int? = 100,
    val clearingStrategy: ClearingStrategy = ClearingStrategy.Never,
    val includedHosts: Set<String> = emptySet(),
    val maxContentLength: Long = 500_000L,
    val redactHeaderNames: Set<String> = emptySet(),
)
