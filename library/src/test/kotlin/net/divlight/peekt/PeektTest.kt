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
        seedTransaction()
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
    fun create_keepsStoreWhenClearingNever() {
        seedTransaction()
        val peekt = Peekt.create(
            context,
            PeektConfig(clearingStrategy = ClearingStrategy.Never),
        )
        val rows = runBlocking { peekt.recorder.observeTransactions().first() }
        assertThat(rows).hasSize(1)
    }

    private fun seedTransaction() {
        val db = PeektDatabase.create(context)
        runBlocking {
            db.httpTransactionDao().insert(
                HttpTransactionEntity(
                    id = 0,
                    method = "GET",
                    url = "https://example.com/",
                    protocol = null,
                    requestHeadersText = "",
                    responseHeadersText = null,
                    requestBody = null,
                    responseBody = null,
                    statusCode = 200,
                    startedAtMillis = 1L,
                    tookMs = 1L,
                    error = null,
                ),
            )
        }
        db.close()
    }
}
