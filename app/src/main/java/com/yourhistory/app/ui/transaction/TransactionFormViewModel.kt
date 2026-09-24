package com.yourhistory.app.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourhistory.app.data.local.entity.CategoryEntity
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import com.yourhistory.app.data.repository.ExpenseRepository
import com.yourhistory.app.domain.handoff.BankingHandoffManager
import com.yourhistory.app.domain.model.BankInfo
import com.yourhistory.app.domain.model.VietnameseBanks
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TransactionFormEvent {
    data object TransactionSaved : TransactionFormEvent()
    data class ShowBankPicker(val availableBanks: List<BankInfo>) : TransactionFormEvent()
    data class Error(val message: String) : TransactionFormEvent()
}

data class TransactionFormState(
    val bankBin: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val recipientName: String = "",
    val amountText: String = "",
    val memo: String = "",
    val selectedType: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val selectedCategory: CategoryEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val saveToContacts: Boolean = true,
    val contactId: String? = null,
    val installedBanks: List<BankInfo> = emptyList(),
    val isSaving: Boolean = false
)

class TransactionFormViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionFormState())
    val uiState: StateFlow<TransactionFormState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TransactionFormEvent>()
    val events: SharedFlow<TransactionFormEvent> = _events.asSharedFlow()

    fun initData(
        context: Context,
        bankBin: String,
        accountNumber: String,
        amount: Long?,
        memo: String,
        recipient: String,
        contactId: String
    ) {
        val bankInfo = VietnameseBanks.findByBin(bankBin)
        val bankName = bankInfo?.shortName ?: if (bankBin.isNotBlank()) "Ngân hàng ($bankBin)" else ""

        val installed = BankingHandoffManager.getInstalledBankingApps(context)

        _uiState.value = _uiState.value.copy(
            bankBin = bankBin,
            bankName = bankName,
            accountNumber = accountNumber,
            amountText = if (amount != null && amount > 0) amount.toString() else "",
            memo = memo,
            recipientName = recipient,
            contactId = contactId.ifBlank { null },
            installedBanks = installed
        )

        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { list ->
                val filtered = list.filter { it.type == _uiState.value.selectedType }
                val defaultCat = filtered.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    categories = filtered,
                    selectedCategory = _uiState.value.selectedCategory ?: defaultCat
                )
            }
        }
    }

    fun onTypeChanged(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        loadCategories()
    }

    fun onAmountChanged(amount: String) {
        val filtered = amount.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(amountText = filtered)
    }

    fun onMemoChanged(memo: String) {
        _uiState.value = _uiState.value.copy(memo = memo)
    }

    fun onRecipientChanged(name: String) {
        _uiState.value = _uiState.value.copy(recipientName = name)
    }

    fun onCategorySelected(category: CategoryEntity) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onSaveToContactsChanged(save: Boolean) {
        _uiState.value = _uiState.value.copy(saveToContacts = save)
    }

    /**
     * Chuyển tiền qua App Ngân hàng & Lưu giao dịch vào máy
     */
    fun transferAndSave(context: Context, targetPackageName: String? = null) {
        val state = _uiState.value
        val amount = state.amountText.toLongOrNull() ?: 0L
        if (amount <= 0) {
            viewModelScope.launch {
                _events.emit(TransactionFormEvent.Error("Vui lòng nhập số tiền hợp lệ."))
            }
            return
        }

        val categoryId = state.selectedCategory?.id ?: "cat_other_expense"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // 1. Lưu hoặc cập nhật Danh bạ QR nếu người dùng tích chọn
            if (state.saveToContacts && state.bankBin.isNotBlank() && state.accountNumber.isNotBlank()) {
                val contact = QrContactEntity(
                    recipientName = state.recipientName.ifBlank { state.bankName },
                    bankBin = state.bankBin,
                    bankName = state.bankName,
                    accountNumber = state.accountNumber,
                    defaultAmount = amount,
                    defaultNote = state.memo,
                    defaultCategoryId = categoryId,
                    lastUsedAt = System.currentTimeMillis()
                )
                repository.saveQrContact(contact)
            } else if (!state.contactId.isNullOrBlank()) {
                repository.updateQrContactLastUsed(state.contactId)
            }

            // 2. Lưu Giao dịch vào Room Database
            val transaction = TransactionEntity(
                amount = amount,
                type = state.selectedType,
                categoryId = categoryId,
                note = state.memo.ifBlank { state.recipientName.ifBlank { state.bankName } },
                qrContactId = state.contactId,
                recipientName = state.recipientName,
                bankName = state.bankName,
                accountNumber = state.accountNumber,
                paymentMethod = if (state.bankBin.isNotBlank()) "VIETQR" else "CASH"
            )
            repository.insertTransaction(transaction)

            // 3. Khởi chạy App Ngân hàng
            if (state.bankBin.isNotBlank() && state.accountNumber.isNotBlank()) {
                BankingHandoffManager.launchBankingHandoff(
                    context = context,
                    bankBin = state.bankBin,
                    accountNumber = state.accountNumber,
                    amount = amount,
                    memo = state.memo,
                    targetPackageName = targetPackageName
                )
            }

            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(TransactionFormEvent.TransactionSaved)
        }
    }

    /**
     * Chỉ lưu giao dịch vào máy (không mở app ngân hàng)
     */
    fun saveOnly() {
        val state = _uiState.value
        val amount = state.amountText.toLongOrNull() ?: 0L
        if (amount <= 0) {
            viewModelScope.launch {
                _events.emit(TransactionFormEvent.Error("Vui lòng nhập số tiền hợp lệ."))
            }
            return
        }

        val categoryId = state.selectedCategory?.id ?: "cat_other_expense"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val transaction = TransactionEntity(
                amount = amount,
                type = state.selectedType,
                categoryId = categoryId,
                note = state.memo.ifBlank { state.recipientName.ifBlank { "Chi tiêu" } },
                recipientName = state.recipientName,
                bankName = state.bankName,
                accountNumber = state.accountNumber,
                paymentMethod = if (state.bankBin.isNotBlank()) "BANK_TRANSFER" else "CASH"
            )
            repository.insertTransaction(transaction)

            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(TransactionFormEvent.TransactionSaved)
        }
    }
}
