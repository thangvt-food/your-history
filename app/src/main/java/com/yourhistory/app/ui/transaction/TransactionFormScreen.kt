package com.yourhistory.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.yourhistory.app.domain.model.BankInfo
import com.yourhistory.app.ui.home.formatCurrency
import com.yourhistory.app.ui.showqr.QrImageSaver
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    bankBin: String,
    accountNumber: String,
    amount: Long?,
    memo: String,
    recipient: String,
    contactId: String,
    onNavigateBack: () -> Unit,
    onTransactionSuccess: () -> Unit,
    onShowQr: (
        payload: String, bankBin: String, bankName: String, accountNumber: String,
        amount: Long, memo: String, senderPackage: String?, senderBankName: String,
        imageSaved: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showBankPickerSheet by remember { mutableStateOf(false) }

    // Android 8-9 cần quyền ghi để lưu ảnh QR vào Thư viện (Android 10+ thì không)
    var pendingSenderPkg by remember { mutableStateOf<String?>(null) }
    var showQrRequested by remember { mutableStateOf(false) }
    val writePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        if (showQrRequested) {
            viewModel.buildAndShowQr(context, pendingSenderPkg)
            showQrRequested = false
        }
    }
    fun requestShowQr(senderPkg: String?) {
        val needPerm = QrImageSaver.needsWritePermission() &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        if (needPerm) {
            pendingSenderPkg = senderPkg
            showQrRequested = true
            writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.buildAndShowQr(context, senderPkg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initData(
            context = context,
            bankBin = bankBin,
            accountNumber = accountNumber,
            amount = amount,
            memo = memo,
            recipient = recipient,
            contactId = contactId
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionFormEvent.TransactionSaved -> {
                    onTransactionSuccess()
                }
                is TransactionFormEvent.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
                is TransactionFormEvent.ShowBankPicker -> {
                    showBankPickerSheet = true
                }
                is TransactionFormEvent.ShowQr -> {
                    onShowQr(
                        event.payload,
                        event.bankBin,
                        event.bankName,
                        event.accountNumber,
                        event.amount,
                        event.memo,
                        event.senderPackage,
                        event.senderBankName,
                        event.imageSaved
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.bankBin.isNotBlank()) "Chuyển tiền & Chi tiêu" else "Ghi chép giao dịch",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thông tin tài khoản ngân hàng nhận (nếu có)
            if (uiState.bankBin.isNotBlank() || uiState.accountNumber.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.bankName.ifBlank { "Ngân hàng: ${uiState.bankBin}" },
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "STK: ${uiState.accountNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.recipientName.isNotBlank()) {
                                Text(
                                    "Người nhận: ${uiState.recipientName}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Nhập số tiền
            Column {
                Text(
                    "Số tiền (VNĐ)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = TransactionFormViewModel.formatAmountInput(uiState.amountText),
                    onValueChange = { viewModel.onAmountChanged(it.filter { c -> c.isDigit() }) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    supportingText = {
                        val raw = uiState.amountText.toLongOrNull() ?: 0L
                        if (raw > 0) Text("${formatCurrency(raw)}")
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Quick amount chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(20000L, 50000L, 100000L, 200000L, 500000L).forEach { quickVal ->
                        SuggestionChip(
                            onClick = {
                                val current = uiState.amountText.toLongOrNull() ?: 0L
                                viewModel.onAmountChanged((current + quickVal).toString())
                            },
                            label = { Text("+${quickVal / 1000}k", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Nội dung chi tiêu / Chuyển khoản + tag nhanh
            Column {
                OutlinedTextField(
                    value = uiState.memo,
                    onValueChange = { viewModel.onMemoChanged(it) },
                    label = { Text("Nội dung chuyển khoản / Ghi chú") },
                    placeholder = { Text("Ví dụ: Cơm trưa, Cà phê...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.memoTags.forEach { tag ->
                        SuggestionChip(
                            onClick = { viewModel.onMemoTagClicked(tag) },
                            label = { Text(tag, fontSize = 12.sp) }
                        )
                    }
                }
                var showAddTag by remember { mutableStateOf(false) }
                var newTagText by remember { mutableStateOf("") }
                if (showAddTag) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            placeholder = { Text("Tag mới...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Button(onClick = {
                            viewModel.onAddCustomTag(newTagText)
                            newTagText = ""
                            showAddTag = false
                        }) { Text("Thêm") }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddTag = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) { Text("+ Tự thêm tag", fontSize = 12.sp) }
                }
            }

            // Tên người nhận (nếu chưa có hoặc muốn sửa)
            if (uiState.bankBin.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.recipientName,
                    onValueChange = { viewModel.onRecipientChanged(it) },
                    label = { Text("Tên người nhận (gợi nhớ)") },
                    placeholder = { Text("Ví dụ: Quán cơm bà Năm, Anh Tuấn...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Checkbox lưu danh bạ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onSaveToContactsChanged(!uiState.saveToContacts) }
                ) {
                    Checkbox(
                        checked = uiState.saveToContacts,
                        onCheckedChange = { viewModel.onSaveToContactsChanged(it) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Lưu vào Danh bạ QR để chuyển nhanh lần sau",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            if (uiState.bankBin.isNotBlank()) {
                // Nút chính: chuyển nhanh — lưu ảnh QR + lịch sử, mở app bank quét từ ảnh
                Button(
                    onClick = {
                        if (uiState.installedBanks.size > 1) {
                            showBankPickerSheet = true
                        } else {
                            requestShowQr(uiState.installedBanks.firstOrNull()?.packageName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chuyển nhanh & Lưu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Nút phụ: Mở app ngân hàng & Lưu (deep link + clipboard)
                OutlinedButton(
                    onClick = {
                        if (uiState.installedBanks.size > 1) {
                            showBankPickerSheet = true
                        } else {
                            viewModel.transferAndSave(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mở app bank (dán thủ công)")
                }

                // Nút phụ: Chỉ lưu không mở ngân hàng
                OutlinedButton(
                    onClick = { viewModel.saveOnly() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chỉ lưu chi tiêu (không mở app ngân hàng)")
                }
            } else {
                // Chỉ lưu chi tiêu (nhập tay)
                Button(
                    onClick = { viewModel.saveOnly() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Lưu giao dịch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // BottomSheet chọn ngân hàng cụ thể nếu có nhiều app
        if (showBankPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBankPickerSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        "Chọn app ngân hàng của bạn (tài khoản chuyển đi)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "App dựng sẵn mã QR đủ số tiền + nội dung, lưu vào Thư viện ảnh và lưu lịch sử. Mở app bank → Quét QR → chọn ảnh từ Thư viện là xong, không cần gõ hay dán.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Không chọn app cụ thể: chỉ lưu ảnh + lịch sử, tự mở bank sau
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showBankPickerSheet = false
                                requestShowQr(null)
                            }
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Không mở app ngay (chỉ lưu ảnh QR + lịch sử)",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Danh sách các app ngân hàng tìm thấy trên máy (tài khoản chuyển đi)
                    uiState.installedBanks.forEach { bank ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showBankPickerSheet = false
                                    requestShowQr(bank.packageName)
                                }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(bank.shortName, fontWeight = FontWeight.Bold)
                                    Text(
                                        bank.fullName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
