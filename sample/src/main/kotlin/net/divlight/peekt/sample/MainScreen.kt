package net.divlight.peekt.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.divlight.peekt.core.HttpTransaction
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.sample.MainUiState.RequestKind

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MainContent(
        uiState = uiState,
        onClickGetButton = viewModel::runGetRequest,
        onClickPostButton = viewModel::runPostRequest,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    uiState: MainUiState,
    onClickGetButton: () -> Unit,
    onClickPostButton: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Peekt Sample")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MainRequestButton(
                        label = "GET /posts/1",
                        enabled = uiState.inFlightRequest == null,
                        inProgress = uiState.inFlightRequest == RequestKind.GetPost,
                        icon = Icons.Filled.CloudDownload,
                        onClick = onClickGetButton,
                    )
                    MainRequestButton(
                        label = "POST /posts",
                        enabled = uiState.inFlightRequest == null,
                        inProgress = uiState.inFlightRequest == RequestKind.PostPosts,
                        icon = Icons.Filled.CloudUpload,
                        onClick = onClickPostButton,
                    )
                }
            }
            item {
                Text(
                    text = "Last request",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                MainLastRequestCard(statusText = uiState.statusText)
            }
            item {
                Text(
                    text = "Recorded transactions",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (uiState.transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = uiState.transactions,
                    key = { it.id.value },
                ) { tx ->
                    MainTransactionCard(tx)
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MainLastRequestCard(statusText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Text(
            text = statusText.ifBlank { "—" },
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun MainRequestButton(
    label: String,
    enabled: Boolean,
    inProgress: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun MainTransactionCard(transaction: HttpTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = transaction.method,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = transaction.statusCode?.toString() ?: "—",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.tookMs?.let { "${it}ms" } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = transaction.url,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private class MainUiStatePreviewParameterProvider : PreviewParameterProvider<MainUiState> {
    override val values: Sequence<MainUiState> = sequenceOf(
        MainUiState(),
        MainUiState(
            statusText = "Loading…",
            inFlightRequest = RequestKind.GetPost,
        ),
        MainUiState(
            statusText = "Loading…",
            inFlightRequest = RequestKind.PostPosts,
        ),
        MainUiState(
            statusText = "HTTP 200\n{\"userId\":1,\"id\":1,\"title\":\"…\",\"body\":\"…\"}",
            transactions = listOf(
                HttpTransaction(
                    id = HttpTransactionId(1L),
                    method = "GET",
                    url = "https://jsonplaceholder.typicode.com/posts/1",
                    protocol = "h2",
                    statusCode = 200,
                    startedAtMillis = 0L,
                    tookMs = 42L,
                    error = null,
                )
            ),
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun MainContentPreview(
    @PreviewParameter(MainUiStatePreviewParameterProvider::class) uiState: MainUiState,
) {
    PeektTheme {
        MainContent(
            uiState = uiState,
            onClickGetButton = {},
            onClickPostButton = {},
        )
    }
}
