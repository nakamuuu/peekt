package net.divlight.peekt.sample

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.divlight.peekt.core.HttpBody
import net.divlight.peekt.core.HttpHeader
import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionContent(
        uiState = uiState,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionContent(
    uiState: TransactionUiState,
    onBack: () -> Unit,
) {
    val title = when (uiState) {
        is TransactionUiState.Loaded -> {
            val tx = uiState.message.transaction
            "${tx.method} ${tx.statusCode ?: "—"}"
        }
        TransactionUiState.Loading -> "Transaction"
        TransactionUiState.Missing -> "Transaction"
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        when (uiState) {
            TransactionUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            TransactionUiState.Missing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Transaction not found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is TransactionUiState.Loaded -> {
                TransactionContent(
                    message = uiState.message,
                    contentPadding = innerPadding,
                )
            }
        }
    }
}

@Composable
private fun TransactionContent(
    message: HttpTransactionMessage,
    contentPadding: PaddingValues,
) {
    val tx = message.transaction
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TransactionSectionTitle("Overview")
        }
        item {
            TransactionCard {
                TransactionMonospaceLine(tx.url)
                TransactionMetaLine(tx.protocol?.let { "Protocol $it" } ?: "No protocol")
                TransactionMetaLine(tx.tookMs?.let { "${it}ms" } ?: "In progress")
                tx.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        item {
            TransactionSectionTitle("Request headers")
        }
        item {
            TransactionHeaderCard(message.requestHeaders)
        }
        item {
            TransactionSectionTitle("Request body")
        }
        item {
            TransactionBodyCard(message.requestBody)
        }
        item {
            TransactionSectionTitle("Response headers")
        }
        item {
            TransactionHeaderCard(message.responseHeaders)
        }
        item {
            TransactionSectionTitle("Response body")
        }
        item {
            TransactionBodyCard(message.responseBody)
        }
    }
}

@Composable
private fun TransactionSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun TransactionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = { content() },
        )
    }
}

@Composable
private fun TransactionHeaderCard(headers: List<HttpHeader>) {
    TransactionCard {
        if (headers.isEmpty()) {
            TransactionMetaLine("No headers")
        } else {
            headers.forEach { header ->
                TransactionMonospaceLine("${header.name}: ${header.value}")
            }
        }
    }
}

@Composable
private fun TransactionBodyCard(body: HttpBody?) {
    TransactionCard {
        when (body) {
            null -> TransactionMetaLine("No body")
            is HttpBody.Text -> {
                TransactionBodyMetaLines(body.contentType, body.size)
                Text(
                    text = body.text,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            is HttpBody.Binary -> {
                TransactionBodyMetaLines(body.contentType, body.size)
                val bytes = body.bytes
                if (bytes == null) {
                    TransactionMetaLine("Bytes omitted")
                } else {
                    val image = remember(bytes) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = "Decoded image body",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        TransactionMetaLine("${bytes.size} bytes")
                    }
                }
            }
            is HttpBody.Skipped -> {
                TransactionBodyMetaLines(body.contentType, body.size)
                TransactionMetaLine("Skipped (one-shot or duplex)")
            }
        }
    }
}

@Composable
private fun TransactionBodyMetaLines(contentType: String?, size: Long?) {
    TransactionMetaLine(contentType ?: "No content type")
    TransactionMetaLine(size?.let { "$it bytes" } ?: "Unknown size")
}

@Composable
private fun TransactionMonospaceLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    )
}

@Composable
private fun TransactionMetaLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    PeektTheme {
        TransactionContent(
            uiState = TransactionUiState.Loaded(
                HttpTransactionMessage(
                    transaction = HttpTransaction(
                        id = HttpTransactionId(1L),
                        method = "POST",
                        url = "https://jsonplaceholder.typicode.com/posts",
                        protocol = "h2",
                        statusCode = 201,
                        startedAtMillis = 0L,
                        tookMs = 88L,
                        error = null,
                    ),
                    requestHeaders = listOf(
                        HttpHeader("Content-Type", "application/json; charset=utf-8"),
                    ),
                    responseHeaders = listOf(
                        HttpHeader("Content-Type", "application/json; charset=utf-8"),
                    ),
                    requestBody = HttpBody.Text(
                        contentType = "application/json; charset=utf-8",
                        size = 40L,
                        text = """{"title":"foo","body":"bar","userId":1}""",
                    ),
                    responseBody = HttpBody.Text(
                        contentType = "application/json; charset=utf-8",
                        size = 48L,
                        text = """{"id":101,"title":"foo","body":"bar","userId":1}""",
                    ),
                ),
            ),
            onBack = {},
        )
    }
}
