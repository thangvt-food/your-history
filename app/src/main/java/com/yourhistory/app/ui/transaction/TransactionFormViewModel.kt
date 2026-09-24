package com.yourhistory.app.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourhistory.app.data.local.entity.CategoryEntity
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import com.yourhistory.app.data.local.preferences.UserPreferencesRepository
import com.yourhistory.app.data.repository.ExpenseRepository
import com.yourhistory.app.domain.handoff.BankingHandoffManager
import com.yourhistory.app.domain.model.BankInfo
import com.yourhistory.app.domain.model.VietnameseBanks
import com.yourhistory.app.domain.parser.VietQrBuilder
import com.yourhistory.app.ui.showqr.QrBitmapRenderer
import com.yourhistory.app.ui.showqr.QrImageSaver
import kotlinx.coroutines.Dispatchers
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
    data class ShowQr(
        val payload: String,
        val bankBin: String,
        val bankName: String,
        val accountNumber: String,
        val amount: Long,
        val memo: String,
        val senderPackage: String?,
        val senderBankName: String,
        val imageSaved: Boolean
    ) : TransactionFormEvent()
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
    val memoTags: List<String> = emptyList(),
    val isSaving: Boolean = false
)

class TransactionFormViewModel(
    private val repository: ExpenseRepository,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionFormState(memoTags = DEFAULT_MEMO_TAGS)
    )
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
        loadMemoTags()
    }

    private fun loadMemoTags() {
        viewModelScope.launch {
            preferences.customMemoTags.collect { custom ->
                val merged = (DEFAULT_MEMO_TAGS + custom).distinct()
                _uiState.value = _uiState.value.copy(memoTags = merged)
            }
        }
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
        // Chỉ giữ chữ số, bỏ mọi dấu cách/chấm phân cách khi nhập
        val filtered = amount.filter { it.isDigit() }.trimStart('0')
        _uiState.value = _uiState.value.copy(amountText = filtered)
    }

    fun onMemoChanged(memo: String) {
        _uiState.value = _uiState.value.copy(memo = memo)
    }

    /** Chạm tag -> điền nhanh vào nội dung CK: trống thì gán, có rồi thì nối thêm. */
    fun onMemoTagClicked(tag: String) {
        val current = _uiState.value.memo.trim()
        val newMemo = when {
            current.isEmpty() -> tag
            current.contains(tag, ignoreCase = true) -> current
            else -> "$current $tag"
        }
        _uiState.value = _uiState.value.copy(memo = newMemo)
    }

    /** Người dùng tự thêm tag mới, lưu bền vào DataStore để lần sau dùng tiếp. */
    fun onAddCustomTag(tag: String) {
        val clean = tag.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            preferences.addMemoTag(clean)
        }
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

    companion object {
        val DEFAULT_MEMO_TAGS = listOf(
            "Cơm trưa", "Cà phê", "Xăng xe", "Đi chợ",
            "Tiền nhà", "Trả nợ", "Ăn vặt", "Mua sắm"
        )

        /**
         * Định dạng chuỗi chữ số thành nhóm 3 số cách nhau bằng dấu cách
         * để dễ nhìn khi nhập: "1000000" -> "1 000 000".
         * Hàm thuần Kotlin, có thể unit test trên JVM.
         */
        fun formatAmountInput(digits: String): String {
            val clean = digits.filter { it.isDigit() }.trimStart('0')
            if (clean.isEmpty()) return ""
            val sb = StringBuilder()
            var count = 0
            for (i in clean.length - 1 downTo 0) {
                sb.append(clean[i])
                count++
                if (count == 3 && i != 0) {
                    sb.append(' ')
                    count = 0
                }
            }
            return sb.reverse().toString()
        }
    }

    /**
     * Luồng chuyển nhanh: dựng mã VietQR động (đủ tiền + nội dung),
     * LƯU ẢNH vào Thư viện để quét từ app bank, lưu giao dịch,
     * copy clipboard dự phòng, rồi mở màn hình hướng dẫn.
     */
    fun buildAndShowQr(context: Context, targetPackageName: String? = null) {
        val state = _uiState.value
        val amount = state.amountText.toLongOrNull() ?: 0L
        if (amount <= 0) {
            viewModelScope.launch {
                _events.emit(TransactionFormEvent.Error("Vui lòng nhập số tiền hợp lệ."))
            }
            return
        }
        if (state.bankBin.isBlank() || state.accountNumber.isBlank()) {
            viewModelScope.launch {
                _events.emit(TransactionFormEvent.Error("Thiếu thông tin tài khoản nhận."))
            }
            return
        }

        val payload = VietQrBuilder.build(
            bankBin = state.bankBin,
            accountNumber = state.accountNumber,
            amount = amount,
            memo = state.memo,
            recipientName = state.recipientName
        )
        if (payload.isNullOrBlank()) {
            viewModelScope.launch {
                _events.emit(TransactionFormEvent.Error("Không dựng được mã QR từ thông tin này."))
            }
            return
        }

        val senderBank = state.installedBanks.firstOrNull { it.packageName == targetPackageName }
        val senderName = senderBank?.shortName ?: "app ngân hàng"

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // 1. Render + lưu ảnh QR vào Thư viện (để quét từ app bank)
            val bitmap = QrBitmapRenderer.render(payload)
            val imageSaved = if (bitmap != null) {
                val fileName = "VietQR_${System.currentTimeMillis()}.png"
                QrImageSaver.saveToGallery(context, bitmap, fileName) != null
            } else false

            // 2. Lưu giao dịch + danh bạ
            persistTransaction(state, amount)

            // 3. Copy clipboard dự phòng (bank không hỗ trợ quét từ ảnh)
            BankingHandoffManager.copyTransferInfoToClipboard(
                context, state.accountNumber, amount, state.memo
            )

            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(
                TransactionFormEvent.ShowQr(
                    payload = payload,
                    bankBin = state.bankBin,
                    bankName = state.bankName,
                    accountNumber = state.accountNumber,
                    amount = amount,
                    memo = state.memo,
                    senderPackage = targetPackageName,
                    senderBankName = senderName,
                    imageSaved = imageSaved
                )
            )
        }
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            persistTransaction(state, amount)

            // Khởi chạy App Ngân hàng
            if (state.bankBin.isNotBlank() && state.accountNumber.isNotBlank()) {
                BankingHandoffManager.launchBankingHandoff(
                    context = context,
                    bankBin = state.bankBin,
                    accountNumber = state.accountNumber,
                    amount = amount,
                    memo = state.memo,
                    recipientName = state.recipientName,
                    targetPackageName = targetPackageName
                )
            }

            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.emit(TransactionFormEvent.TransactionSaved)
        }
    }

    /** Lưu Danh bạ QR (nếu tick) + Giao dịch vào Room. Dùng chung cho mọi luồng. */
    private suspend fun persistTransaction(state: TransactionFormState, amount: Long) {
        val categoryId = state.selectedCategory?.id ?: "cat_other_expense"

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
