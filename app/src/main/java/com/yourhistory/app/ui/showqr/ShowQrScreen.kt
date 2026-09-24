package com.yourhistory.app.ui.showqr

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourhistory.app.domain.handoff.BankingHandoffManager
import com.yourhistory.app.ui.home.formatCurrency

/**
 * Màn hình sau khi bấm chuyển nhanh: ảnh mã VietQR động (đủ số tiền +
 * nội dung) đã được LƯU vào Thư viện. Người dùng mở app ngân hàng của mình,
 * chọn "Quét QR từ ảnh" và chọn ảnh vừa lưu — STK, số tiền, nội dung tự điền.
 * Giao dịch đã được lưu lịch sử trong app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowQrScreen(
    emvPayload: String,
    bankBin: String,
    bankName: String,
    accountNumber: String,
    amount: Long?,
    memo: String,
    senderPackage: String?,
    senderBankName: String,
    imageSaved: Boolean,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(emvPayload) { QrBitmapRenderer.render(emvPayload) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển nhanh qua QR", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trạng thái ảnh QR
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Mã VietQR đã lưu vào Thư viện",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (imageSaved) "Ảnh QR đã lưu vào Thư viện ảnh"
                        else "Không lưu được ảnh — hãy dán thông tin từ Clipboard",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (imageSaved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Tóm tắt để đối chiếu trong app bank
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        bankName.ifBlank { "Ngân hàng nhận" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("STK: $accountNumber", style = MaterialTheme.typography.bodyLarge)
                    if (amount != null && amount > 0) {
                        Text(
                            formatCurrency(amount),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    if (memo.isNotBlank()) {
                        Text(
                            "Nội dung: $memo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hướng dẫn quét từ ảnh
            Text(
                "1. Bấm nút MỞ $senderBankName bên dưới\n" +
                    "2. Chọn Quét QR / Chuyển tiền bằng QR\n" +
                    "3. Chọn quét từ Thư viện ảnh, chọn ảnh QR mới nhất\n" +
                    "4. STK, số tiền, nội dung tự điền — kiểm tra rồi xác nhận",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            )

            // Mở app ngân hàng người gửi
            if (!senderPackage.isNullOrBlank()) {
                Button(
                    onClick = { BankingHandoffManager.openAppPackage(context, senderPackage) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mở $senderBankName ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đã chuyển xong")
            }

            // Mở app nhận (tham khảo): deep link best-effort nếu muốn thử
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Không thấy mục quét từ ảnh? Hãy dán STK, số tiền, nội dung từ Clipboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
