package com.yourhistory.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "qr_contacts")
data class QrContactEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val recipientName: String,
    val bankBin: String,
    val bankName: String,
    val accountNumber: String,
    val defaultAmount: Long? = null,
    val defaultNote: String = "",
    val defaultCategoryId: String? = null,
    val lastUsedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING"
)
