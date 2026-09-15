package net.divlight.peekt.interceptor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.divlight.peekt.core.PeektConfig
import net.divlight.peekt.datastore.HttpTransactionDao
import net.divlight.peekt.datastore.HttpTransactionEntity
import net.divlight.peekt.datastore.PeektDatabase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PeektInterceptorTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun intercept() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig())
        server.enqueue(MockResponse().setBody("""{"id":1}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).hasSize(1)
        val row = rows.single()
        assertThat(row.method).isEqualTo("GET")
        assertThat(row.url).contains("/posts/1")
        assertThat(row.statusCode).isEqualTo(200)
        assertThat(row.responseBody).contains("id")
        assertThat(row.tookMs).isNotNull()
    }

    @Test
    fun intercept_trimsToMaxTransactions() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig(maxTransactions = 2))
        repeat(3) { index ->
            server.enqueue(MockResponse().setBody("ok"))
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            val request = Request.Builder()
                .url(server.url("/posts/${index + 1}"))
                .get()
                .build()
            client.newCall(request).execute().close()
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).hasSize(2)
        assertThat(rows.map { it.url }).containsExactly(
            server.url("/posts/2").toString(),
            server.url("/posts/3").toString(),
        )
    }

    @Test
    fun intercept_skipsTrimWhenMaxTransactionsIsNull() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig(maxTransactions = null))
        repeat(3) {
            server.enqueue(MockResponse().setBody("ok"))
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            client.newCall(Request.Builder().url(server.url("/posts")).get().build()).execute().close()
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).hasSize(3)
    }

    @Test
    fun intercept_recordsWhenHostIsIncluded() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val host = server.url("/").host
        val interceptor = PeektInterceptor(dao, PeektConfig(includedHosts = setOf(host)))
        server.enqueue(MockResponse().setBody("""{"id":1}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).hasSize(1)
    }

    @Test
    fun intercept_skipsWhenHostIsNotIncluded() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(
            dao,
            PeektConfig(includedHosts = setOf("example.com")),
        )
        server.enqueue(MockResponse().setBody("""{"id":1}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).isEmpty()
    }

    @Test
    fun intercept_recordsPostBodyAndForwardsIt() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig())
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        val payload = """{"title":"foo"}"""
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts"))
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        assertThat(server.takeRequest().body.readUtf8()).isEqualTo(payload)
        val row = runBlocking { dao.observeAll().first() }.single()
        assertThat(row.requestBody).isEqualTo(payload)
        assertThat(row.requestBodyKind).isEqualTo("text")
    }

    @Test
    fun intercept_recordsImageResponseBytes() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig())
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "image/jpeg")
                .setBody(okio.Buffer().write(jpeg)),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        client.newCall(Request.Builder().url(server.url("/photo")).get().build()).execute().close()
        val row = runBlocking { dao.observeAll().first() }.single()
        assertThat(row.responseBodyKind).isEqualTo("binary")
        assertThat(row.responseBody).isNull()
        assertThat(row.responseBodyBytes).isEqualTo(jpeg)
        assertThat(row.responseContentType).contains("image/jpeg")
        assertThat(row.responseBodySize).isEqualTo(jpeg.size.toLong())
    }

    @Test
    fun intercept_omitsOversizedImageResponse() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig(maxContentLength = 50))
        val jpeg = ByteArray(200) { 1 }
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "image/jpeg")
                .setBody(okio.Buffer().write(jpeg)),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        client.newCall(Request.Builder().url(server.url("/photo")).get().build()).execute().close()
        val row = runBlocking { dao.observeAll().first() }.single()
        assertThat(row.responseBodyKind).isEqualTo("binary")
        assertThat(row.responseBodyBytes).isNull()
        assertThat(row.responseBodySize).isEqualTo(jpeg.size.toLong())
    }

    @Test
    fun intercept_omitsProtobufResponse() {
        val db = PeektDatabase.createInMemory(context)
        val dao = db.httpTransactionDao()
        val interceptor = PeektInterceptor(dao, PeektConfig())
        server.enqueue(
            MockResponse()
                .addHeader("Content-Type", "application/x-protobuf")
                .setBody("wire"),
        )
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        client.newCall(Request.Builder().url(server.url("/rpc")).get().build()).execute().close()
        val row = runBlocking { dao.observeAll().first() }.single()
        assertThat(row.responseBodyKind).isEqualTo("binary")
        assertThat(row.responseBody).isNull()
        assertThat(row.responseBodyBytes).isNull()
    }

    @Test
    fun intercept_proceedsWhenInsertFails() {
        val db = PeektDatabase.createInMemory(context)
        val dao = FailingHttpTransactionDao(
            delegate = db.httpTransactionDao(),
            insertException = RuntimeException("insert failed"),
        )
        val interceptor = PeektInterceptor(dao, PeektConfig())
        server.enqueue(MockResponse().setBody("""{"id":1}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        assertThat(server.takeRequest().path).isEqualTo("/posts/1")
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).isEmpty()
    }

    @Test
    fun intercept_returnsResponseWhenUpdateFails() {
        val db = PeektDatabase.createInMemory(context)
        val dao = FailingHttpTransactionDao(
            delegate = db.httpTransactionDao(),
            updateException = RuntimeException("update failed"),
        )
        val interceptor = PeektInterceptor(dao, PeektConfig())
        server.enqueue(MockResponse().setBody("""{"id":1}"""))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            assertThat(response.code).isEqualTo(200)
        }
        val rows = runBlocking { dao.observeAll().first() }
        assertThat(rows).hasSize(1)
        assertThat(rows.single().statusCode).isNull()
    }

    @Test
    fun intercept_rethrowsNetworkErrorWhenFailureUpdateFails() {
        val db = PeektDatabase.createInMemory(context)
        val dao = FailingHttpTransactionDao(
            delegate = db.httpTransactionDao(),
            updateException = RuntimeException("update failed"),
        )
        val interceptor = PeektInterceptor(dao, PeektConfig())
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        val request = Request.Builder()
            .url(server.url("/posts/1"))
            .get()
            .build()
        server.shutdown()
        try {
            client.newCall(request).execute()
            throw AssertionError("expected a network failure")
        } catch (e: Exception) {
            assertThat(e).isNotInstanceOf(RuntimeException::class.java)
            assertThat(e.message).isNotEqualTo("update failed")
        }
    }
}

private class FailingHttpTransactionDao(
    private val delegate: HttpTransactionDao,
    private val insertException: Exception? = null,
    private val updateException: Exception? = null,
) : HttpTransactionDao {
    override suspend fun insert(entity: HttpTransactionEntity): Long {
        insertException?.let { throw it }
        return delegate.insert(entity)
    }

    override suspend fun update(entity: HttpTransactionEntity) {
        updateException?.let { throw it }
        delegate.update(entity)
    }

    override fun observeAll(): Flow<List<HttpTransactionEntity>> = delegate.observeAll()

    override suspend fun getById(id: Long): HttpTransactionEntity? = delegate.getById(id)

    override suspend fun deleteAll() = delegate.deleteAll()

    override suspend fun deleteAllExceptLatest(keep: Int) = delegate.deleteAllExceptLatest(keep)
}
