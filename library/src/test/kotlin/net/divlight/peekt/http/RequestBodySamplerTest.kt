package net.divlight.peekt.http

import com.google.common.truth.Truth.assertThat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.junit.Test

class RequestBodySamplerTest {
    @Test
    fun sample_returnsFullTextWhenWithinLimit() {
        val body = """{"id":1}""".toRequestBody("application/json".toMediaType())
        assertThat(RequestBodySampler.sample(body, 500)).isEqualTo("""{"id":1}""")
    }

    @Test
    fun sample_truncatesToMaxContentLength() {
        val body = "a".repeat(10_000).toRequestBody("text/plain".toMediaType())
        assertThat(RequestBodySampler.sample(body, 50)).isEqualTo("a".repeat(50) + "\n…")
    }

    @Test
    fun sample_skipsOneShotBody() {
        val counting = CountingRequestBody(
            """{"ok":true}""".toRequestBody("application/json".toMediaType()),
            oneShot = true,
        )
        assertThat(RequestBodySampler.sample(counting, 500)).isNull()
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    @Test
    fun sample_skipsBinaryContentType() {
        val counting = CountingRequestBody(
            ByteArray(1024).toRequestBody("image/jpeg".toMediaType()),
        )
        assertThat(RequestBodySampler.sample(counting, 500)).isNull()
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    private class CountingRequestBody(
        private val delegate: RequestBody,
        private val oneShot: Boolean = false,
    ) : RequestBody() {
        var writeToCount = 0
            private set

        override fun contentType() = delegate.contentType()

        override fun contentLength() = delegate.contentLength()

        override fun isOneShot() = oneShot

        override fun writeTo(sink: BufferedSink) {
            writeToCount++
            delegate.writeTo(sink)
        }
    }
}
