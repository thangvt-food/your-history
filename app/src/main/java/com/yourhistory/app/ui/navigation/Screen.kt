package com.yourhistory.app.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scanner : Screen("scanner")
    data object Contacts : Screen("contacts")
    data object History : Screen("history")

    data object TransactionForm : Screen(
        "transaction_form?bankBin={bankBin}&account={account}&amount={amount}&memo={memo}&recipient={recipient}&contactId={contactId}"
    ) {
        fun createRoute(
            bankBin: String = "",
            account: String = "",
            amount: Long? = null,
            memo: String = "",
            recipient: String = "",
            contactId: String = ""
        ): String {
            val encodedMemo = try { URLEncoder.encode(memo, "UTF-8") } catch (e: Exception) { memo }
            val encodedRecipient = try { URLEncoder.encode(recipient, "UTF-8") } catch (e: Exception) { recipient }
            val amountStr = amount?.toString() ?: ""
            return "transaction_form?bankBin=$bankBin&account=$account&amount=$amountStr&memo=$encodedMemo&recipient=$encodedRecipient&contactId=$contactId"
        }
    }

    data object ShowQr : Screen(
        "show_qr?payload={payload}&bankBin={bankBin}&bankName={bankName}&account={account}&amount={amount}&memo={memo}&senderPkg={senderPkg}&senderName={senderName}&imgSaved={imgSaved}"
    ) {
        /** Encode payload phòng ký tự `?`/`+` từ memo làm vỡ route. */
        fun createRoute(
            payload: String,
            bankBin: String = "",
            bankName: String = "",
            account: String = "",
            amount: Long? = null,
            memo: String = "",
            senderPkg: String? = null,
            senderName: String = "",
            imageSaved: Boolean = false
        ): String {
            fun enc(s: String) = try { URLEncoder.encode(s, "UTF-8") } catch (e: Exception) { s }
            val amountStr = amount?.toString() ?: ""
            return "show_qr?payload=${enc(payload)}&bankBin=$bankBin&bankName=${enc(bankName)}" +
                "&account=$account&amount=$amountStr&memo=${enc(memo)}" +
                "&senderPkg=${enc(senderPkg ?: "")}&senderName=${enc(senderName)}" +
                "&imgSaved=${if (imageSaved) "1" else "0"}"
        }
    }
}
