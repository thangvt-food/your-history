package com.yourhistory.app.domain.handoff

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.yourhistory.app.domain.model.BankInfo
import com.yourhistory.app.domain.model.VietnameseBanks
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

object BankingHandoffManager {

    /**
     * Copies account number, amount, and memo to the system clipboard
     * and shows a brief confirmation toast.
     */
    fun copyTransferInfoToClipboard(
        context: Context,
        accountNumber: String,
        amount: Long?,
        memo: String
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val formattedAmount = amount?.let {
            NumberFormat.getNumberInstance(Locale("vi", "VN")).format(it) + " đ"
        } ?: ""

        val clipText = buildString {
            append("STK: $accountNumber")
            if (formattedAmount.isNotEmpty()) {
                append("\nSố tiền: $formattedAmount")
            }
            if (memo.isNotBlank()) {
                append("\nNội dung: $memo")
            }
        }

        val clip = ClipData.newPlainText("Thông tin chuyển khoản", clipText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            context,
            "Đã sao chép thông tin chuyển khoản vào Clipboard!",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Builds the official VietQR Payment Deeplink URL:
     * https://dl.vietqr.io/pay?app={appId}&ba={accountNumber}@{bankBin}&am={amount}&tn={memo}&bn={recipientName}
     */
    fun buildVietQrPaymentDeeplink(
        appId: String,
        bankBin: String,
        accountNumber: String,
        amount: Long?,
        memo: String,
        recipientName: String? = null
    ): String {
        val encodedMemo = try {
            URLEncoder.encode(memo, "UTF-8")
        } catch (e: Exception) {
            memo
        }
        val recipientParam = if (!recipientName.isNullOrBlank()) {
            val encodedRecipient = try {
                URLEncoder.encode(recipientName, "UTF-8")
            } catch (e: Exception) {
                recipientName
            }
            "&bn=$encodedRecipient"
        } else ""
        val amountParam = if (amount != null && amount > 0) "&am=$amount" else ""
        return "https://dl.vietqr.io/pay?app=$appId&ba=$accountNumber@$bankBin$amountParam&tn=$encodedMemo$recipientParam"
    }

    /**
     * Trả về app ngân hàng ưu tiên nếu đã cài trên máy.
     * Ưu tiên MBBank nếu có, sau đó là app đầu tiên tìm thấy.
     */
    fun getPreferredBankingApp(context: Context): BankInfo? {
        val installed = getInstalledBankingApps(context)
        return installed.firstOrNull { it.vietQrAppId == "mb" } ?: installed.firstOrNull()
    }

    /**
     * Builds the Napas / VietQR deep link URI.
     */
    fun buildVietQrDeepLinkUri(
        bankBin: String,
        accountNumber: String,
        amount: Long?,
        memo: String
    ): Uri {
        val encodedMemo = try {
            URLEncoder.encode(memo, "UTF-8")
        } catch (e: Exception) {
            memo
        }
        val amountParam = if (amount != null && amount > 0) "&amount=$amount" else ""
        val url = "vietqr://transfer?bank=$bankBin&account=$accountNumber$amountParam&memo=$encodedMemo"
        return Uri.parse(url)
    }

    /**
     * Scans installed packages to find which Vietnamese banking apps are installed.
     */
    fun getInstalledBankingApps(context: Context): List<BankInfo> {
        val pm = context.packageManager
        return VietnameseBanks.ALL.filter { bank ->
            bank.packageName != null && isPackageInstalled(pm, bank.packageName)
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Mở app theo package, không copy clipboard, không deep link.
     * Dùng khi đã xử lý clipboard/ảnh QR ở tầng trên.
     */
    fun openAppPackage(context: Context, packageName: String): Boolean {
        return try {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Khởi chạy chuyển khoản trực tiếp sang app ngân hàng:
     *  1. Tự động sao chép STK, số tiền, nội dung vào Clipboard (phao cứu sinh 100%).
     *  2. Dựng VietQR Payment Deeplink chuẩn (dl.vietqr.io/pay?app=...) để mở thẳng
     *     màn hình thanh toán của app ngân hàng (MB Bank, VietinBank, BIDV, ACB...).
     *  3. Fallback: mở app bằng direct package launch nếu deeplink gặp lỗi.
     */
    fun launchBankingHandoff(
        context: Context,
        bankBin: String,
        accountNumber: String,
        amount: Long?,
        memo: String,
        recipientName: String? = null,
        targetPackageName: String? = null
    ): Boolean {
        // 1. Luôn sao chép thông tin vào Clipboard làm dự phòng an toàn
        copyTransferInfoToClipboard(context, accountNumber, amount, memo)

        // Xác định app ngân hàng mục tiêu (ưu tiên targetPackageName, sau đó là app ưu tiên đã cài)
        val targetBank = if (!targetPackageName.isNullOrBlank()) {
            VietnameseBanks.findByPackage(targetPackageName)
        } else {
            getPreferredBankingApp(context)
        }

        // 2. Nếu tìm được app có VietQR appId (ví dụ 'mb' cho MB Bank) -> mở qua Payment Deeplink
        if (targetBank?.vietQrAppId != null) {
            val paymentDeeplink = buildVietQrPaymentDeeplink(
                appId = targetBank.vietQrAppId,
                bankBin = bankBin,
                accountNumber = accountNumber,
                amount = amount,
                memo = memo,
                recipientName = recipientName
            )
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentDeeplink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "Đang mở ${targetBank.shortName}... Đã sao chép STK & nội dung!",
                    Toast.LENGTH_SHORT
                ).show()
                return true
            } catch (e: Exception) {
                // Fallback sang mở trực tiếp package bên dưới
            }
        }

        // 3. Fallback: Nếu không mở được deeplink hoặc không có appId, mở app trắng theo package
        val fallbackPackage = targetBank?.packageName ?: targetPackageName
        if (!fallbackPackage.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(fallbackPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Toast.makeText(
                    context,
                    "Đã mở ${targetBank?.shortName ?: "app ngân hàng"}. Hãy dán STK & nội dung từ Clipboard.",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }

        // 4. Nếu không có app nào được cài, thử mở deep link Napas chung
        try {
            val genericUri = buildVietQrDeepLinkUri(bankBin, accountNumber, amount, memo)
            val genericIntent = Intent(Intent.ACTION_VIEW, genericUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (genericIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(genericIntent)
                return true
            }
        } catch (e: Exception) {
            // Không mở được
        }

        return false
    }
}

