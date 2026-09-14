package net.divlight.peekt

/**
 * Decides whether a request host should be recorded, based on [includedHosts].
 */
internal class HostFilter(
    private val includedHosts: Set<String>,
) {
    /**
     * Returns true when [includedHosts] is empty, or when [host] equals an entry or is a subdomain of one.
     *
     * Comparison is case-insensitive. A leading `.` on an entry is ignored. Blank entries are skipped.
     */
    fun shouldRecord(host: String): Boolean {
        if (includedHosts.isEmpty()) return true
        val normalizedHost = host.lowercase()
        return includedHosts.any { pattern ->
            val normalized = pattern.trim().trimStart('.').lowercase()
            if (normalized.isEmpty()) return@any false
            normalizedHost == normalized || normalizedHost.endsWith(".$normalized")
        }
    }
}
