package net.divlight.peekt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.divlight.peekt.core.ClearingStrategy
import net.divlight.peekt.core.PeektConfig
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
class PeektTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        context.deleteDatabase("peekt.db")
    }

    @Test
    fun create_clearsStoreOnLaunch() {
        seedTransaction(startedAtMillis = 1L, url = PREVIOUS_PROCESS_URL)
        val peekt = Peekt.create(
            context,
            PeektConfig(clearingStrategy = ClearingStrategy.OnLaunch),
        )
        val rows = runBlocking {
            withTimeout(5_000) {
                peekt.recorder.observeTransactions().first { it.isEmpty() }
            }
        }
        assertThat(rows).isEmpty()
    }

    @Test
    fun create_onLaunchKeepsRowsStartedAfterProcessStart() {
        seedTransaction(startedAtMillis = 1L, url = PREVIOUS_PROCESS_URL)
        seedTransaction(startedAtMillis = System.currentTimeMillis(), url = THIS_PROCESS_URL)
        val peekt = Peekt.create(
            context,
            PeektConfig(clearingStrategy = ClearingStrategy.OnLaunch),
        )
        val rows = runBlocking {
            withTimeout(5_000) {
                peekt.recorder.observeTransactions().first { list ->
                    list.none { it.url == PREVIOUS_PROCESS_URL } &&
                        list.any { it.url == THIS_PROCESS_URL }
                }
            }
        }
        assertThat(rows.map { it.url }).containsExactly(THIS_PROCESS_URL)
    }

    @Test
    fun create_onLaunchDoesNotDeleteRequestRecordedAfterCreate() {
        seedTransaction(startedAtMillis = 1L, url = PREVIOUS_PROCESS_URL)
        val peekt = Peekt.create(
            context,
            PeektConfig(clearingStrategy = ClearingStrategy.OnLaunch),
        )
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            val client = OkHttpClient.Builder()
                .addInterceptor(peekt.interceptor())
                .build()
            val requestUrl = server.url("/after-create").toString()
            client.newCall(Request.Builder().url(requestUrl).get().build()).execute().close()
            val rows = runBlocking {
                withTimeout(5_000) {
                    peekt.recorder.observeTransactions().first { list ->
                        list.none { it.url == PREVIOUS_PROCESS_URL } &&
                            list.any { it.url == requestUrl }
                    }
                }
            }
            assertThat(rows.map { it.url }).containsExactly(requestUrl)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun create_keepsStoreWhenClearingNever() {
        seedTransaction()
        val peekt = Peekt.create(
            context,
            PeektConfig(clearingStrategy = ClearingStrategy.Never),
        )
        val rows = runBlocking { peekt.recorder.observeTransactions().first() }
        assertThat(rows).hasSize(1)
    }

    private fun seedTransaction(
        startedAtMillis: Long = 1L,
        url: String = PREVIOUS_PROCESS_URL,
    ) {
        val db = PeektDatabase.create(context)
        runBlocking {
            db.httpTransactionDao().insert(
                HttpTransactionEntity(
                    id = 0,
                    method = "GET",
                    url = url,
                    protocol = null,
                    requestHeadersText = "",
                    responseHeadersText = null,
                    requestBody = null,
                    responseBody = null,
                    statusCode = 200,
                    startedAtMillis = startedAtMillis,
                    tookMs = 1L,
                    error = null,
                ),
            )
        }
        db.close()
    }

    private companion object {
        const val PREVIOUS_PROCESS_URL = "https://example.com/previous"
        const val THIS_PROCESS_URL = "https://example.com/current"
    }
}
