package net.divlight.peekt.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.divlight.peekt.Peekt
import net.divlight.peekt.core.HttpTransaction
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class MainUiState(
    val statusText: String = "",
    val inFlightRequest: RequestKind? = null,
    val transactions: List<HttpTransaction> = emptyList(),
) {
    enum class RequestKind {
        GetPost,
        PostPosts,
    }
}

class MainViewModel(
    private val peekt: Peekt,
    private val client: OkHttpClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peekt.recorder.observeTransactions().collect { transactions ->
                _uiState.update { it.copy(transactions = transactions) }
            }
        }
    }

    fun runGetRequest() {
        runRequest(MainUiState.RequestKind.GetPost) {
            val req = Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts/1")
                .get()
                .build()
            client.newCall(req).execute().use { response ->
                "HTTP ${response.code}\n${response.body.string()}"
            }
        }
    }

    fun runPostRequest() {
        runRequest(MainUiState.RequestKind.PostPosts) {
            val body = """{"title":"foo","body":"bar","userId":1}""".toRequestBody(
                "application/json; charset=utf-8".toMediaType(),
            )
            val req = Request.Builder()
                .url("https://jsonplaceholder.typicode.com/posts")
                .post(body)
                .build()
            client.newCall(req).execute().use { response ->
                "HTTP ${response.code}\n${response.body.string()}"
            }
        }
    }

    private fun runRequest(kind: MainUiState.RequestKind, block: suspend () -> String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(statusText = "Loading…", inFlightRequest = kind)
            }
            try {
                val msg = withContext(Dispatchers.IO) { block() }
                _uiState.update { it.copy(statusText = msg, inFlightRequest = null) }
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(
                        statusText = e.message ?: e.toString(),
                        inFlightRequest = null,
                    )
                }
            }
        }
    }

    class Factory(
        private val peekt: Peekt,
        private val client: OkHttpClient,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(peekt, client) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
