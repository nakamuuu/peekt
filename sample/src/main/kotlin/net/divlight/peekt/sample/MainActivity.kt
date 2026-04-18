package net.divlight.peekt.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import net.divlight.peekt.Peekt
import net.divlight.peekt.core.PeektConfig
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    private val peekt by lazy {
        Peekt.create(
            applicationContext,
            PeektConfig(
                redactHeaderNames = emptySet(),
            ),
        )
    }
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(peekt.interceptor())
            .build()
    }
    private val viewModel: MainViewModel by viewModels(
        factoryProducer = { MainViewModel.Factory(peekt, client) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PeektTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
