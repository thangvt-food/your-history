package com.yourhistory.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QrContactsUiState(
    val contacts: List<QrContactEntity> = emptyList(),
    val searchQuery: String = ""
)

class QrContactsViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrContactsUiState())
    val uiState: StateFlow<QrContactsUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            repository.getAllQrContacts().collect { list ->
                _uiState.value = _uiState.value.copy(contacts = list)
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun deleteContact(id: String) {
        viewModelScope.launch {
            repository.deleteQrContact(id)
        }
    }
}
