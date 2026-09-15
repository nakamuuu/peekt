package net.divlight.peekt.http

import com.google.common.truth.Truth.assertThat
import net.divlight.peekt.core.HttpBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class ResponseBodySamplerTest {
    @Test
    fun sample_returnsJsonText() {
        val payload = """{"id":1}"""
        val sampled = ResponseBodySampler.sample(
            response("application/json", payload.toByteArray()),
            500,
        )
        assertThat(sampled).isInstanceOf(HttpBody.Text::class.java)
        sampled as HttpBody.Text
        assertThat(sampled.text).isEqualTo(payload)
        assertThat(sampled.contentType).contains("json")
    }

    @Test
    fun sample_keepsSmallImageBytes() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val sampled = ResponseBodySampler.sample(response("image/jpeg", jpeg), 500)
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        sampled as HttpBody.Binary
        assertThat(sampled.isOmitted).isFalse()
        assertThat(sampled.bytes).isEqualTo(jpeg)
        assertThat(sampled.size).isEqualTo(jpeg.size.toLong())
    }

    @Test
    fun sample_omitsOversizedImageWithoutKeepingBytes() {
        val jpeg = ByteArray(1_000) { 1 }
        val sampled = ResponseBodySampler.sample(response("image/jpeg", jpeg), 50)
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        sampled as HttpBody.Binary
        assertThat(sampled.isOmitted).isTrue()
        assertThat(sampled.size).isEqualTo(1_000)
    }

    @Test
    fun sample_omitsProtobuf() {
        val sampled = ResponseBodySampler.sample(
            response("application/x-protobuf", "wire".toByteArray()),
            500,
        )
        assertThat(sampled).isInstanceOf(HttpBody.Binary::class.java)
        assertThat((sampled as HttpBody.Binary).isOmitted).isTrue()
    }

    @Test
    fun sample_omitsCompressedEncoding() {
        val response = response("application/json", """{"ok":true}""".toByteArray()).newBuilder()
            .header("Content-Encoding", "gzip")
            .build()
        val sampled = ResponseBodySampler.sample(response, 500)
        assertThat((sampled as HttpBody.Binary).isOmitted).isTrue()
    }

    private fun response(contentType: String, body: ByteArray): Response {
        val mediaType = contentType.toMediaType()
        return Response.Builder()
            .request(Request.Builder().url("http://example.com/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .header("Content-Type", contentType)
            .body(body.toResponseBody(mediaType))
            .build()
    }
}
