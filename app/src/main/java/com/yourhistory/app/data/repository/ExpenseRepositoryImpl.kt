package com.yourhistory.app.data.repository

import com.yourhistory.app.data.local.AppDatabase
import com.yourhistory.app.data.local.entity.CategoryEntity
import com.yourhistory.app.data.local.entity.QrContactEntity
import com.yourhistory.app.data.local.entity.TransactionEntity
import com.yourhistory.app.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ExpenseRepositoryImpl(
    private val database: AppDatabase
) : ExpenseRepository {

    private val categoryDao = database.categoryDao()
    private val qrContactDao = database.qrContactDao()
    private val transactionDao = database.transactionDao()

    override fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    override suspend fun insertCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    override fun getAllQrContacts(): Flow<List<QrContactEntity>> =
        qrContactDao.getAllQrContacts()

    override fun getRecentQrContacts(limit: Int): Flow<List<QrContactEntity>> =
        qrContactDao.getRecentQrContacts(limit)

    override suspend fun getQrContactById(id: String): QrContactEntity? =
        qrContactDao.getQrContactById(id)

    override suspend fun saveQrContact(contact: QrContactEntity) {
        val existing = qrContactDao.findByAccount(contact.bankBin, contact.accountNumber)
        if (existing != null) {
            qrContactDao.update(
                contact.copy(
                    id = existing.id,
                    createdAt = existing.createdAt,
                    updatedAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis()
                )
            )
        } else {
            qrContactDao.insertOrUpdate(contact)
        }
    }

    override suspend fun deleteQrContact(id: String) =
        qrContactDao.softDelete(id)

    override suspend fun updateQrContactLastUsed(id: String) =
        qrContactDao.updateLastUsed(id)

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    override fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsInRange(startDate, endDate)

    override suspend fun getTransactionById(id: String): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity) =
        transactionDao.insertTransaction(transaction)

    override suspend fun deleteTransaction(id: String) =
        transactionDao.softDeleteTransaction(id)

    override fun getFinancialSummary(startDate: Long, endDate: Long): Flow<FinancialSummary> {
        val expenseFlow = transactionDao.getTotalExpenseInRange(startDate, endDate)
        val incomeFlow = transactionDao.getTotalIncomeInRange(startDate, endDate)

        return combine(expenseFlow, incomeFlow) { expense, income ->
            FinancialSummary(
                totalExpense = expense,
                totalIncome = income,
                balance = income - expense
            )
        }
    }
}
