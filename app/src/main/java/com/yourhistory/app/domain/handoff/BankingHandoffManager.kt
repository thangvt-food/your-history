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
     * Attempts to launch the banking app.
     * If targetPackageName is provided, opens that app directly.
     * Otherwise tries the VietQR deep link.
     * In both cases, copies details to clipboard as safety fallback.
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

        // 1. If user selected a specific bank app package
        if (!targetPackageName.isNullOrBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPackageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        // 2. Try the primary VietQR deep link intent
        try {
            val deepLinkUri = buildVietQrDeepLinkUri(bankBin, accountNumber, amount, memo)
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

        // 3. Fallback: Try looking up bank by BIN to open its app if installed
        val bank = VietnameseBanks.findByBin(bankBin)
        if (bank?.packageName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(bank.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        return false
    }
}
