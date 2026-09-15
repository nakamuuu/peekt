package net.divlight.peekt.datastore

import androidx.room.ColumnInfo

/**
 * List-row projection of [HttpTransactionEntity] without header or body columns.
 */
data class HttpTransactionSummary(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "method") val method: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "protocol") val protocol: String?,
    @ColumnInfo(name = "status_code") val statusCode: Int?,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    @ColumnInfo(name = "took_ms") val tookMs: Long?,
    @ColumnInfo(name = "error") val error: String?,
)
