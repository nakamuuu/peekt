package net.divlight.peekt.sample

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import net.divlight.peekt.Peekt
import net.divlight.peekt.core.HttpTransactionId

@Composable
fun SampleNavDisplay(
    peekt: Peekt,
    mainViewModel: MainViewModel,
) {
    val backStack = rememberNavBackStack(MainRoute)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<MainRoute> {
                MainScreen(
                    viewModel = mainViewModel,
                    onClickTransaction = { id ->
                        backStack.add(TransactionRoute(id.value))
                    },
                )
            }
            entry<TransactionRoute> { route ->
                val viewModel = viewModel<TransactionViewModel>(
                    factory = TransactionViewModel.Factory(
                        peekt = peekt,
                        transactionId = HttpTransactionId(route.id),
                    ),
                )
                TransactionScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
