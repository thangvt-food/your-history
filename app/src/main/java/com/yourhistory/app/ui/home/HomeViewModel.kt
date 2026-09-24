package com.yourhistory.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import com.yourhistory.app.data.repository.ExpenseRepository
import com.yourhistory.app.domain.model.FinancialSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val summary: FinancialSummary = FinancialSummary(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val recentContacts: List<QrContactEntity> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        viewModelScope.launch {
            repository.getFinancialSummary(startOfMonth, endOfMonth).collect { summary ->
                _uiState.value = _uiState.value.copy(summary = summary)
            }
        }

        viewModelScope.launch {
            repository.getRecentTransactions(5).collect { txs ->
                _uiState.value = _uiState.value.copy(recentTransactions = txs)
            }
        }

        viewModelScope.launch {
            repository.getRecentQrContacts(6).collect { contacts ->
                _uiState.value = _uiState.value.copy(recentContacts = contacts)
            }
        }
    }
}
