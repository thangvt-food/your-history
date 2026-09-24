package com.yourhistory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val amount: Long,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: String,
    val note: String,
    val qrContactId: String? = null,
    val recipientName: String? = null,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val transactionDate: Long = System.currentTimeMillis(),
    val paymentMethod: String = "VIETQR", // "VIETQR", "CASH", "BANK_TRANSFER"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING"
)
