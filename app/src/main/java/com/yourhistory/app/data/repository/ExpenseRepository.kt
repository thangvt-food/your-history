package com.yourhistory.app.data.repository

import com.yourhistory.app.data.local.entity.CategoryEntity
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import com.yourhistory.app.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>>
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity)

    // QR Contacts
    fun getAllQrContacts(): Flow<List<QrContactEntity>>
    fun getRecentQrContacts(limit: Int = 10): Flow<List<QrContactEntity>>
    suspend fun getQrContactById(id: String): QrContactEntity?
    suspend fun saveQrContact(contact: QrContactEntity)
    suspend fun deleteQrContact(id: String)
    suspend fun updateQrContactLastUsed(id: String)

    // Transactions
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getRecentTransactions(limit: Int = 15): Flow<List<TransactionEntity>>
    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: String): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(id: String)

    // Financial Summary
    fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary>
}
