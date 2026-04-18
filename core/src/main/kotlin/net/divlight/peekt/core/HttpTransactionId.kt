package net.divlight.peekt.core

/**
 * Stable identifier for a persisted HTTP transaction (matches the Room row primary key).
 */
@JvmInline
value class HttpTransactionId(val value: Long) {
    override fun toString(): String = value.toString()
}
