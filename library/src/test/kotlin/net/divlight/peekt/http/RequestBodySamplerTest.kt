package net.divlight.peekt.http

import com.google.common.truth.Truth.assertThat
import net.divlight.peekt.core.HttpBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import okio.BufferedSink
import org.junit.Test

class RequestBodySamplerTest {
    @Test
    fun sample_returnsFullTextWhenWithinLimit() {
        val payload = """{"id":1}"""
        val body = payload.toRequestBody("application/json".toMediaType())
        val sampled = RequestBodySampler.sample(body, 500)
        assertThat(sampled).isInstanceOf(HttpBody.Text::class.java)
        sampled as HttpBody.Text
        assertThat(sampled.text).isEqualTo(payload)
        assertThat(sampled.contentType).contains("json")
        assertThat(sampled.size).isEqualTo(body.contentLength())
    }

    @Test
    fun sample_truncatesToMaxContentLength() {
        val body = "a".repeat(10_000).toRequestBody("text/plain".toMediaType())
        val sampled = RequestBodySampler.sample(body, 50) as HttpBody.Text
        assertThat(sampled.text).isEqualTo("a".repeat(50) + "\n…")
    }

    @Test
    fun prepare_replaysBodyThatCanBeWrittenOnlyOnce() {
        val payload = """{"title":"foo"}"""
        val consuming = ConsumingRequestBody(payload)
        val prepared = RequestBodySampler.prepare(consuming, 500)
        assertThat(prepared.sampled).isInstanceOf(HttpBody.Text::class.java)
        assertThat((prepared.sampled as HttpBody.Text).text).isEqualTo(payload)
        val replay = Buffer()
        prepared.outgoing.writeTo(replay)
        assertThat(replay.readUtf8()).isEqualTo(payload)
    }

    @Test
    fun sample_skipsOneShotBody() {
        val counting = CountingRequestBody(
            """{"ok":true}""".toRequestBody("application/json".toMediaType()),
            oneShot = true,
        )
        val sampled = RequestBodySampler.sample(counting, 500)
        assertThat(sampled).isInstanceOf(HttpBody.Skipped::class.java)
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    @Test
    fun sample_keepsSmallImageBytes() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val counting = CountingRequestBody(jpeg.toRequestBody("image/jpeg".toMediaType()))
        val sampled = RequestBodySampler.sample(counting, 500)
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        sampled as HttpBody.Binary
        assertThat(sampled.isOmitted).isFalse()
        assertThat(sampled.bytes).isEqualTo(jpeg)
        assertThat(sampled.contentType).isEqualTo("image/jpeg")
        assertThat(counting.writeToCount).isEqualTo(1)
    }

    @Test
    fun sample_omitsOversizedImageWithoutReading() {
        val counting = CountingRequestBody(
            ByteArray(1024).toRequestBody("image/jpeg".toMediaType()),
        )
        val sampled = RequestBodySampler.sample(counting, 500)
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        sampled as HttpBody.Binary
        assertThat(sampled.isOmitted).isTrue()
        assertThat(sampled.size).isEqualTo(1024)
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    @Test
    fun sample_omitsProtobufWithoutReading() {
        val counting = CountingRequestBody(
            ByteArray(32).toRequestBody("application/x-protobuf".toMediaType()),
        )
        val sampled = RequestBodySampler.sample(counting, 500)
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        assertThat((sampled as HttpBody.Binary).isOmitted).isTrue()
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    @Test
    fun sample_omitsGzipContentTypeWithoutReading() {
        val counting = CountingRequestBody(
            ByteArray(32).toRequestBody("application/gzip".toMediaType()),
        )
        val sampled = RequestBodySampler.sample(counting, 500)
        assertThat((sampled as HttpBody.Binary).isOmitted).isTrue()
        assertThat(counting.writeToCount).isEqualTo(0)
    }

    @Test
    fun sample_omitsCompressedEncodingWithoutReading() {
        val counting = CountingRequestBody(
            """{"ok":true}""".toRequestBody("application/json".toMediaType()),
        )
        val sampled = RequestBodySampler.sample(counting, 500, contentEncoding = "gzip")
        assertThat((sampled as HttpBody.Binary).isOmitted).isTrue()
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

    private class ConsumingRequestBody(
        payload: String,
    ) : RequestBody() {
        private val bytes = payload.encodeToByteArray()
        private var consumed = false

        override fun contentType() = "application/json".toMediaType()

        override fun contentLength() = bytes.size.toLong()

        override fun writeTo(sink: BufferedSink) {
            check(!consumed) { "already consumed" }
            consumed = true
            sink.write(bytes)
        }
    }
}
