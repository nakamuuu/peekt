package net.divlight.peekt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import net.divlight.peekt.datastore.PeektDatabase
import net.divlight.peekt.core.PeektConfig
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
}
