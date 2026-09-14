package net.divlight.peekt

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
import okhttp3.OkHttpClient
import okhttp3.Request
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
}
