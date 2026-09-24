package com.yourhistory.app.domain.model

data class VietQrData(
    val bankBin: String,
    val bankName: String,
    val accountNumber: String,
    val amount: Long? = null,
    val memo: String = "",
    val recipientName: String? = null,
    val rawPayload: String = ""
)
