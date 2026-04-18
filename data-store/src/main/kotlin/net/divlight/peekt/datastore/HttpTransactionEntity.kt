package net.divlight.peekt.datastore

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted snapshot of a single HTTP exchange (request and response metadata and bodies).
 *
 * Rows are stored in the `http_transactions` table and ordered by [startedAtMillis] for listing.
 */
@Entity(
    tableName = "http_transactions",
    indices = [Index(value = ["started_at_millis"], name = "idx_http_transactions_started_at")],
)
data class HttpTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "method") val method: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "protocol") val protocol: String?,
    @ColumnInfo(name = "request_headers_text") val requestHeadersText: String,
    @ColumnInfo(name = "response_headers_text") val responseHeadersText: String?,
    @ColumnInfo(name = "request_body") val requestBody: String?,
    @ColumnInfo(name = "response_body") val responseBody: String?,
    @ColumnInfo(name = "status_code") val statusCode: Int?,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    @ColumnInfo(name = "took_ms") val tookMs: Long?,
    @ColumnInfo(name = "error") val error: String?,
)
