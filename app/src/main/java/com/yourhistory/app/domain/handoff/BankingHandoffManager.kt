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
     * Attempts to launch the banking app.
     *
     * Thực tế tại VN: hầu hết app ngân hàng KHÔNG hỗ trợ điền sẵn form
     * chuyển khoản qua intent (ngoại trừ số ít xử lý scheme Napas VietQR).
     * Do đó thứ tự ưu tiên:
     *  1. Thử VietQR deep link có gắn [targetPackageName] (setPackage) —
     *     nếu bank có đăng ký scheme thì mở thẳng màn hình CK có sẵn thông tin.
     *  2. Nếu deep link không được xử lý, mở app trắng + hướng dẫn dán thủ công
     *     (thông tin đã copy vào Clipboard ở bước trước).
     *  3. Không chọn app cụ thể: thử deep link chung, rồi fallback mở app của
     *     ngân hàng nhận (BIN) nếu đã cài.
     */
    fun launchBankingHandoff(
        context: Context,
        bankBin: String,
        accountNumber: String,
        amount: Long?,
        memo: String,
        targetPackageName: String? = null
    ): Boolean {
        // Always copy transfer info to clipboard as safety fallback
        copyTransferInfoToClipboard(context, accountNumber, amount, memo)

        val deepLinkUri = buildVietQrDeepLinkUri(bankBin, accountNumber, amount, memo)

        // 1. User chọn app cụ thể: ưu tiên deep link gắn package
        if (!targetPackageName.isNullOrBlank()) {
            try {
                val targetedDeepLink = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(targetPackageName)
                }
                if (targetedDeepLink.resolveActivity(context.packageManager) != null) {
                    context.startActivity(targetedDeepLink)
                    Toast.makeText(
                        context,
                        "Đã mở app ngân hàng kèm thông tin chuyển khoản.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
            } catch (e: Exception) {
                // Deep link không được app đó xử lý -> fallback bên dưới
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Toast.makeText(
                    context,
                    "App ngân hàng không tự điền được — hãy dán STK, số tiền, nội dung từ Clipboard.",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }

        // 2. Try the primary VietQR deep link intent (không gắn package)
        try {
            val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (e: Exception) {
            // Deep link not handled
        }

        // 3. Fallback: mở app của ngân hàng nhận (tra theo BIN) nếu đã cài.
        // Thử deep link gắn package trước, rồi mới mở trắng.
        val bank = VietnameseBanks.findByBin(bankBin)
        if (bank?.packageName != null) {
            try {
                val bankDeepLink = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage(bank.packageName)
                }
                if (bankDeepLink.resolveActivity(context.packageManager) != null) {
                    context.startActivity(bankDeepLink)
                    return true
                }
            } catch (e: Exception) {
                // Bỏ qua, mở trắng bên dưới
            }
            val launchIntent = context.packageManager.getLaunchIntentForPackage(bank.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                Toast.makeText(
                    context,
                    "App ngân hàng không tự điền được — hãy dán STK, số tiền, nội dung từ Clipboard.",
                    Toast.LENGTH_LONG
                ).show()
                return true
            }
        }

        return false
    }
}
