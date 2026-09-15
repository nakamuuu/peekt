package net.divlight.peekt.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.divlight.peekt.Peekt
import net.divlight.peekt.core.HttpTransactionId
import net.divlight.peekt.core.HttpTransactionMessage

sealed interface TransactionUiState {
    data object Loading : TransactionUiState
    data class Loaded(val message: HttpTransactionMessage) : TransactionUiState
    data object Missing : TransactionUiState
}

class TransactionViewModel(
    private val peekt: Peekt,
    private val transactionId: HttpTransactionId,
) : ViewModel() {
    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Loading)
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peekt.recorder.observeTransactions().collect {
                val message = peekt.recorder.getTransactionMessage(transactionId)
                _uiState.value = if (message == null) {
                    TransactionUiState.Missing
                } else {
                    TransactionUiState.Loaded(message)
                }
            }
        }
    }

    class Factory(
        private val peekt: Peekt,
        private val transactionId: HttpTransactionId,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                return TransactionViewModel(peekt, transactionId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
